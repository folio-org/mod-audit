# mod-audit

Copyright (C) 2017-2023 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Table of contents

- [Introduction](#introduction)
- [API](#api)
  - [Configuration API](#configuration-api)
    - [Permissions for working with audit configurations](#permissions-for-working-with-audit-configurations)
    - [Updating a configuration](#updating-a-configuration)
- [Additional information](#additional-information)
  - [Other documentation](#other-documentation)
  - [Issue tracker](#issue-tracker)
  - [Quick start](#quick-start)
  - [API documentation](#api-documentation)
  - [Code analysis](#code-analysis)
  - [Download and configuration](#download-and-configuration)

## Introduction

The mod-audit module provides API to access and modify audit data.

## API

### Configuration API

| METHOD | URL                                                   | DESCRIPTION                                       |
|:-------|:------------------------------------------------------|:--------------------------------------------------|
| GET    | `/audit/config/groups`                                | Lists module's all available configuration groups |
| GET    | `/audit/config/groups/{groupId}/settings`             | Retrieves configurations of specific group        |
| PUT    | `/audit/config/groups/{groupId}/settings/{settingId}` | Updates a specific configuration of a group       |

#### Permissions for working with audit configurations
Audit configurations are grouped by certain identifiers and to view or update them the user must have the following special permissions:

| Permission                                                    | DESCRIPTION                                                      |
|:--------------------------------------------------------------|:-----------------------------------------------------------------|
| `audit.config.groups.settings.{groupId}.collection.get`       | permission required for retrieving the configurations of a group |
| `audit.config.groups.settings.{groupId}.{settingId}.item.put` | permission for updating a configuration of a group               |

Here, `{groupId}` is the id of a configuration group that can have values like `audit.authority`, `audit.inventory` 
and `{settingId}` is the id of a configuration to be updated that can have values like `enabled`, `records.page.size`, `records.retention.period` etc.

Example response for the configuration groups API call `GET /audit/config/groups`:

```json
{
  "settingGroups": [
    {
      "id": "audit.authority",
      "name": "Authority Audit Configuration",
      "description": "Group of configurations for audit of authority records"
    },
    {
      "id": "audit.inventory",
      "name": "Inventory Audit Configuration",
      "description": "Group of configurations for audit of inventory records: instances, holdings, items, etc."
    }
  ],
  "totalRecords": 2
}
```

By replacing the `groupId` in `GET /audit/config/groups/{groupId}/settings` taken from the above API call with an id of some configuration group, one can retrieve the settings for that group
For example, Inventory Audit configurations can be retrieved with the API call: `GET /audit/config/groups/audit.inventory/settings` and example response may look something like this:

```json
{
  "settings": [
    {
      "key": "enabled",
      "value": true,
      "type": "BOOLEAN",
      "description": "Defines if the inventory audit is enabled",
      "groupId": "audit.inventory",
      "metadata": {
        "createdDate": "2025-03-19T00:00:00.000+00:00",
        "createdByUserId": "00000000-0000-0000-0000-000000000000",
        "updatedDate": "2025-03-19T00:00:00.000+00:00",
        "updatedByUserId": "00000000-0000-0000-0000-000000000000"
      }
    }
  ],
  "totalRecords": 1
}
```

#### Updating a configuration
In order to update a configuration, we can use the `PUT /audit/config/groups/{groupId}/settings/{settingId}` API call.
Here, the `groupId` is the id of configuration group and `settingId` is the id of configuration to be updated which can be retrieved from the previous API call.
The information about `key` and `type` fields which are provided in the request body can also be retrieved from the previous API call.

For example, to update the `enabled` configuration of the `audit.inventory` group, we can use the following API call:

```http
PUT /audit/config/groups/audit.inventory/settings/enabled

Content-Type: application/json
x-okapi-tenant: [tenant]
x-okapi-token: [JWT_TOKEN]

{
    "key": "enabled",
    "value": true,
    "description": "Defines if the inventory audit is enabled",
    "type": "BOOLEAN",
    "groupId": "audit.inventory"
}
```

Only the `value` and `description` fields of a configuration can be updated using the above API call and the other fields should have the same
values as they have in the response from the `GET /audit/config/groups/{groupId}/settings`. Providing the different values for those other fields will
break the logic of configuration's usage.

Here is an example of request body for updating the `records.retention.period` configuration which is of `INTEGER` type

```json
{
  "key": "records.retention.period",
  "value": 30,
  "description": "Defines the retention period of audit records in days",
  "type": "INTEGER",
  "groupId": "audit.inventory"
}
``` 
In general, the value for a configuration to be updated should be the valid value for the type of that configuration.

## Additional information

### Other documentation

Other [modules](https://dev.folio.org/source-code/#server-side) are described,
with further FOLIO Developer documentation at [dev.folio.org](https://dev.folio.org/)

### Issue tracker

See project [MODAUDIT](https://issues.folio.org/browse/MODAUD)
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker).

### Quick start

Compile with `mvn clean install`

Run the local stand-alone instance:

```
java -jar target/mod-audit-fat.jar -Dhttp.port=8085 embed_postgres=true
```

### API documentation

This module's [API documentation](https://dev.folio.org/reference/api/#mod-audit).

The local API docs are available, for example:
```
http://localhost:8081/apidocs/?raml=raml/audit-data.raml
etc.
```

### Code analysis

[SonarQube analysis](https://sonarcloud.io/dashboard?id=org.folio%3Amod-audit).

### Download and configuration

The built artifacts for this module are available.
See [configuration](https://dev.folio.org/download/artifacts) for repository access,
and the [Docker image](https://hub.docker.com/r/folioorg/mod-audit/).

