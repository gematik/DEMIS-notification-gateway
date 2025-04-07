# NotificationsApi

All URIs are relative to *https://portal.ingress.local/notification-gateway*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**apiNgNotificationDiseasePost**](NotificationsApi.md#apiNgNotificationDiseasePost) | **POST** /api/ng/notification/disease | Create a disease notification that will be send to the specified target |
| [**apiNgNotificationPathogenPost**](NotificationsApi.md#apiNgNotificationPathogenPost) | **POST** /api/ng/notification/pathogen | Create a pathogen notification that will be send to the specified target |


<a name="apiNgNotificationDiseasePost"></a>
# **apiNgNotificationDiseasePost**
> Object apiNgNotificationDiseasePost(body)

Create a disease notification that will be send to the specified target

### Parameters

|Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **body** | **oas_any_type_not_mapped**|  | [optional] |

### Return type

**Object**

### Authorization

[remoteIP](../README.md#remoteIP), [bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

<a name="apiNgNotificationPathogenPost"></a>
# **apiNgNotificationPathogenPost**
> Object apiNgNotificationPathogenPost(body)

Create a pathogen notification that will be send to the specified target

### Parameters

|Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **body** | **oas_any_type_not_mapped**|  | [optional] |

### Return type

**Object**

### Authorization

[remoteIP](../README.md#remoteIP)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

