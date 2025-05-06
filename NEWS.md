## 3.0.0-SNAPSHOT 2025-mm-dd
* [MODAUD-XXX](https://folio-org.atlassian.net/browse/MODAUD-XXX) - Description

## 2.11.1 2025-04-15 
* [MODAUD-250](https://folio-org.atlassian.net/browse/MODAUD-250) - Version history of "MARC" records is not tracked

## 2.11.0 2025-03-14
* [MODAUD-205](https://folio-org.atlassian.net/browse/MODAUD-205) - Consume domain event for MARC_BIB and MARC_AUTHORITY
* [MODAUD-231](https://folio-org.atlassian.net/browse/MODAUD-231) - Handle inventory shadow copy events
* [MODAUD-214](https://folio-org.atlassian.net/browse/MODAUD-214) - Rest Endpoint for history of MARC_BIB
* [MODAUD-210](https://folio-org.atlassian.net/browse/MODAUD-210) - Create scheduled job to delete expired audit records
* [MODAUD-235](https://folio-org.atlassian.net/browse/MODAUD-235) - Extend scheduled job with old empty partitions deletion, new partitions creation
* [MODAUD-206](https://folio-org.atlassian.net/browse/MODAUD-206) - [Instance Audit] Consume domain event
* [MODAUD-204](https://folio-org.atlassian.net/browse/MODAUD-204) - [Holdings Audit] Consume domain event
* [MODAUD-203](https://folio-org.atlassian.net/browse/MODAUD-203) - [Item Audit] Consume domain event
* [MODAUD-222](https://folio-org.atlassian.net/browse/MODAUD-222) - Support settings to configure number of cards in version history
* [MODAUD-215](https://folio-org.atlassian.net/browse/MODAUD-215) - [Instance Audit] Rest Endpoint for history
* [MODAUD-213](https://folio-org.atlassian.net/browse/MODAUD-213) - [Holding Audit] Rest Endpoint for history
* [MODAUD-219](https://folio-org.atlassian.net/browse/MODAUD-219) - [Instance, Holdings, Item Audit] Calculate entity diff on event consumption
* [MODAUD-212](https://folio-org.atlassian.net/browse/MODAUD-212) - [Item Audit] Rest Endpoint for history
* [MODAUD-230](https://folio-org.atlassian.net/browse/MODAUD-230) - Issue with mod-audit module migration
* [MODAUD-207](https://folio-org.atlassian.net/browse/MODAUD-207) - [Instance/Item/Holding/Bib/Authority Audit] Retention Period Configuration
* [MODAUD-209](https://folio-org.atlassian.net/browse/MODAUD-209) - [Instance/Item/Holding/Bib/Authority Audit] Feature Flag Configuration
* [MODAUD-234](https://folio-org.atlassian.net/browse/MODAUD-234) - Update request for configuration entries should be protected by separate permissions
* [MODAUD-229](https://folio-org.atlassian.net/browse/MODAUD-229) - Support version counter in Version history pane
* [MODAUD-225](https://folio-org.atlassian.net/browse/MODAUD-225) - Handle Repeatable Fields for MARC_BIB and MARC_AUTHORITY Audit Events
* [MODAUD-236](https://folio-org.atlassian.net/browse/MODAUD-236) - Support version counter in Version history pane for MARC records
* [MODAUD-228](https://folio-org.atlassian.net/browse/MODAUD-228) - "400" error is returned by GET and PUT for view and update number of cards in version history config on Eureka environments
* [MODAUD-237](https://folio-org.atlassian.net/browse/MODAUD-237) - Sub-partitions tables have wrong date ranges
* [FOLIO-4233](https://folio-org.atlassian.net/browse/FOLIO-4233) - Update to mod-audit Java 21
* [MODAUD-238](https://folio-org.atlassian.net/browse/MODAUD-238) - Make adjustments for MARC repeatable fields diff calculation and format the values
* [MODAUD-240](https://folio-org.atlassian.net/browse/MODAUD-240) - 999 subfield order is changed creating a log entry of "Updated" in MARC authority version history

## 2.10.2 2025-01-10

* [MODAUD-216](https://folio-org.atlassian.net/browse/MODAUD-199) - Remove PII from mod-audit logs

## 2.10.1 2024-11-19

* [MODAUD-199](https://folio-org.atlassian.net/browse/MODAUD-199) - Make pubsub permission changes.


## 2.10.0 2024-10-30

* [MODAUD-185](https://folio-org.atlassian.net/browse/MODAUD-185) - Unpin jackson fixing Number Parse DoS (PRISMA-2023-0067)
* [MODAUD-186](https://folio-org.atlassian.net/browse/MODAUD-186) - Upgrade `holdings-storage` to 7.0
* [MODAUD-187](https://folio-org.atlassian.net/browse/MODAUD-187) - Review and cleanup Module Descriptors for mod-audit
* [MODAUD-189](https://folio-org.atlassian.net/browse/MODAUD-189) - Upgrade `inventory` to 14.0
* [MODAUD-190](https://folio-org.atlassian.net/browse/MODAUD-190) - Upgrade `holdings-storage` to 8.0
* [MODAUD-192](https://folio-org.atlassian.net/browse/MODAUD-192) - Upgrade RMB version to V.35.3.0
* [MODPUBSUB-301](https://folio-org.atlassian.net/browse/MODPUBSUB-301) - Revert permission changes for MODPUBSUB-301

## 2.9.0 2024-03-21

* [MODAUD-174](https://issues.folio.org/browse/MODAUD-174) - Consume piece change events and implement endpoints
* [MODAUD-176](https://issues.folio.org/browse/MODAUD-176) - Check-ins are automatically backdated to 5 hours later (DUNY/Dominican)
* [MODAUD-177](https://issues.folio.org/browse/MODAUD-177) - Link on a virtual item is available in circ log if fees/fines is applied
* [MODAUD-180](https://issues.folio.org/browse/MODAUD-180) - Modify SQL query for /status-change-history to return records if claimingInterval changed
* [MODAUD-181](https://issues.folio.org/browse/MODAUD-181) - Make additional properties to true in user.json
* [MODAUD-183](https://issues.folio.org/browse/MODAUD-183) - mod-audit Quesnelia 2024 R1 - RMB v35.2.x update
* [FOLIO-3944](https://issues.folio.org/browse/FOLIO-3944) - Upgrade the Actions used by API-related GitHub Workflows

## 2.8.0 2023-10-11

* [MODAUD-110](https://issues.folio.org/browse/MODAUD-110) - Logging improvement
* [MODAUD-161](https://issues.folio.org/browse/MODAUD-161) - Use GitHub Workflows api-lint and api-schema-lint and api-doc
* [MODAUD-164](https://issues.folio.org/browse/MODAUD-164) - Allow action types for loan info
* [MODAUD-166](https://issues.folio.org/browse/MODAUD-166) - Update to Java 17
* [MODAUD-167](https://issues.folio.org/browse/MODAUD-167) - Upgrade folio-kafka-wrapper to 3.0.0 version
* [MODAUD-168](https://issues.folio.org/browse/MODAUD-168) - Incorrect Patron name shown in Circulation log as source for Change Due Date

## 2.7.0 2023-02-23

* [MODAUD-137](https://issues.folio.org/browse/MODAUD-137) - Logging improvement - Configuration
* [MODAUD-140](https://issues.folio.org/browse/MODAUD-140) - Create tables to store audit logs for orders and order lines
* [MODAUD-141](https://issues.folio.org/browse/MODAUD-141) - Add Kafka infrastructure code to mod-audit
* [MODAUD-142](https://issues.folio.org/browse/MODAUD-142) - Implement kafka consumer for Order Change events
* [MODAUD-143](https://issues.folio.org/browse/MODAUD-143) - Implement kafka consumer for Order Line Change events
* [MODAUD-144](https://issues.folio.org/browse/MODAUD-144) - Implement endpoint to retrieve Order Change events
* [MODAUD-145](https://issues.folio.org/browse/MODAUD-145) - Implement endpoint to retrieve Order Line Change events
* [MODAUD-149](https://issues.folio.org/browse/MODAUD-149) - Upgrades: Jackson 2.14.1, mod-pubsub 2.7.0, RMB 35.0.4
* [MODAUD-152](https://issues.folio.org/browse/MODAUD-152) - Align the module with API breaking change
* [MODAUD-154](https://issues.folio.org/browse/MODAUD-154) - Add Additional Info in Description column of Circulation Log

## 2.6.0 - Released
This release contains interface and module upgrades

[Full Changelog](https://github.com/folio-org/mod-audit/compare/v2.5.0...v2.6.0)

### Technical tasks
* [MODAUD-133](https://issues.folio.org/browse/MODAUD-133) - mod-audit: Upgrade RAML Module Builder
* [MODAUD-128](https://issues.folio.org/browse/MODAUD-128) - Supports interface 'users' version 16.0

## 2.5.0 - Released
This release contains bug fixes and other technical improvements for the circulation log

[Full Changelog](https://github.com/folio-org/mod-audit/compare/v2.4.0...v2.5.0)

### Stories
* [MODAUD-121](https://issues.folio.org/browse/MODAUD-121) - mod-audit - Remove vertx-completable-future for Morning Glory (2022 R2)
* [MODAUD-113](https://issues.folio.org/browse/MODAUD-113) - Replace ExtendedAsyncResult by PgUtil in AuditDataImpl
* [MODAUD-111](https://issues.folio.org/browse/MODAUD-111) - RMB v34 upgrade - Morning Glory 2022 R2 module release

### Bug Fixes
* [MODAUD-123](https://issues.folio.org/browse/MODAUD-123) - Notice events for users with tags does not appear in circulation log
* [MODAUD-119](https://issues.folio.org/browse/MODAUD-119) - Spring4Shell Morning Glory R2 2022 (CVE-2022-22965)

## 2.2.0 - Released
This release contains bug fixes and other improvements for the circulation log

[Full Changelog](https://github.com/folio-org/mod-audit/compare/v2.1.0...v2.2.0)

### Stories
* [MODAUD-86](https://issues.folio.org/browse/MODAUD-86) - Add support for NOTICE_ERROR log event type
* [MODAUD-81](https://issues.folio.org/browse/MODAUD-81) - align dependency versions affected by Inventory's Optimistic Locking
* [MODAUD-38](https://issues.folio.org/browse/MODAUD-38) - Source for ManualBlock deleting

### Bug Fixes
* [MODAUD-90](https://issues.folio.org/browse/MODAUD-90) - Screen is hanging on a loading screen when trying to view loan details on the Circulation log page
* [MODAUD-88](https://issues.folio.org/browse/MODAUD-88) - The Description column is empty after applying the filter "staff information only added" in the "Circulation log" pane

## 2.1.0 - Released
This release contains bug fixes and RMB update

### Stories
* [MODAUD-60](https://issues.folio.org/browse/MODAUD-60) - mod-audit: RMB Update

### Bug Fixes
* [MODAUD-79](https://issues.folio.org/browse/MODAUD-79) - Both Expired and Pickup expired request statuses are shown as Expired in circulation log
* [MODAUD-83](https://issues.folio.org/browse/MODAUD-83) - Update mod-pubsub-client dependency to v2.3.0

## 2.0.4 - Released
This release contains bug fix for check out through override processing

[Full Changelog](https://github.com/folio-org/mod-audit/compare/v2.0.3...v2.0.4)

### Bug fixes
* [MODAUD-77](https://issues.folio.org/browse/MODAUD-77) - Check out through override support

## 2.0.3 - Released
This release contains bug fixes for request source

[Full Changelog](https://github.com/folio-org/mod-audit/compare/v2.0.2...v2.0.3)

### Bug Fixes
* [MODAUD-74](https://issues.folio.org/browse/MODAUD-74) - Invalid source value for requests actions

## 2.0.2 - Released
This release contains bug fixes for anonymization

[Full Changelog](https://github.com/folio-org/mod-audit/compare/v2.0.1...v2.0.2)

### Bug Fixes
* [MODAUD-58](https://issues.folio.org/browse/MODAUD-58) - Incomplete anonymization in Circulation log

## 2.0.1 - Released
This release contains bug fixes

[Full Changelog](https://github.com/folio-org/mod-audit/compare/v2.0.0...v2.0.1)

### Bug Fixes
* [MODAUD-66](https://issues.folio.org/browse/MODAUD-66) - User ID is `undefined` for Notice entries
* [MODAUD-64](https://issues.folio.org/browse/MODAUD-64) - Filter circulation log on description is not working
* [MODAUD-62](https://issues.folio.org/browse/MODAUD-62) - Remove sample data for Circulation Log App
* [MODAUD-54](https://issues.folio.org/browse/MODAUD-54) - Click on item barcode results in error message

## 2.0.0 - Released
This release contains improvements and RMB update

[Full Changelog](https://github.com/folio-org/mod-audit/compare/v1.0.3...v2.0.0)

### Stories
* [MODAUD-47](https://issues.folio.org/browse/MODAUD-47) - mod-audit: RMB Update
* [MODAUD-50](https://issues.folio.org/browse/MODAUD-50) - Update Age to lost data in Circulation log
* [MODAUD-51](https://issues.folio.org/browse/MODAUD-51) - Add personal data disclosure form
* [MODAUD-57](https://issues.folio.org/browse/MODAUD-57) - Add support for log records for requests created through override

## 1.0.3 - Released
This patch release contains improvements and bug fixes for requests and fees/fines log actions

[Full Changelog](https://github.com/folio-org/mod-audit/compare/v1.0.2...v1.0.3)

### Bug Fixes
 * [MODAUD-31](https://issues.folio.org/browse/MODAUD-31) - Requests log actions - improvements
 * [MODAUD-41](https://issues.folio.org/browse/MODAUD-41) - Fees/fines records in Circulation log do not contain item details

## 1.0.2 - Released
The primary focus of this release was to update RMB and module logging

[Full Changelog](https://github.com/folio-org/mod-audit/compare/v1.0.1...v1.0.2)

### Stories
* [MODAUD-37](https://issues.folio.org/browse/MODAUD-37) ERROR StatusLogger Unrecognized format specifier [d]
* [MODAUD-34](https://issues.folio.org/browse/MODAUD-34) mod-audit: RMB Update

## 1.0.1 - Released
The primary focus of this release was to fix issues with Notices, Loans and Manual Blocks actions logging

[Full Changelog](https://github.com/folio-org/mod-audit/compare/v1.0.0...v1.0.1)

### Stories
* [MODAUD-29](https://issues.folio.org/browse/MODAUD-29) Template name in Notices log description and Source filed in Loan log record are empty
* [MODAUD-28](https://issues.folio.org/browse/MODAUD-28) User barcode isn't present in log record entry

## 1.0.0 - Released
The primary focus of this release was to implement Circulation Audit Logs logic

[Full Changelog](https://github.com/folio-org/mod-audit/compare/v0.0.4...v1.0.0)

### Stories
* [MODAUD-24](https://issues.folio.org/browse/MODAUD-24) Migrate mod-audit to JDK 11
* [MODAUD-23](https://issues.folio.org/browse/MODAUD-23) mod-audit: RMB Update
* [MODAUD-21](https://issues.folio.org/browse/MODAUD-23) Notices: log actions
* [MODAUD-20](https://issues.folio.org/browse/MODAUD-20) Check In/Check Out: log actions
* [MODAUD-19](https://issues.folio.org/browse/MODAUD-19) Requests: log actions
* [MODAUD-18](https://issues.folio.org/browse/MODAUD-18) Blocks: log actions
* [MODAUD-17](https://issues.folio.org/browse/MODAUD-17) Fees/fines: log actions
* [MODAUD-16](https://issues.folio.org/browse/MODAUD-16) Loans: log actions
* [MODAUD-15](https://issues.folio.org/browse/MODAUD-15) LogEventListener for mod-audit
* [MODAUD-14](https://issues.folio.org/browse/MODAUD-14) LogEventSender library
* [MODAUD-12](https://issues.folio.org/browse/MODAUD-12) Prepare DB tables for Circulation Audit Logs
* [MODAUD-10](https://issues.folio.org/browse/MODAUD-10) GET /audit-data/logs for Circulation Audit Logs
* [MODAUD-9](https://issues.folio.org/browse/MODAUD-9) Create LogRecord schema

## 0.0.4
* https://issues.folio.org/browse/MODAUD-7: Update RMB to v30.0.2

## 0.0.3
* update some dependencies

## 0.0.2
* update to RAML 1.0 and RMB 21 

## 0.0.1
* provide basic CRUD for audit data
