<img src="media/Gematik_Logo_Flag.png" alt="Gematik Logo" width="200" height="37">

# notification-gateway

## Description

This service is the backend of the notification portal website.
The gateway receives requests from the notification portal and, if necessary, delegates them to the
DEMIS core services.
It's main purpose is to create FHIR bundles from the user input from the portal and send them to the
corresponding DEMIS core service, such as NES (notification entry service) and RPS (report processing service).

The notification gateway builds FHIR documents. These are sent to the DEMIS core FHIR interfaces. Supported use cases
are:

- pathogen notification (authentication required, but can be disabled)
- disease notification (authentication required)
- bed-occupancy report (authentication required)

Also, the gateways provides access to hospital location data. (no authentication required)

<details>
  <summary>Table of Contents</summary>
  <ol>
    <li>
      <a href="#about-the-project">About The Project</a>
      <ul>
        <li><a href="#quality-gate">Quality Gate</a></li>
        <li><a href="#release-notes">Release Notes</a></li>
      </ul>
    </li>
    <li><a href="#internals">Internals</a></li>
    <li><a href="#usage">Usage</a></li>
    <li><a href="#security">Security</a></li>
    <li><a href="#security-policy">Security Policy</a></li>
    <li><a href="#contributing">Contributing</a></li>
    <li><a href="#license">License</a></li>
    <li><a href="#contact">Contact</a></li>
  </ol>
</details>

## About the project

### Quality Gate

