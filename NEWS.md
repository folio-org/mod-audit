## 2.2.0 - Unreleased

## 2.1.0 - Released
This release contains bug fixes and RMB update

### Stories
* [MODAUD-60](https://issues.folio.org/browse/MODAUD-60) - mod-audit: RMB Update

### Bug Fixes
* [MODAUD-79](https://issues.folio.org/browse/MODAUD-79) - Both Expired and Pickup expired request statuses are shown as Expired in circulation log
* [MODAUD-83](https://issues.folio.org/browse/MODAUD-83) - Update mod-pubsub-client dependency to v2.3.0

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
