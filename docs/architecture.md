# Architecture Overview

## Components

### GitHub Workflow Integration
- Receives GitHub Workflow events
- Processes runner requirements
- Communicates with Azure API

### Azure VM Management
- Provisions Azure VMs
- Applies specified labels
- Manages VM lifecycle

### Event Processing
- Event queue handling
- Error management
- Retry mechanisms
