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
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */

import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.interceptor.SimpleRequestHeaderInterceptor;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class FhirRequestInterceptor extends SimpleRequestHeaderInterceptor {

  private final String fhirProfile;
  private final String fhirProfileVersion;
  private final String userAgentId;

  @Override
  public void interceptRequest(IHttpRequest theRequest) {
    setUserAgent(theRequest);
    setFhirProfile(theRequest);
  }

  private void setUserAgent(IHttpRequest theRequest) {
    theRequest.removeHeaders("User-Agent");
    theRequest.addHeader("User-Agent", this.userAgentId);
  }

  private void setFhirProfile(IHttpRequest theRequest) {
    // add request header for validation service selection through k8s
    theRequest.addHeader("fhir-profile", this.fhirProfile);
    theRequest.addHeader("fhir-profile-version", this.fhirProfileVersion);
  }
}
