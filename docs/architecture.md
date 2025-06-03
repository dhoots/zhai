# Architecture Overview

## Core Components

### 1. Tomcat Application (Core Service)
- Java-based Tomcat application running on Azure App Service
- Single instance deployment in WEST US region
- Manages entire runner provisioning lifecycle

### 2. GitHub Integration
- Webhook endpoint for GitHub `workflow_job` events
- Processes job requirements from webhook payloads
- Validates webhook signatures using GITHUB_WEBHOOK_SECRET

### 3. Azure VM Management
- Uses Azure SDK for VM operations
- Manages VM lifecycle:
  - Provisioning
  - Configuration
  - Monitoring
  - De-provisioning

### 4. VM Pool Management
- Maintains pools of warm VMs per label type
- Configurable pool parameters:
  - minimum_warm_vms
  - maximum_pool_size
  - scale_up_trigger_threshold
  - idle_timeout

### 5. Runner Configuration System
- Manages GitHub runner setup on VMs
- Handles runner registration and configuration
- Maintains runner state tracking

## Architecture Flow

1. **Event Reception**
   - GitHub webhook sends `workflow_job` event
   - Tomcat validates webhook signature
   - Event is processed by core service

2. **VM Pool Management**
   - Checks existing warm VMs
   - Provisions new VM if needed
   - Maintains minimum warm pool size

3. **VM Provisioning**
   - Uses Azure SDK for VM creation
   - Applies configuration from YAML
   - Sets up networking and security

4. **Runner Setup**
   - Configures GitHub runner software
   - Registers runner with repository
   - Starts runner service

5. **Monitoring & Cleanup**
   - Polls VM status every 30 seconds
   - Manages idle VMs
   - Handles de-provisioning when required

## Technical Details

### 1. Configuration Management
- YAML-based configuration
- Contains:
  - VM specifications per label
  - Pool management parameters
  - Network settings
  - Resource group details

### 2. Networking
- VNet integration required
- Private IP addresses used
- NSG rules for status endpoint
- VNet Integration to `hw-vnet`

### 3. Error Handling
- Exponential backoff retry mechanism
- Maximum 5 retry attempts
- Detailed error logging
- Alert system integration

### 4. Monitoring
- Health check endpoint at `/runner/status`
- VM status polling every 30 seconds
- Idle VM monitoring
- Pool size tracking

## Security

1. **Authentication**
   - Azure Service Principal
   - GitHub PAT for runner registration
   - H2 database credentials
   - SMTP credentials for alerts

2. **Authorization**
   - Webhook signature validation
   - Health check token validation
   - Network security groups

3. **Data Protection**
   - Environment variable storage
   - Encrypted credentials
   - Secure token management

## Scalability

1. **Pool Management**
   - Configurable pool sizes
   - Dynamic scaling based on demand
   - Warm pool maintenance

2. **Resource Optimization**
   - Idle VM timeout
   - Minimum warm VMs
   - Maximum pool size limits

3. **Performance**
   - 30-second polling interval
   - Exponential backoff for retries
   - Efficient state management

## Error States & Recovery

1. **Common Error States**
   - VM provisioning failure
   - Runner configuration failure
   - Network connectivity issues
   - Authentication failures

2. **Recovery Mechanisms**
   - Automatic retries
   - Resource cleanup
   - State reconciliation
   - Alert notifications

## Deployment Requirements

1. **Azure Resources**
   - App Service in WEST US
   - Resource group: `hw-rg`
   - VNet: `hw-vnet`
   - Subnet: `hw-subnet`
   - NSG: `hw-nsg`

2. **Environment Variables**
   - Azure credentials
   - GitHub credentials
   - Database credentials
   - SMTP credentials
   - Diagnostic tokens

3. **Dependencies**
   - Java runtime
   - Tomcat 10.1
   - Azure SDK
   - GitHub API client
   - H2 database
   - Networking components
