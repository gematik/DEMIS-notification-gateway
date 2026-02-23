# NotificationsApi

All URIs are relative to *https://portal.ingress.local/notification-gateway*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**notificationDiseasePost**](NotificationsApi.md#notificationDiseasePost) | **POST** /notification/disease | Create a disease notification that will be send to the specified target |
| [**notificationPathogenPost**](NotificationsApi.md#notificationPathogenPost) | **POST** /notification/pathogen | Create a pathogen notification that will be send to the specified target |


<a name="notificationDiseasePost"></a>
# **notificationDiseasePost**
> Object notificationDiseasePost(body)

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

<a name="notificationPathogenPost"></a>
# **notificationPathogenPost**
> Object notificationPathogenPost(body)

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

