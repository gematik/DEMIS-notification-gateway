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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import ca.uhn.fhir.rest.client.api.IHttpRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FhirRequestInterceptorTest {

  @Test
  void testInterceptRequest() {
    // Arrange
    String fhirProfile = "http://example.com/profile";
    String fhirProfileVersion = "1.0.0";
    String identifier = "TestClient";

    IHttpRequest mockRequest = mock(IHttpRequest.class);
    FhirRequestInterceptor interceptor =
        new FhirRequestInterceptor(fhirProfile, fhirProfileVersion, identifier);

    // Act
    interceptor.interceptRequest(mockRequest);

    // Assert
    verify(mockRequest).removeHeaders("User-Agent");
    verify(mockRequest).addHeader("User-Agent", identifier);
    verify(mockRequest).addHeader("fhir-profile", fhirProfile);
    verify(mockRequest).addHeader("fhir-profile-version", fhirProfileVersion);
    verifyNoMoreInteractions(mockRequest);
  }
}