[![Quality Gate Status](https://sonar.prod.ccs.gematik.solutions/api/project_badges/measure?project=de.gematik.demis%3Anotification-gateway&metric=alert_status&token=sqb_8360b88be549acbfe14167b50216ece987e9a58c)](https://sonar.prod.ccs.gematik.solutions/dashboard?id=de.gematik.demis%3Anotification-gateway)
[![Vulnerabilities](https://sonar.prod.ccs.gematik.solutions/api/project_badges/measure?project=de.gematik.demis%3Anotification-gateway&metric=vulnerabilities&token=sqb_8360b88be549acbfe14167b50216ece987e9a58c)](https://sonar.prod.ccs.gematik.solutions/dashboard?id=de.gematik.demis%3Anotification-gateway)
[![Bugs](https://sonar.prod.ccs.gematik.solutions/api/project_badges/measure?project=de.gematik.demis%3Anotification-gateway&metric=bugs&token=sqb_8360b88be549acbfe14167b50216ece987e9a58c)](https://sonar.prod.ccs.gematik.solutions/dashboard?id=de.gematik.demis%3Anotification-gateway)
[![Code Smells](https://sonar.prod.ccs.gematik.solutions/api/project_badges/measure?project=de.gematik.demis%3Anotification-gateway&metric=code_smells&token=sqb_8360b88be549acbfe14167b50216ece987e9a58c)](https://sonar.prod.ccs.gematik.solutions/dashboard?id=de.gematik.demis%3Anotification-gateway)
[![Lines of Code](https://sonar.prod.ccs.gematik.solutions/api/project_badges/measure?project=de.gematik.demis%3Anotification-gateway&metric=ncloc&token=sqb_8360b88be549acbfe14167b50216ece987e9a58c)](https://sonar.prod.ccs.gematik.solutions/dashboard?id=de.gematik.demis%3Anotification-gateway)
[![Coverage](https://sonar.prod.ccs.gematik.solutions/api/project_badges/measure?project=de.gematik.demis%3Anotification-gateway&metric=coverage&token=sqb_8360b88be549acbfe14167b50216ece987e9a58c)](https://sonar.prod.ccs.gematik.solutions/dashboard?id=de.gematik.demis%3Anotification-gateway)

### Release Notes

See [ReleaseNotes](ReleaseNotes.md) for all information regarding the (newest) releases.

# Internals

| Aspect                 | Tools/Frameworks    | Details                                                                                          |
|------------------------|---------------------|--------------------------------------------------------------------------------------------------|
| Application Framework  | Spring Boot         | Version 2.7.x                                                                                    |
| Security               | Spring Security     | JSON Web Token (JWT) based                                                                       |
| Java Version           | Java                | Version 21                                                                                       |
| Build Management       | Maven               | Used as the software project management tool                                                     |
| Testing                | JUnit 5, Mockito    | Used for testing purposes                                                                        |
| Versioning             | Semantic Versioning | Follows the Semantic Versioning 2.0.0 specification, as defined by [SemVer](https://semver.org/) |
| Logging                | Logback             | Used for logging                                                                                 |
| CI/CD                  | Jenkins             | Used for continuous integration and continuous deployment                                        |
| Quality Gate           | SonarLint, SonarQube | Used for code quality analysis                                                                   |

# Usage

## Build
When you checkout the repository you have to generate class from the Open-API specification. Usually this is done in
Maven automatically. However, if you are using IntelliJ you have to go to the Maven-Tab and select
**Generate Sources and Update Folders For All Projects**. See https://stackoverflow.com/a/46812593


To build the project locally using Maven, you can use the following goals:

```
mvn clean package -DskipTests
```
The artifact can be found in the /target/notification-gateway-x.y.z.war directory
You can deploy the war file in a servlet container
To build the Docker image, you can use the `docker profile`:

```
mvn clean install -Pdocker
```
Please note that you should replace x.y.z with the actual version number of the project.

it will be available as `notification-gateway:latest`

## Tests

To run the tests locally, you can use the following command:

```
mvn clean verify
```

## Run
To run the gateway locally in an embedded Tomcat server, start the [NotificationGatewayApplication.java](src/main/java/de/gematik/demis/notificationgateway/NotificationGatewayApplication.java).

Alternatively, you can use the [application-local.properties](src/main/resources/application-local.properties) file in combination with the [demis-localhost.yml](https://gitlab.prod.ccs.gematik.solutions/git/demis/demis/-/blob/master/demis_localhost.yml) configuration.

## Data model / Generated code

Parts of the data model are generated by the OpenAPI Generator. You can see the configuration in the `openapi-generator-maven-plugin` in the [pom.xml](pom.xml). The source data is taken from [notification.yaml](src/main/resources/spec/schemas-spec.yml), and this generation process occurs during the `generate-sources` phase.

The data model is maintained here:

- [studio.apicur.io](https://studio.apicur.io)
- [gematik-demis.stoplight.io](https://gematik-demis.stoplight.io/studio/meldeportal-openapi:main)

If you have problems with unfound classes in the code after calling `mvn clean compile`, try the
reload / update folders button in your Maven tab.

## Swagger

When running locally, swagger can be found at:

http://localhost:8080/swagger-ui/index.html

Each configuration property can also be overridden with the definition of environment variables, following the SpringBoot convention (name in uppercase, snake case - e.g. from `http.connection.timeout.ms` to `HTTP_CONNECTION_TIMEOUT_MS`).

## Configuration

The Spring application properties of the service.

| Feature                                         | Parameter                                 | Description                                          | Default | Example values |
|-------------------------------------------------|-------------------------------------------|------------------------------------------------------|---------|----------------|
| HSL data queries                                | HOSPITAL_LOCATION_SERVICE_URL_ENABLED     | On-off switch for unauthenticated HLS data queries   | false   | true, false    |
| pathogen notification                           | PATHOGEN_AUTHENTICATION_REQUIRED          | Support unauthenticated pathogen notifications       | true    | true, false    |

# Endpoints

| Endpoint                               | Description                                                                                                                       |
|----------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------|
| `/api/ng/notification/laboratory`      | This is a non-secured endpoint for test result notifications.                                                                     |
| `/api/ng/notification/hospitalization` | This endpoint is used for hospitalization notifications.                                                                          |
| `/api/ng/reports/bedOccupancy`         | This endpoint is used for bed occupancy reports.                                                                                  |
| `/health`                              | Health endpoint to check the overall health of the application.                                                                   |
| `/health/liveness`                     | Endpoint to check the liveness status of the application.                                                                         |
| `/health/readiness`                    | Endpoint to check the readiness status of the application.                                                                        |
| `/api-spec`                            | The [api-spec](src/main/resources/spec/api-spec.yml) file contains the specification for all the exposed endpoints.               |
| `/notification-gateway`                | The gateway can be accessed locally at [http://localhost:9042/notification-gateway](http://localhost:9042/notification-gateway) . |


## Security

The entrypoint for the security configuration is [WebSecurityConfiguration](../src/main/java/de/gematik/demis/notificationgateway/security/WebSecurityConfiguration.java).
Here you can find
- the CORS configuration
- authentication settings for all endpoints

The endpoints specified above (i.e. test, hospitalization and bed occupancy) can be invoked after authentication or anonymously (no authentication available in Spring Context). Currently, there is 2 distinct options for authentication available: Certificate-based and Federated Identity Providers (e.g. Authenticator, BundID, MeinUnternehmenskonto).  
[AuthenticationManagerResolver](../src/main/java/de/gematik/demis/notificationgateway/security/AuthenticationManagerResolver.java) plays a crucial role for these flows. Namely, it registers dedicated token decoders DEMIS issued tokens. 

In order to facilitate the sending of notifications to DEMIS-Core for the principals of Federated IDPs a custom AuthenticationManager was implemented [FederatedIdentityProviderAuthenticationManager](../src/main/java/de/gematik/demis/notificationgateway/security/fidp/FederatedIdentityProviderAuthenticationManager.java), 
which ensures the Authentication object in Spring Security Context.

Additionally, [JwtAuthenticationFilter](../src/main/java/de/gematik/demis/notificationgateway/security/oauth2/JwtAuthenticationFilter.java) was adapted to allow tokens issued by Federated Identity Providers by verifying their issuer claim.

## Security Policy
If you want to see the security policy, please check our [SECURITY.md](.github/SECURITY.md).

## Contributing
If you want to contribute, please check our [CONTRIBUTING](.github/CONTRIBUTING.md).

## License

Copyright 2021-2025 gematik GmbH

EUROPEAN UNION PUBLIC LICENCE v. 1.2

EUPL © the European Union 2007, 2016

See the [LICENSE](LICENSE.md) for the specific language governing permissions and limitations under the license.

## Additional Notes and Disclaimer from gematik GmbH

1. Copyright notice: Each published work result is accompanied by an explicit statement of the license conditions for use. These are regularly typical conditions in connection with open source or free software. Programs described/provided/linked here are free software, unless otherwise stated.
2. Permission notice: Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
    1. The copyright notice (Item 1) and the permission notice (Item 2) shall be included in all copies or substantial portions of the Software.
    2. The software is provided "as is" without warranty of any kind, either express or implied, including, but not limited to, the warranties of fitness for a particular purpose, merchantability, and/or non-infringement. The authors or copyright holders shall not be liable in any manner whatsoever for any damages or other claims arising from, out of or in connection with the software or the use or other dealings with the software, whether in an action of contract, tort, or otherwise.
    3. We take open source license compliance very seriously. We are always striving to achieve compliance at all times and to improve our processes. If you find any issues or have any suggestions or comments, or if you see any other ways in which we can improve, please reach out to: ospo@gematik.de
3. Please note: Parts of this code may have been generated using AI-supported technology. Please take this into account, especially when troubleshooting, for security analyses and possible adjustments.

See [LICENSE](LICENSE.md).

## Contact
Email to: [DEMIS Entwicklung](mailto:demis-entwicklung@gematik.de?subject=%5BGitHub%5D%20notification-gateway)
