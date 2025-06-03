# Project Specification: GitHub Actions VM Runner Provisioner for Azure

**Version:** 1.2 (Developer Ready with Testing Plan)
**Date:** June 3, 2025

## 1. Introduction / System Overview

* **Purpose:** To create a Java Tomcat application that listens for GitHub Workflow events and dynamically provisions self-hosted GitHub Action runners on Azure Virtual Machines (VMs). The system will manage a pool of these VMs based on demand, ensuring efficient use of resources while providing necessary compute for GitHub Actions jobs.
* **High-Level Workflow:**
    1.  GitHub triggers a `workflow_job` event.
    2.  A webhook notifies the Tomcat application.
    3.  The application parses the job requirements (labels).
    4.  It checks a pool of existing warm VMs or provisions a new Azure VM if necessary and capacity allows.
    5.  The VM is configured with the GitHub Runner software and registered to the specified repository.
    6.  The Tomcat application monitors the VM/runner and de-provisions it when no longer needed according to pool management rules.

## 2. Core Functionality: GitHub Event Triggering and Job Intake

* **Event Source:** GitHub `workflow_job` events.
* **Webhook Endpoint:** The Tomcat application will expose a specific HTTP endpoint to receive webhook payloads from GitHub.
* **Triggering Condition:** The application will primarily act on `workflow_job` events where the `action` field is `queued`.
* **Label Extraction:** Runner requirements (labels) will be extracted from the `payload.workflow_job.labels` array in the webhook.

## 3. Configuration Management

* **Primary Configuration File:** A YAML file, packaged with the Tomcat application, will store the primary configuration.
* **Label-to-VM Mapping:** This section in the YAML file defines the Azure VM specifications corresponding to specific GitHub runner labels. Each mapping includes:
    * **Label:** The GitHub runner label (e.g., `android-small`).
    * **VM Series/Size:** Azure VM size (e.g., `Standard_D8s_v6`).
    * **OS Image:** Azure VM image reference (e.g., Ubuntu 24.04).
    * **Region:** Azure region for VM deployment (e.g., `US WEST`).
    * **Network Settings:**
        * VNet: `hw-vnet`
        * Subnet: `hw-subnet`
        * Network Security Group (NSG): `hw-nsg`
    * **Disk Type/Size:** E.g., `Premium SSD V2 with 1024GB`.
    * **Runners per VM:** 1.
    * **Example Mappings:**
        * **Label:** `android-small` (VM: `Standard_D8s_v6`, OS: `Ubuntu 24.04`, Region: `US WEST`, Network: `hw-vnet/hw-subnet/hw-nsg`, Disk: `Premium SSD V2 1024GB`)
        * **Label:** `android-medium` (VM: `Standard_D32s_v6`, OS: `Ubuntu 24.04`, Region: `US WEST`, Network: `hw-vnet/hw-subnet/hw-nsg`, Disk: `Premium SSD V2 1024GB`)
        * **Label:** `android-large` (VM: `Standard_D48s_v6`, OS: `Ubuntu 24.04`, Region: `US WEST`, Network: `hw-vnet/hw-subnet/hw-nsg`, Disk: `Premium SSD V2 1024GB`)
* **VM Pool Management Parameters:** Defined per label in the YAML file.
    * `minimum_warm_vms`: Desired number of idle, ready VMs (e.g., `android-small`: 3, `android-medium`: 4, `android-large`: 3).
    * `maximum_pool_size`: Maximum total VMs allowed for this label (e.g., `android-small`: 12, `android-medium`: 48, `android-large`: 84).
    * `scale_up_trigger_threshold`: Number of idle VMs at or below which new VMs should be provisioned to replenish the warm pool (e.g., if `minimum_warm_vms` is 3, this threshold is also 3, meaning scale up if idle VMs <= 3).
    * `idle_timeout`: Duration after which an idle VM (above minimum warm count) is a candidate for de-provisioning (e.g., `android-small`: 3 hours, `android-medium`: 1 hour, `android-large`: 30 minutes).
* **Handling Unconfigured Labels:** If a `workflow_job` event contains labels for which no VM configuration is defined in the YAML, the event will be ignored.

## 4. Azure VM Provisioning

