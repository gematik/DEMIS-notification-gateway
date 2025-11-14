<img align="right" width="200" height="37" src="media/Gematik_Logo_Flag.png"/> <br/>

# Release notes notification-gateway

## 6.7.4
- add feature flag feature.flag.snapshot.6.active for address creating in notified person and organizations

## 6.7.3
- add validation for notifiedPersonAnonymous in disease notifications
- upgrade notification-builder-library to 8.0.0

## 6.7.2
- rename feature flag feature.flag.snapshot.6.active to feature.flag.pathogenStrictSnapshotActive

## 6.7.1
- add feature flag feature.flag.snapshot.6.active

## 6.7.0
- added full terminology system versions support to disease notifications
- fix declined notByName follow up notification by adding addressUse extension to patient profile
- remove automatic header version switch from deployment resource
- remove FEATURE_FLAG_SNAPSHOT_5_3_0_ACTIVE
- bump spring parent to 2.14.2
- improved handling of contained CodeSystem resources
- upgraded dependencies

## 6.6.0
- remove Comparator from Quantity as it will not be used in profiles
- remove unused secrets and volumes and environment variables from helm charts
- update spring-parent version to 2.13.4

## 6.5.1
- add severity level in case of validation errors
- remove feature flag FEATURE_FLAG_HOSP_COPY_CHECKBOXES
- fix: allow monthly precision for date fields 'recordedDate' and 'onset'

## 6.5.0
- add Support for new API Endpoint for Backend-Services (NES, RPS)
- set headers x-fhir-profile and x-fhir-api-version for rps and nps requests
- add support for follow-up notifications through possibility to create a notifiedPersonAnonymous and fixes
- fix: initial notification id used as notification identifier instead of
  relates-to parameter and fixes
 
## 6.4.2
- Update spring-parent to 2.12.11
- fix laboratory notification creation process

## 6.4.1
- Update Spring-Parent 
- fix missing version for codes in different resources in laboratory notification creation
- add default feature flags FEATURE_FLAG_NOTIFICATIONS_7_3, FEATURE_FLAG_SNAPSHOT_5_3_0_ACTIVE to values.yaml

## 6.4.0
- Removed Notification-Object from the data model
- Updated dependencies
- add pathogen/laboratory strict processing
- use AddressDataBuilder 

## 6.3.9
- updating dependencies

## 6.3.7
- change base chart to istio hostnames
- updating dependencies
- §7.1 creation code is extended with §7.3 pathogen notifications.
- update method code for resistance gene oberservations

## 6.3.6
- Updated ospo-resources for adding additional notes and disclaimer
- setting new ressources in helm chart
- setting new timeouts and retries in helm chart
- updating dependencies

## 6.3.2
- second official GitHub-Release
- updated dependencies
- remove Komfort-Client
- update data model
- adding  different statuses of a message
- adding diagnostic note
- align address details for affected person with pathogen certificates
- laboratory order number can be entered in the reporting portal
- updated token handling
- deactivation of not authorized notifications


## 3.0.0 (2023-12-15)
- first official GitHub-Release
- Migration to SpringBoot 3.2.1 as standalone JAR
