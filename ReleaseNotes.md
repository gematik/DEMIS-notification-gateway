<img align="right" width="200" height="37" src="media/Gematik_Logo_Flag.png"/> <br/>

# Release notes notification-gateway

### 

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
