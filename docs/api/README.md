# Documentation for notification-gateway

<a name="documentation-for-api-endpoints"></a>
## Documentation for API Endpoints

All URIs are relative to *https://portal.ingress.local/notification-gateway*

| Class | Method | HTTP request | Description |
|------------ | ------------- | ------------- | -------------|
| *NotificationsApi* | [**apiNgNotificationDiseasePost**](Apis/NotificationsApi.md#apingnotificationdiseasepost) | **POST** /api/ng/notification/disease | Create a disease notification that will be send to the specified target |
*NotificationsApi* | [**apiNgNotificationPathogenPost**](Apis/NotificationsApi.md#apingnotificationpathogenpost) | **POST** /api/ng/notification/pathogen | Create a pathogen notification that will be send to the specified target |
| *ReportsApi* | [**apiNgReportsBedOccupancyPost**](Apis/ReportsApi.md#apingreportsbedoccupancypost) | **POST** /api/ng/reports/bedOccupancy | Create a bedOccupancy notification that will be send to the specified target |


<a name="documentation-for-models"></a>
## Documentation for Models



<a name="documentation-for-authorization"></a>
## Documentation for Authorization

<a name="bearerAuth"></a>
### bearerAuth

- **Type**: HTTP Bearer Token authentication (jwt)

<a name="remoteIP"></a>
### remoteIP

- **Type**: API key
- **API key parameter name**: x-real-ip
- **Location**: HTTP header