* **Authentication:** Azure Service Principal. Credentials sourced from environment variables (see Section 9.1).
* **Resource Group:** VMs will be provisioned in a predefined Azure resource group: `hw-rg`.
* **VM Naming Convention:** `andr-<label_suffix>-<timestamp>` (e.g., `andr-small-20250603100000`). The timestamp ensures uniqueness. `<label_suffix>` is the part of the label after any prefix (e.g., "small" from "android-small").
* **VM Tagging:** VMs provisioned by this application should be tagged with a unique application identifier (e.g., `app_name: github-runner-provisioner`, `managed_by: tomcat-app`) to allow for discovery and reconciliation if the Tomcat application restarts.
* **Networking:**
    * VMs will be assigned Private IP addresses within the `hw-vnet` and `hw-subnet` specified in the configuration.
    * The `hw-nsg` must be configured to allow inbound traffic on the VM status endpoint port (see Section 7) from the App Service's integrated subnet.
* **VM Creation Process:** The application will use the Azure SDK to provision VMs and will wait for the VM creation process to complete successfully before proceeding with runner configuration.

## 5. GitHub Runner Configuration on VM

* **Startup Script:**
    * **Language:** Bash script (`.sh`).
    * **Storage:** The script will be maintained as a template file, packaged with the Tomcat application.
    * **Injection:** Tomcat will read this template, replace defined placeholders with dynamic values, and then inject the personalized script into the Azure VM (e.g., via Azure Custom Data).
    * **Placeholders in Script Template:**
        * `%%RUNNER_REGISTRATION_TOKEN%%`: One-time GitHub Actions runner registration token.
        * `%%RUNNER_NAME%%`: Generated unique name for the runner.
        * `%%RUNNER_LABELS%%`: Comma-separated string of labels.
        * `%%GITHUB_REPO_URL%%`: URL of the GitHub repository for registration (e.g., `https://github.com/eBayMobile/andr_core`).
        * `%%STATUS_SERVER_PORT%%`: Port for the local Python HTTP status server (e.g., `8080`).
    * **Script Tasks (executed on the VM):**
        1.  Install `jq` utility.
        2.  Download the latest GitHub Actions runner software package.
        3.  Unpack the runner software.
        4.  Execute the runner's `./config.sh` script with necessary parameters:
            * `--unattended`
            * `--url "%%GITHUB_REPO_URL%%"`
            * `--token "%%RUNNER_REGISTRATION_TOKEN%%"`
            * `--name "%%RUNNER_NAME%%"`
            * `--labels "%%RUNNER_LABELS%%"`
            * `--replace` (to handle cases where a runner with the same name might exist).
            * (Runner will be configured as non-ephemeral).
        5.  Register the runner as a system service (e.g., systemd on Linux) and start it.
        6.  Start the local Python HTTP status server (see Section 7 for details).
* **Runner Registration:**
    * **Scope:** Repository level, specifically to `eBayMobile/andr_core`.
    * **Token Acquisition by Tomcat:** The Tomcat application will call the GitHub REST API endpoint for creating a runner registration token using a stored GitHub Personal Access Token (PAT). PAT sourced from environment variables (see Section 9.1).
* **Runner Naming Convention:** Same as the VM: `andr-<label_suffix>-<timestamp>`.

## 6. VM Pool Management Logic

* **Tomcat Application State:** The single Tomcat instance will manage the state of all VMs/runners it provisions. This state will be stored in the H2 database (see Section 10).
* **Application Startup:**
    1.  Validate all configurations (YAML, environment variables).
    2.  Reconcile state: Query Azure for existing VMs tagged as belonging to this application. Compare with internal DB state to identify active, orphaned, or missing VMs.
    3.  Pre-warm VM pool: For each configured label type, provision VMs until the `minimum_warm_vms` count is met with idle, ready runners.
* **Handling Incoming `workflow_job` (action: `queued`):**
    1.  The application attempts to find an available "warm" (idle) VM from the pool that matches the job's labels.
    2.  **If a warm VM is found:**
        * The VM is assigned the job (status updated to `BUSY`).
        * If the number of remaining warm VMs for that label type falls to or below the `scale_up_trigger_threshold`, the application will proactively provision a new VM to replenish the warm pool, aiming to restore the `minimum_warm_vms` count.
    3.  **If no warm VM is found AND the current total number of VMs (busy + warm) for that label type is less than its `maximum_pool_size`:**
        * The application will immediately provision a new VM specifically for this job.
    4.  **If no warm VM is found AND the pool for that label type is already at its `maximum_pool_size`:**
        * The application will ignore the `workflow_job` event. The job will remain queued on GitHub's side.
* **Handling Job Completion (signaled by runner becoming idle):**
    * When a runner completes a job, it will become idle. The Tomcat application's polling mechanism (see Section 7) will detect this change.
    * The VM's status will be updated to `IDLE`, and its `IDLE_SINCE_TIMESTAMP` will be recorded. It is now part of the warm pool.

## 7. VM Idle Detection and De-provisioning

