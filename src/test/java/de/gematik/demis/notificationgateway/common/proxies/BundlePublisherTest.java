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

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import de.gematik.demis.exceptions.TokenException;
import de.gematik.demis.notificationgateway.common.enums.SupportedRealm;
import de.gematik.demis.notificationgateway.common.properties.ApplicationProperties;
import de.gematik.demis.notificationgateway.common.properties.LoggingProperties;
import de.gematik.demis.notificationgateway.common.properties.TLSProperties;
import de.gematik.demis.notificationgateway.common.properties.TestUserProperties;
import de.gematik.demis.notificationgateway.common.services.fhir.FhirObjectCreationService;
import org.apache.commons.lang3.RandomStringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BundlePublisherTest {

  @Mock
  private FhirObjectCreationService fhirObjectCreationService =
      mock(FhirObjectCreationService.class);

  @Mock private TokenProxy tokenProxy = mock(TokenProxy.class);
  @Mock private LoggingProperties loggingProperties = mock(LoggingProperties.class);
  @Mock private ApplicationProperties applicationProperties = mock(ApplicationProperties.class);
  @Mock private TLSProperties tlsProperties = mock(TLSProperties.class);
  @Mock private TestUserProperties testUserProperties = mock(TestUserProperties.class);

  @InjectMocks private BundlePublisher bundlePublisher;

  @BeforeEach
  void init() {
    when(testUserProperties.isTestIp(anyString())).thenReturn(true);
    when(fhirObjectCreationService.createParameters(any())).thenReturn(new Parameters());
  }

  @Test
  void testFailedToFetchToken() {
    final TokenException tokenException = new TokenException("failed to fetch token");
    doThrow(tokenException).when(tokenProxy).fetchToken(any(), anyBoolean());

    assertThatThrownBy(
            () ->
                bundlePublisher.postRequest(
                    new Bundle(),
                    SupportedRealm.LAB,
                    RandomStringUtils.randomAlphabetic(5),
                    RandomStringUtils.randomAlphabetic(5),
                    RandomStringUtils.randomAlphabetic(0),
                    "rki.demis.r4.core",
                    "1.23.0"))
        .isInstanceOf(TokenException.class)
        .hasMessage(tokenException.getMessage());
  }

  @Test
  void testMissingKeyStoreParameter() {
    when(tokenProxy.fetchToken(any(), anyBoolean())).thenReturn(RandomStringUtils.random(5));
    assertThatThrownBy(
            () ->
                bundlePublisher.postRequest(
                    new Bundle(),
                    SupportedRealm.LAB,
                    RandomStringUtils.randomAlphabetic(5),
                    RandomStringUtils.randomAlphabetic(5),
                    RandomStringUtils.randomAlphabetic(0),
                    "rki.demis.r4.core",
                    "1.23.0"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("requireNonNull keyStoreConfigParameter");
  }

  @Test
  void testMissingCloseableHttpClient() {
    when(tokenProxy.fetchToken(any(), anyBoolean())).thenReturn(RandomStringUtils.random(5));
    when(tlsProperties.getAuthCertPath()).thenReturn("certs/DEMIS.p12");
    when(tlsProperties.getTruststorePath()).thenReturn("certs/nginx.truststore");
    when(tlsProperties.getTruststorePassword()).thenReturn("wrong_password");
    when(testUserProperties.getAuthCertPath()).thenReturn("certs/Testuser.p12");
    when(testUserProperties.getAuthCertAlias()).thenReturn("demis-test-int");
    when(testUserProperties.getAuthCertPassword()).thenReturn("UY1Rrdk8s%tEugOT*");
    when(applicationProperties.isLegacyModeEnabled()).thenReturn(true);

    assertThatThrownBy(
            () ->
                bundlePublisher.postRequest(
                    new Bundle(),
                    SupportedRealm.LAB,
                    RandomStringUtils.randomAlphabetic(5),
                    RandomStringUtils.randomAlphabetic(5),
                    RandomStringUtils.randomAlphabetic(0),
                    "rki.demis.r4.core",
                    "1.23.0"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("requireNonNull closeableHttpClient");
  }

  @Test
  void testWrongServerURL() {
    when(tokenProxy.fetchToken(any(), anyBoolean())).thenReturn(RandomStringUtils.random(15));
    when(tlsProperties.getAuthCertPath()).thenReturn("certs/DEMIS.p12");
    when(tlsProperties.getTruststorePath()).thenReturn("certs/nginx.truststore");
    when(tlsProperties.getTruststorePassword()).thenReturn("secret");
    when(tlsProperties.getAuthCertAlias()).thenReturn("demis-99999");
    when(tlsProperties.getAuthCertPassword()).thenReturn("-8qp08U4Zx_j$gLj-");
    when(applicationProperties.isLegacyModeEnabled()).thenReturn(true);
    when(loggingProperties.isUseLoggingInterceptor()).thenReturn(true);
    when(testUserProperties.isTestIp(anyString())).thenReturn(false);

    assertThatThrownBy(
            () ->
                bundlePublisher.postRequest(
                    new Bundle(),
                    SupportedRealm.LAB,
                    RandomStringUtils.randomAlphabetic(5),
                    RandomStringUtils.randomAlphabetic(5),
                    RandomStringUtils.randomAlphabetic(0),
                    "rki.demis.r4.core",
                    "1.23.0"))
        .isInstanceOf(FhirClientConnectionException.class)
        .hasMessageContaining("Failed to parse response from server when performing POST");
  }
}
