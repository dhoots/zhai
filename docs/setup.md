# Setup Guide

## Prerequisites

### 1. Azure Resources
- Azure subscription with:
  - Resource group: `hw-rg`
  - VNet: `hw-vnet`
  - Subnet: `hw-subnet`
  - NSG: `hw-nsg`
  - App Service plan (min=1, max=1 instances)

### 2. Software Requirements
- Java runtime environment
- Tomcat 10.1
- Git
- Azure CLI
- Maven (for building)

### 3. Network Requirements
- VNet integration capability
- Private IP address space
- Network Security Group rules configured

## Configuration

### 1. Azure Service Principal
Create a service principal with the following permissions:
- Virtual Machine Contributor
- Network Contributor
- Storage Blob Data Contributor

### 2. Environment Variables
Set the following environment variables:

#### Azure Authentication
```
AZURE_CLIENT_ID=your_client_id
AZURE_CLIENT_SECRET=your_client_secret
AZURE_TENANT_ID=your_tenant_id
AZURE_SUBSCRIPTION_ID=your_subscription_id
```

#### GitHub Integration
```
GITHUB_REGISTRATION_PAT=your_github_pat
GITHUB_WEBHOOK_SECRET=your_webhook_secret
```

#### Database Configuration
```
H2_DB_USER=your_db_user
H2_DB_PASSWORD=your_db_password
```

#### Alerting Configuration
```
RUNNER_DIAG_API_TOKEN=your_diag_token
ALERT_EMAIL_TO=your_alert_email
ALERT_EMAIL_FROM=your_from_email
SMTP_USER=your_smtp_user
SMTP_PASSWORD=your_smtp_password
```

### 3. Configuration Files
1. Create YAML configuration file with:
   - VM specifications per label
   - Pool management parameters
   - Network settings
   - Resource group details

Example label configuration:
```yaml
labels:
  android-small:
    vm_size: Standard_D8s_v6
    os_image: Ubuntu 24.04
    region: US WEST
    network:
      vnet: hw-vnet
      subnet: hw-subnet
      nsg: hw-nsg
    disk:
      type: Premium SSD V2
      size: 1024GB
    pool:
      minimum_warm_vms: 3
      maximum_pool_size: 12
      scale_up_trigger_threshold: 3
      idle_timeout: 3h
```

## Deployment Steps

### 1. Repository Setup
```bash
git clone https://github.com/your-repo.git
cd your-repo
```

### 2. Build Application
```bash
mvn clean package
```

### 3. Azure App Service Setup
1. Create App Service in WEST US region
2. Configure VNet Integration to `hw-vnet`
3. Set runtime stack to Tomcat 10.1
4. Configure App Service plan (B2 SKU)

### 4. Application Deployment
1. Deploy WAR file to App Service
2. Configure environment variables
3. Set up health check endpoint
4. Configure alerting system

### 5. GitHub Webhook Setup
1. Go to repository settings
2. Add webhook with:
   - Payload URL: `https://your-app-service-url/runner/webhook`
   - Content type: `application/json`
   - Secret: `GITHUB_WEBHOOK_SECRET`
   - Events: `workflow_job`

## Post-Deployment Verification

1. Verify App Service is running
2. Test webhook endpoint
3. Check initial VM pool provisioning
4. Verify health check endpoint
5. Test error alerting system

## Troubleshooting

### Common Issues
1. **Webhook Validation Failed**
   - Verify GITHUB_WEBHOOK_SECRET matches GitHub settings
   - Check webhook URL is correct

2. **VM Provisioning Failed**
   - Verify Azure service principal permissions
   - Check resource group access
   - Verify VNet/subnet availability

3. **Database Connection Issues**
   - Verify H2_DB_USER and H2_DB_PASSWORD
   - Check database permissions

4. **Alerting Not Working**
   - Verify SMTP credentials
   - Check email configuration
   - Verify alert triggers are configured