* **Local Status Endpoint on VM:**
    * **Implementation:** A simple HTTP server using Python's built-in `http.server` module, started by the VM's startup script.
    * **Listening Address:** `0.0.0.0` on port `8080` (or the value of `%%STATUS_SERVER_PORT%%`), path `/runner/status`.
    * **Busy/Idle Determination Logic:** The Python script will determine the runner's state by checking for the existence of a specific GitHub Actions runner worker process (e.g., `Runner.Worker` child process of `Runner.Listener`).
    * **Response Format:** JSON: `{"busy": true}` if a worker process is found, `{"busy": false}` otherwise.
* **Tomcat Application Polling:**
    * **Frequency:** Every 30 seconds.
    * **Target:** `http://<private-vm-ip>:<port>/runner/status` for each active VM managed by the application.
    * The Tomcat application will update its internal record of the VM's status based on the poll response.
* **De-provisioning Trigger Conditions:** A VM becomes a candidate for de-provisioning if all the following are true:
    1.  The VM continuously reports `{"busy": false}`.
    2.  The duration for which the VM has been idle (current time - `IDLE_SINCE_TIMESTAMP`) exceeds the `idle_timeout` defined for its label type in the YAML configuration.
    3.  The current number of warm (idle) VMs for its label type is greater than the configured `minimum_warm_vms`.
* **De-provisioning Actions (in order):**
    1.  De-register the GitHub runner from the repository using the GitHub API.
    2.  Terminate the Azure VM using the Azure SDK.
    3.  Remove the VM's record from the Tomcat application's internal state/database.

## 8. Error Handling and Retry Strategy

* **General Principle:** For critical operational failures (e.g., VM provisioning, startup script success, de-provisioning steps), the application will:
    1.  Attempt to clean up any partially created resources if applicable and safe.
    2.  Log the error in detail.
* **Retry Mechanism:**
    * **Number of Retries:** 5 times for the failed high-level action.
    * **Delay Strategy:** Exponential backoff between retries (e.g., starting at 10 seconds, doubling with each attempt, with a reasonable maximum delay like 5 minutes).
* **Alerting on Failures:**
    1.  An alert (see Section 9 for alerting system details) will be sent on the initial occurrence of a significant failure.
    2.  If all 5 retry attempts for that action are exhausted and the action ultimately remains unsuccessful, a final, more critical alert will be sent.
* **Startup Script Failure Detection:**
    1.  After an Azure VM is provisioned, the Tomcat application will wait up to **3 minutes** for the VM's status endpoint (`http://<private-ip>:<port>/runner/status`) to become responsive and return a valid initial JSON status (e.g., `{"busy": false}`).
    2.  If the endpoint does not become responsive with a valid status within this timeout, Tomcat will:
        * Consider the VM's startup and configuration to have failed.
        * Attempt to terminate this problematic Azure VM.
        * Trigger the retry mechanism (5 attempts) for provisioning a *replacement* VM.
* **Specific Failures to Handle:** Azure VM provisioning errors, startup script execution failures, GitHub Runner registration failures, VM status polling failures, de-provisioning failures.

## 9. Tomcat Application Deployment & Operations

### 9.1. Environment Variables

* **Azure Authentication:**
    * `AZURE_CLIENT_ID`
    * `AZURE_CLIENT_SECRET`
    * `AZURE_TENANT_ID`
    * `AZURE_SUBSCRIPTION_ID`
* **GitHub Integration:**
    * `GITHUB_REGISTRATION_PAT` (Personal Access Token for runner registration)
    * `GITHUB_WEBHOOK_SECRET` (For GitHub webhook signature validation)
* **H2 Database Authentication:**
    * `H2_DB_USER`
    * `H2_DB_PASSWORD`
