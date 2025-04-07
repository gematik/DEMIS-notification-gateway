package de.gematik.demis.notificationgateway.common.proxies;

/*-
 * #%L
 * DEMIS Notification-Gateway
 * %%
 * Copyright (C) 2025 gematik GmbH
 * %%
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
 * #L%
 */

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.impl.RestfulClientFactory;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import de.gematik.demis.notificationgateway.common.logging.CustomLoggingInterceptor;
import de.gematik.demis.notificationgateway.common.properties.ApplicationProperties;
import de.gematik.demis.notificationgateway.common.properties.LoggingProperties;
import de.gematik.demis.notificationgateway.common.services.fhir.FhirObjectCreationService;
import de.gematik.demis.notificationgateway.common.utils.Token;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BundlePublisher {

  private final FhirContext fhirContext = FhirContext.forR4();
  private final LoggingProperties loggingProperties;
  private final ApplicationProperties applicationProperties;
  private final FhirObjectCreationService fhirObjectCreationService;

  public Parameters postRequest(
      @NonNull Bundle bundle,
      @NonNull String url,
      @NonNull String operationName,
      @NonNull String fhirProfile,
      @NonNull String fhirProfileVersion,
      @NonNull Token token) {
    Parameters parameters = fhirObjectCreationService.createParameters(bundle);
    IGenericClient client = createClient(url, fhirProfile, fhirProfileVersion, token);
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
            .execute();
    log.info("Processing operation {} successful.", operationName);
    return parametersResult;
  }

  private IGenericClient createClient(
      String url, String fhirProfile, String fhirProfileVersion, Token token) {
    RestfulClientFactory clientFactory =
        (RestfulClientFactory) fhirContext.getRestfulClientFactory();
    // Override default, unchangeable properties from generic interface
    log.debug("Setting HTTP Connection Timeouts from properties");
    clientFactory.setConnectTimeout(applicationProperties.getHttpConnectionTimeoutMilliseconds());
    clientFactory.setConnectionRequestTimeout(
        applicationProperties.getHttpConnectionPoolTimeoutMilliseconds());
    clientFactory.setSocketTimeout(applicationProperties.getHttpSocketTimeoutMilliseconds());
    clientFactory.setServerValidationMode(ServerValidationModeEnum.NEVER);
    final IGenericClient client = fhirContext.newRestfulGenericClient(url);
    registerUserAgentHeader(client, fhirProfile, fhirProfileVersion);
    setLogging(client);
    client.registerInterceptor(new BearerTokenAuthInterceptor(token.asText()));
    return client;
  }

  private void registerUserAgentHeader(
      IGenericClient notificationApiClient, String fhirProfile, String fhirProfileVersion) {
    notificationApiClient.registerInterceptor(
        new FhirRequestInterceptor(
            fhirProfile, fhirProfileVersion, applicationProperties.identifier()));
  }

  private void setLogging(IGenericClient client) {
    if (loggingProperties.isUseLoggingInterceptor()) {
      CustomLoggingInterceptor loggingInterceptor = new CustomLoggingInterceptor();
      loggingInterceptor.setLogRequestSummary(true);
      loggingInterceptor.setLogRequestHeaders(true);
      loggingInterceptor.setLogResponseHeaders(true);
      client.registerInterceptor(loggingInterceptor);
    }
  }
}
