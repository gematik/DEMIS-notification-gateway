
<img src="../media/Gematik_Logo_Flag.png" alt="Gematik Logo" width="200" height="37">


# notification-gateway

## Description

This service is the backend of the notification portal website.
The gateway receives requests from the notification portal and, if necessary, delegates them to the
DEMIS core services.
It's main purpose is to create FHIR bundles from the user input from the portal and send them to the
corresponding DEMIS core service, such as NES (notification entry service) and RPS (report processing service).

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

See [ReleaseNotes](../ReleaseNotes.md) for all information regarding the (newest) releases.


# Internals

| Aspect                 | Tools/Frameworks    | Details                                                                                       |
|------------------------|---------------------|-----------------------------------------------------------------------------------------------|
| Application Framework  | Spring Boot         | Version 2.7.x                                                                                  |
| Security               | Spring Security     | JSON Web Token (JWT) based                                                                    |
| Java Version           | Java                | Version 11                                                                                    |
| Build Management       | Maven               | Used as the software project management tool                                                  |
| Testing                | JUnit 5, Mockito    | Used for testing purposes                                                                     |
| Versioning             | Semantic Versioning | Follows the Semantic Versioning 2.0.0 specification, as defined by [SemVer](https://semver.org/) |
| Logging                | Logback             | Used for logging                                                                              |
| CI/CD                  | Jenkins             | Used for continuous integration and continuous deployment                                      |
| Quality Gate           | SonarLint, SonarQube | Used for code quality analysis                                                                 |

**Hint:**

For usage or perform the tests, sample data must be added to the files:

* src/main/resources/application.properties.template
  * The following values has to be set:
    * token.client.secret.lab
    * token.client.secret.hospital
    * truststore.password
    * auth.cert.password
    * testuser.auth.cert.password
* src/main/resources/certs/nginx.truststore.template
    * Needs to be filled with a CA-Certificate and an End-Entity-Certificate which is issued by the CA. 
* src/test/resources/app.properties.template
    * The following values has to be set:
      * 0.idp.lab.authcertkeystore
      * 0.idp.lab.authcertpassword
      * 0.idp.hospital.authcertkeystore
      * 0.idp.hospital.authcertpassword

In addition for authentication, the two keystores DEMIS.p12 (for fetching a token) and Testuser.p12 (for testuser) are required and should be stored under src/main/resources/certs.
Afterwards, please remove the ".template" suffix so that the files can be used in the code.

# Usage

## Build
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

it will be available as `europe-west3-docker.pkg.dev/gematik-all-infra-prod/demis-dev/notification-gateway:latest`

## Tests

To run the tests locally, you can use the following command:

```
mvn clean verify

The tests by default use the offline JWT Token Decoder. In the `application-test.properties` the option `jwt.offline.decoder.enabled` is set to true, so during the tests, instead of contacting the Ulbirch IDP, an offline class will be used.
```
## Run
To run the gateway locally in an embedded Tomcat server, start the [NotificationGatewayApplication.java](../src/main/java/de/gematik/demis/notificationgateway/NotificationGatewayApplication.java).

By default, all requests are sent to the [DEV-environment](https://gsltucd01.ltu.int.gematik.de:8001).

Alternatively, you can use the [application-local.properties](../src/main/resources/application-local.properties) file in combination with the [demis-localhost.yml](https://gitlab.prod.ccs.gematik.solutions/git/demis/demis/-/blob/master/demis_localhost.yml) configuration.

## Data model / Generated code

Parts of the data model are generated by the OpenAPI Generator. You can see the configuration in the `openapi-generator-maven-plugin` in the [pom.xml](../pom.xml). The source data is taken from [notification.yaml](../src/main/resources/spec/schemas-spec.yml), and this generation process occurs during the `generate-sources` phase.

The data model is maintained here:

- [studio.apicur.io](https://studio.apicur.io)
- [gematik-demis.stoplight.io](https://gematik-demis.stoplight.io/studio/meldeportal-openapi:main)

If you have problems with unfound classes in the code after calling `mvn clean compile`, try the
reload / update folders button in your Maven tab.

## Configuration

| Configuration File                                               | Description                                                                                                   |
|-----------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| [application.properties](../src/main/resources/application.properties) | Contains the configuration settings for the project.                                                        |
| [demis-configuration](https://gitlab.prod.ccs.gematik.solutions/git/demis/demis-configuration)               | The demis-configuration project contains the properties for different environments. These properties are replaced during the build process for the specified environment. |


Each configuration property can also be overridden with the definition of environment variables, following the SpringBoot convention (name in uppercase, snake case - e.g. from `http.connection.timeout.ms` to `HTTP_CONNECTION_TIMEOUT_MS`).

# Endpoints

| Endpoint                        | Description                                                                                           |
|---------------------------------|-------------------------------------------------------------------------------------------------------|
| `/api/ng/notification/laboratory` | This is a non-secured endpoint for test result notifications.                                         |
| `/api/ng/notification/hospitalization` | This endpoint is used for hospitalization notifications.                                         |
| `/api/ng/reports/bedOccupancy`   | This endpoint is used for bed occupancy reports.                                                    |
| `/health`                        | Health endpoint to check the overall health of the application.                                      |
| `/health/liveness`               | Endpoint to check the liveness status of the application.                                            |
| `/health/readiness`              | Endpoint to check the readiness status of the application.                                           |
| `/api-spec`                      | The [api-spec](../src/main/resources/spec/api-spec.yml) file contains the specification for all the exposed endpoints. |
| `/notification-gateway`          | The gateway can be accessed locally at [http://localhost:9042/notification-gateway](http://localhost:9042/notification-gateway) .                |


## Security
The entrypoint for the security configuration is [WebSecurityConfiguration](../src/main/java/de/gematik/demis/notificationgateway/security/WebSecurityConfiguration.java).
Here you can find
- the CORS configuration
- authentication settings for all endpoints

The endpoints specified above (i.e. test, hospitalization and bed occupancy) can be invoked after authentication or anonymously (no authentication available in Spring Context). Currently, there are 2 distinct options for authentication available: IBM Komfort-Client and Federated Identity Providers (e.g. Authenticator, BundID, MeinUnternehmenskonto).  
[AuthenticationManagerResolver](../src/main/java/de/gematik/demis/notificationgateway/security/AuthenticationManagerResolver.java) plays a crucial role for these flows. Namely, it registers dedicated token decoders for Komfort-Client and DEMIS issued tokens. 

In order to facilitate the sending of notifications to DEMIS-Core for the principals of Federated IDPs a custom AuthenticationManager was implemented [FederatedIdentityProviderAuthenticationManager](../src/main/java/de/gematik/demis/notificationgateway/security/fidp/FederatedIdentityProviderAuthenticationManager.java), 
which ensures the Authentication object in Spring Security Context.

Additionally, [JwtAuthenticationFilter](../src/main/java/de/gematik/demis/notificationgateway/security/oauth2/JwtAuthenticationFilter.java) was adapted to allow tokens issued by Federated Identity Providers by verifying their issuer claim.

## Security Policy

If you want to see the security policy, please check our [SECURITY.md](SECURITY.md).


## Contributing

If you want to contribute, please check our [CONTRIBUTING](CONTRIBUTING.md).

## License

EUROPEAN UNION PUBLIC LICENCE v. 1.2

EUPL © the European Union 2007, 2016

Copyright (c) 2023 gematik GmbH

See [LICENSE](../LICENSE.md).

## Contact

Email to: [DEMIS Entwicklung](mailto:demis-entwicklung@gematik.de?subject=%5BGitHub%5D%20notification-gateway)
