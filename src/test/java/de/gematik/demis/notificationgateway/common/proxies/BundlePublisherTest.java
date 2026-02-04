package de.gematik.demis.notificationgateway.common.proxies;

/*-
 * #%L
 * DEMIS Notification-Gateway
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
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
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik,
 * find details in the "Readme" file.
 * #L%
 */

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import de.gematik.demis.notificationgateway.common.properties.ApplicationProperties;
import de.gematik.demis.notificationgateway.common.properties.LoggingProperties;
import de.gematik.demis.notificationgateway.common.services.fhir.FhirObjectCreationService;
import de.gematik.demis.notificationgateway.common.utils.Token;
import jakarta.servlet.http.HttpServletRequest;
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
  private static final RandomStringUtils random = RandomStringUtils.secure();

  @Mock
  private FhirObjectCreationService fhirObjectCreationService =
      mock(FhirObjectCreationService.class);

  @Mock private LoggingProperties loggingProperties;
  @Mock private ApplicationProperties applicationProperties;
  @Mock private Token token;
  @Mock private HttpServletRequest httpServletRequest;
  @InjectMocks private BundlePublisher bundlePublisher;

  @BeforeEach
  void init() {
    when(fhirObjectCreationService.createParameters(any())).thenReturn(new Parameters());
  }

  @Test
  void testWrongServerURL() {
    when(token.asText()).thenReturn(random.nextAlphanumeric(15));
    when(loggingProperties.isUseLoggingInterceptor()).thenReturn(true);
    assertThatThrownBy(
            () ->
                bundlePublisher.postRequest(
                    new Bundle(),
                    random.nextAlphabetic(5),
                    random.nextAlphabetic(5),
                    token,
                    httpServletRequest))
        .isInstanceOf(FhirClientConnectionException.class)
        .hasMessageContaining("Failed to parse response from server when performing POST");
  }
}