* **Tomcat Application Diagnostics & Alerting:**
    * `RUNNER_DIAG_API_TOKEN` (For authenticating to the Tomcat app's health endpoint)
    * `ALERT_EMAIL_TO`
    * `ALERT_EMAIL_FROM`
    * `SMTP_USER` (For SMTP server authentication)
    * `SMTP_PASSWORD` (For SMTP server authentication)

### 9.2. Deployment Platform

* **Platform:** Azure App Service.
* **Instance Count:** Single instance (configure App Service plan for `min=1, max=1` instances).
* **Region:** `WEST US`.
* **Runtime Stack:** Tomcat 10.1, Java (version as appropriate for dependencies).
* **Service Plan SKU:** `B2` (evaluate adequacy for single instance).

### 9.3. Networking

* **VNet Integration:** The Azure App Service hosting Tomcat **must** be configured with VNet Integration to connect to the VNet (`hw-vnet`) where the runner VMs reside.

### 9.4. Health Check Endpoint (for Tomcat application)

* **Path:** `GET /runner/status` (on the Tomcat application's port, e.g., `8080`).
* **Authentication:** Expects an `access-token` HTTP header. The token value should be read from `RUNNER_DIAG_API_TOKEN`.

### 9.5. Alerting System

* **Method:** Email notifications.
* **Recipient(s):** From `ALERT_EMAIL_TO` environment variable.
* **Sender Address:** From `ALERT_EMAIL_FROM` environment variable.
* **SMTP Configuration:**
    * Host: `smtp.google.com`
    * Port: `465`
    * TLS Method: SMTPS (SSL/TLS).
    * Authentication: Application will use username/password SMTP authentication, with credentials from `SMTP_USER` and `SMTP_PASSWORD` environment variables. (Note: "Anonymous send" for `smtp.google.com` is generally not supported without specific configurations like GSuite relay or if using an App Password).
* **Alert Triggers:** Initial/final provisioning failures, pool size thresholds critically breached, de-provisioning failures, and other critical errors.

## 10. Technology Stack / Dependencies (Java Ecosystem)

* **Framework:** Spring Boot
    * `spring-boot-starter-web`
    * `spring-boot-starter-data-jpa`
    * `spring-boot-starter-validation`
    * `spring-boot-starter-test`
* **Database: H2**
    * **Purpose:** For internal state management by the single Tomcat instance.
    * **Configuration Details:**
        * **Location:** `/home/devops/h2/runner.db` (Path within the App Service container. Ensure this path is persistent if needed, e.g., by mounting Azure Files to `/home` if App Service default storage is ephemeral for that path).
        * **Connection:** File-based H2 database.
        * **Authentication:** Username and password sourced from `H2_DB_USER` and `H2_DB_PASSWORD` environment variables.
    * **Conceptual H2 Schema for VMs:** A table (e.g., `ManagedRunners`) with columns like: `vm_id (VARCHAR PK)`, `azure_vm_id (VARCHAR)`, `runner_name (VARCHAR)`, `private_ip (VARCHAR)`, `labels (VARCHAR)`, `status (VARCHAR: PROVISIONING, IDLE, BUSY, PENDING_DELETION)`, `idle_since_timestamp (TIMESTAMP)`, `last_heartbeat_timestamp (TIMESTAMP)`, `assigned_job_id (VARCHAR)`.
* **Azure SDK for Java:**
    * Azure SDK for Compute (`azure-resourcemanager-compute`)
    * Azure SDK for Identity (`azure-identity`)
* **HTTP Client:** Apache HttpClient (or Spring's RestTemplate/WebClient) for GitHub API calls.
* **JSON Processing:** Jackson.
* **YAML Processing:** SnakeYAML.
* **Logging:** Logback with a JSON encoder for structured logging.
* **Email:** JavaMail API (`jakarta.mail-api`).
* **Testing Utilities:** Testcontainers.

## 11. Security Considerations

* **Credentials Management:** All sensitive credentials (Azure SP, GitHub PAT, DB auth, diagnostic tokens, SMTP auth) to be managed via environment variables, ideally injected through Azure App Service configuration from Azure Key Vault.
* **Network Security:**
    * VMs operate with private IP addresses only.
    * Tomcat App Service uses VNet Integration.
    * NSG for VM subnet strictly configured to allow VM status endpoint port only from App Service subnet.
* **API Access Permissions:**
    * GitHub PAT: Minimum necessary `repo` scope for `eBayMobile/andr_core`.
    * Azure Service Principal: Minimum necessary roles (e.g., "Virtual Machine Contributor" on `hw-rg`).
* **Webhook Security:** Implement GitHub webhook secret validation using the `GITHUB_WEBHOOK_SECRET` environment variable.
* **Input Validation:** Validate webhook payloads and configuration file contents.
* **Dependency Management:** Regularly update libraries to patch known vulnerabilities.

## 12. Testing Plan

### 12.1. Overview
* The testing strategy aims to ensure the application's reliability, correctness, and performance through a comprehensive suite of automated tests.

### 12.2. Unit Testing
* **Focus:** Individual classes, methods, and logic components in isolation.
* **Areas to Test:**
    * YAML configuration parsing and validation.
    * Mapping logic from GitHub labels to Azure VM specifications.
    * VM and GitHub Runner naming convention generation.
    * Pool management calculations:
        * Determining when to scale up to meet `minimum_warm_vms` or `scale_up_trigger_threshold`.
        * Identifying VMs eligible for termination based on `idle_timeout` and pool state.
        * Checking against `maximum_pool_size`.
    * Startup script template placeholder replacement.
    * Exponential backoff calculation for retry logic.
    * Parsing of status from the VM's Python HTTP server (e.g., `{"busy": true}`).
* **Tools:** JUnit 5, Mockito.

### 12.3. Integration Testing
* **Focus:** Interactions between different internal components and with external services (which may be mocked or emulated).
* **Areas to Test:**
    * **H2 Database Interaction:** Service layer logic for creating, reading, updating, and deleting VM/runner state records. (Spring Boot's `@DataJpaTest`, Testcontainers for H2 if needed).
    * **GitHub API Client:** Test the client responsible for fetching runner registration tokens from GitHub (mock the HTTP calls to GitHub).
    * **Azure SDK Client:** Test the service layer interacting with the Azure SDK for VM provisioning, termination, and status lookups (mock the Azure SDK clients).
    * **Webhook Endpoint Processing:** Test the controller receiving mock GitHub webhook payloads, validating them (including signature if `GITHUB_WEBHOOK_SECRET` is used), and ensuring correct invocation of service layer logic.
    * **VM Status Polling & Update:** Simulate VMs and their status endpoints; test Tomcat's ability to poll these, interpret responses, and update internal VM states and idle timers.
    * **Email Alerting Service:** Verify that the email service constructs and attempts to send emails correctly when triggered by events like provisioning failures (mock the `JavaMailSender`).
* **Tools:** Spring Boot Test framework (`@SpringBootTest`), JUnit 5, Mockito, Testcontainers (for H2, GreenMail for SMTP testing).

### 12.4. End-to-End (E2E) / System Testing
* **Focus:** Validating complete workflows through the system, simulating external interactions as closely as possible.
* **Key Scenarios:**
    1.  **Full Job Lifecycle (Happy Path):**
        * Event: Mock `workflow_job` (queued) received.
        * Action: No warm VM, pool below max -> New VM provisioned (mock Azure). VM status endpoint becomes available (simulated). Runner "registers" (mock GitHub interaction). Status becomes `{"busy": false}`.
        * Action: Simulate job assignment -> status `{"busy": true}`.
        * Action: Simulate job completion -> status `{"busy": false}`. VM returns to warm pool.
    2.  **Pool Replenishment:**
        * Action: Job assigned from warm pool, triggering `scale_up_trigger_threshold`.
        * Verification: New VM is provisioned to replenish the pool (mock Azure).
    3.  **Idle VM Termination:**
        * Condition: VM remains idle longer than `idle_timeout`, pool above `minimum_warm_vms`.
        * Verification: Runner "de-registered" (mock GitHub), VM "terminated" (mock Azure).
    4.  **Max Pool Capacity:**
        * Condition: Relevant VM pool is at `maximum_pool_size`.
        * Action: New `workflow_job` (queued) event for that label arrives.
        * Verification: Event is logged and ignored by the provisioning logic.
    5.  **Error Handling Flows:**
        * Simulate Azure VM provisioning failure -> verify retry logic, alerts, and eventual failure handling.
        * Simulate VM startup script failure (status endpoint never becomes responsive within 3 min) -> verify VM termination and replacement attempts.
        * Simulate persistent polling failure for a running VM -> verify VM is eventually declared unhealthy and decommissioned.
* **Environment & Tools:**
    * Deploy the Tomcat application to a test App Service instance or run locally.
    * Use REST clients (e.g., RestAssured, Postman scripts) to send mock GitHub webhook events to the application's endpoint.
    * Develop simple mock HTTP servers (e.g., using WireMock, or custom in-test servers) to simulate the per-VM Python status endpoints (`/runner/status`).
    * Mock external GitHub and Azure API interactions at the boundary of the application or use cloud emulators if available and suitable (e.g., Azurite for Azure Storage, but less so for Compute).

### 12.5. Performance Considerations (Not formal tests for V1, but design considerations)
* Evaluate the efficiency of VM status polling, especially as the number of managed VMs increases.
* Assess H2 database performance with increasing numbers of VM records and concurrent status updates (even with a single app instance, internal concurrency can be a factor).
* Consider the application's resource footprint (CPU, memory) on the B2 App Service SKU under typical load.

### 12.6. Security Testing (Manual & Review Focus)
* Verify implementation of GitHub webhook secret validation.
* Confirm authentication on the Tomcat application's own health/diagnostic endpoint.
* Review secure handling and storage strategy for all secrets (environment variables, Key Vault integration).
* Ensure robustness against malformed webhook payloads or configuration file entries.