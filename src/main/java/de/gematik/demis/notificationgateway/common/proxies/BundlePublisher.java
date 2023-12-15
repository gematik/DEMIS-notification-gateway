/*
 * Copyright [2023], gematik GmbH
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the
 * European Commission – subsequent versions of the EUPL (the "Licence").
 * You may not use this work except in compliance with the Licence.
 *
 * You find a copy of the Licence in the "Licence" file or at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.
 * In case of changes by gematik find details in the "Readme" file.
 *
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */

package de.gematik.demis.notificationgateway.common.proxies;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.impl.RestfulClientFactory;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import de.gematik.demis.enums.KeyStoreType;
import de.gematik.demis.notificationgateway.common.enums.SupportedRealm;
import de.gematik.demis.notificationgateway.common.logging.CustomLoggingInterceptor;
import de.gematik.demis.notificationgateway.common.properties.ApplicationProperties;
import de.gematik.demis.notificationgateway.common.properties.LoggingProperties;
import de.gematik.demis.notificationgateway.common.properties.TLSProperties;
import de.gematik.demis.notificationgateway.common.properties.TestUserProperties;
import de.gematik.demis.notificationgateway.common.services.fhir.FhirObjectCreationService;
import de.gematik.demis.notificationgateway.common.utils.FileUtils;
import de.gematik.demis.token.data.KeyStoreConfigParameter;
import de.gematik.demis.token.services.HttpClientService;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Slf4j
@Component
@RequiredArgsConstructor
@RequestScope
public class BundlePublisher {

  private static final String TEST_USER_POSTAL_CODE = "abcde";

  private final FhirContext fhirContext = FhirContext.forR4();
  private final HttpClientService httpClientService = new HttpClientService();

  private final LoggingProperties loggingProperties;

  private final ApplicationProperties applicationProperties;

  private final TLSProperties tlsProperties;

  private final TestUserProperties testUserProperties;

  private final FhirObjectCreationService fhirObjectCreationService;
  private final TokenProxy tokenProxy;

  public Parameters postRequest(
      @NonNull Bundle bundle,
      @NonNull SupportedRealm realm,
      @NonNull String url,
      @NonNull String operationName,
      @NonNull String remoteAddress,
      @NonNull String fhirProfile,
      @NonNull String fhirProfileVersion) {
    final boolean isTestUser = testUserProperties.isTestIp(remoteAddress);
    if (isTestUser) {
      updatePostalCodeForTestUser(bundle);
    }

    Parameters parameters = fhirObjectCreationService.createParameters(bundle);

    String bearerToken = tokenProxy.fetchToken(realm, isTestUser);
    IGenericClient client =
        Objects.requireNonNull(
            createClient(isTestUser, url, bearerToken, fhirProfile, fhirProfileVersion));
    return processParameters(client, parameters, operationName);
  }

  private Parameters processParameters(
      IGenericClient client, Parameters parameters, String operationName) {
    final Parameters parametersResult =
        client
            .operation()
            .onServer()
            .named(operationName)
            .withParameters(parameters)
            .encodedJson()
            .prettyPrint()
            .execute();
    log.info("Processing operation {} successful.", operationName);
    return parametersResult;
  }

