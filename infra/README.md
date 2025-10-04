# README Update Checklist

Use this checklist when making changes to the infrastructure to ensure the README stays current.

## 📋 When to Update README

- [ ] Adding new Docker services
- [ ] Changing port configurations
- [ ] Modifying database schemas
- [ ] Updating dataset or sample data
- [ ] Adding new API endpoints
- [ ] Changing connection credentials
- [ ] Adding development tools
- [ ] Modifying Docker Compose configurations

## 📝 Sections to Update

### 1. Directory Structure Section
```markdown
## 📁 Directory Structure
[Update the tree structure to reflect new files/folders]
```

### 2. Architecture Overview
```markdown
## 🏗️ Architecture Overview
[Update the ASCII diagram if adding/removing services]
```

### 3. Service Connection Details
```markdown
## 🔌 Service Connection Details
[Update tables with new ports, credentials, or services]
```

### 4. Quick Start Guide
```markdown
## 🚀 Quick Start Guide
[Update commands if process changes]
```

### 5. Migration Workflow Examples
```markdown
## 📋 Migration Workflow Examples  
[Update curl commands if API endpoints change]
```

### 6. Troubleshooting Guide
```markdown
## 🔧 Troubleshooting Guide
[Add new common issues and solutions]
```

### 7. Maintenance Notes
```markdown
## 📝 Maintenance Notes
**Last Updated**: [ALWAYS UPDATE THIS DATE]
**Version**: [INCREMENT VERSION NUMBER]
```

## 🔧 Quick Update Template

When making infrastructure changes, copy this template:

```markdown
### Change: [Brief description]
**Date**: [YYYY-MM-DD]
**Type**: [Addition/Modification/Removal]
**Impact**: [What sections need updating]

Updated Sections:
- [ ] Directory Structure
- [ ] Architecture Overview  
- [ ] Connection Details
- [ ] Quick Start Guide
- [ ] Migration Examples
- [ ] Troubleshooting
- [ ] Maintenance Notes
```

## 📊 Version History Template

Add this to the bottom of README when making major changes:

```markdown
## 📊 Version History

| Version | Date | Changes | Impact |
|---------|------|---------|---------|
| 1.1.0 | 2024-XX-XX | Added new service X | Updated ports, added endpoints |
| 1.0.0 | 2024-XX-XX | Initial infrastructure | Complete setup |
```