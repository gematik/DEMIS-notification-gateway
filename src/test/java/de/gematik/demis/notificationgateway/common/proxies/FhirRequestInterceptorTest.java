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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.client.api.IHttpRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FhirRequestInterceptorTest {

  @Mock private HttpServletRequest httpServletRequest;

  @Test
  void testInterceptRequest() {

    // Arrange
    final String apiVersion = "preview6";
    final String fhirPackage = "http://example.com/profile";
    final String fhirPackageVersion = "v6";

    final String identifier = "TestClient";
    final String submissionType = "pathogen";
    final String requestOrigin = "internal";

    when(httpServletRequest.getHeader("x-api-version")).thenReturn(apiVersion);
    when(httpServletRequest.getHeader("x-fhir-package")).thenReturn(fhirPackage);
    when(httpServletRequest.getHeader("x-fhir-package-version")).thenReturn(fhirPackageVersion);

    when(httpServletRequest.getHeader("x-fhir-profile")).thenReturn(fhirPackage);
    when(httpServletRequest.getHeader("x-fhir-api-version")).thenReturn(fhirPackageVersion);
    when(httpServletRequest.getHeader("x-fhir-api-submission-type")).thenReturn(submissionType);
    when(httpServletRequest.getHeader("x-fhir-api-request-origin")).thenReturn(requestOrigin);

    IHttpRequest mockRequest = mock(IHttpRequest.class);
    FhirRequestInterceptor interceptor = new FhirRequestInterceptor(identifier, httpServletRequest);

    // Act
    interceptor.interceptRequest(mockRequest);

    // Assert
    verify(mockRequest).removeHeaders("User-Agent");
    verify(mockRequest).addHeader("User-Agent", identifier);
    verify(mockRequest).addHeader("x-api-version", apiVersion);
    verify(mockRequest).addHeader("x-fhir-package", fhirPackage);
    verify(mockRequest).addHeader("x-fhir-package-version", fhirPackageVersion);

    verify(mockRequest).addHeader("x-fhir-profile", fhirPackage);
    verify(mockRequest).addHeader("x-fhir-api-version", fhirPackageVersion);
    verify(mockRequest).addHeader("x-fhir-api-submission-type", submissionType);
    verify(mockRequest).addHeader("x-fhir-api-request-origin", requestOrigin);
    verifyNoMoreInteractions(mockRequest);
  }
}