  private IGenericClient createClient(
      boolean isTestUser,
      String url,
      String bearerToken,
      String fhirProfile,
      String fhirProfileVersion) {
    final KeyStoreConfigParameter keyStoreConfigParameter =
        Objects.requireNonNull(
            createKeyStoreConfigParameter(isTestUser), "requireNonNull keyStoreConfigParameter");

    // build the ssl context for the request client for the fhir context
    HttpClient closeableHttpClient =
        Objects.requireNonNull(
            httpClientService.createHttpClient(keyStoreConfigParameter),
            "requireNonNull closeableHttpClient");

    RestfulClientFactory clientFactory =
        (RestfulClientFactory) fhirContext.getRestfulClientFactory();

    // Override default, unchangeable properties from generic interface
    log.debug("Setting HTTP Connection Timeouts from properties");
    clientFactory.setConnectTimeout(applicationProperties.getHttpConnectionTimeoutMilliseconds());
    clientFactory.setConnectionRequestTimeout(
        applicationProperties.getHttpConnectionPoolTimeoutMilliseconds());
    clientFactory.setSocketTimeout(applicationProperties.getHttpSocketTimeoutMilliseconds());

    clientFactory.setHttpClient(closeableHttpClient);
    clientFactory.setServerValidationMode(ServerValidationModeEnum.NEVER);

    var client = fhirContext.newRestfulGenericClient(url);

    registerUserAgentHeader(client, fhirProfile, fhirProfileVersion);

    if (loggingProperties.isUseLoggingInterceptor()) {
      CustomLoggingInterceptor loggingInterceptor = new CustomLoggingInterceptor();
      loggingInterceptor.setLogRequestSummary(true);
      loggingInterceptor.setLogRequestHeaders(true);
      loggingInterceptor.setExcludedRequestHeaders(loggingProperties.getExcludedRequestHeaders());
      loggingInterceptor.setLogResponseHeaders(true);
      client.registerInterceptor(loggingInterceptor);
    }

    client.registerInterceptor(new BearerTokenAuthInterceptor(bearerToken));
    return client;
  }

  private void registerUserAgentHeader(
      IGenericClient notificationApiClient, String fhirProfile, String fhirProfileVersion) {
    notificationApiClient.registerInterceptor(
        new FhirRequestInterceptor(
            fhirProfile, fhirProfileVersion, applicationProperties.identifier()));
  }

  @Nullable
  private KeyStoreConfigParameter createKeyStoreConfigParameter(boolean isTestUser) {
    String filePath = tlsProperties.getAuthCertPath();
    if (isTestUser) {
      filePath = testUserProperties.getAuthCertPath();
    }
    final InputStream trustStoreInputStream =
        FileUtils.getFileInput(
            tlsProperties.getTruststorePath(), applicationProperties.isLegacyModeEnabled());

    final InputStream keyStoreInputStream =
        FileUtils.getFileInput(filePath, applicationProperties.isLegacyModeEnabled());

    if (trustStoreInputStream == null || keyStoreInputStream == null) {
      log.info("Failed to load Truststore or Keystore");
      return null;
    }
    var builder =
        KeyStoreConfigParameter.builder()
            .authCertKeyStoreType(KeyStoreType.JKS)
            .authCertKeyStore(keyStoreInputStream)
            .trustStore(trustStoreInputStream)
            .trustStorePassword(tlsProperties.getTruststorePassword());

    if (isTestUser) {
      return builder
          .authCertAlias(testUserProperties.getAuthCertAlias())
          .authCertPassword(testUserProperties.getAuthCertPassword())
          .build();
    }
    return builder
        .authCertAlias(tlsProperties.getAuthCertAlias())
        .authCertPassword(tlsProperties.getAuthCertPassword())
        .build();
  }

  private void updatePostalCodeForTestUser(Bundle bundle) {
    var organizations = findResource(bundle, "Organization");
    var patients = findResource(bundle, "Patient");

    organizations.forEach(resource -> updatePostalCodes(((Organization) resource).getAddress()));
    patients.forEach(resource -> updatePostalCodes(((Patient) resource).getAddress()));
  }

  private void updatePostalCodes(List<Address> addresses) {
    addresses.stream()
        .filter(Objects::nonNull)
        .forEach(address -> address.setPostalCode(TEST_USER_POSTAL_CODE));
  }

  private List<Resource> findResource(Bundle response, String resourceName) {
    if (Objects.isNull(resourceName) || Objects.isNull(response)) {
      return Collections.emptyList();
    }

    return response.getEntry().stream()
        .map(Bundle.BundleEntryComponent::getResource)
        .filter(resource -> resourceName.equals(resource.getResourceType().name()))
        .collect(Collectors.toList());
  }
}
