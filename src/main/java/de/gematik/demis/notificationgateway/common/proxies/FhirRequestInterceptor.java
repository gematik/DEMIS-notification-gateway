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

import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.interceptor.SimpleRequestHeaderInterceptor;
import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class FhirRequestInterceptor extends SimpleRequestHeaderInterceptor {

  private final String userAgentId;

  private final HttpServletRequest httpServletRequest;
  private final Set<String> headersToForward =
      Set.of(
          "x-fhir-api-request-origin",
          "x-fhir-api-submission-type",
          "x-fhir-api-version",
          "x-fhir-profile");

  @Override
  public void interceptRequest(IHttpRequest theRequest) {
    setUserAgent(theRequest);
    final Map<String, String> rawHeaders = getHeadersToForward();
    rawHeaders.forEach(theRequest::addHeader);
  }

  private void setUserAgent(IHttpRequest theRequest) {
    theRequest.removeHeaders("User-Agent");
    theRequest.addHeader("User-Agent", this.userAgentId);
  }

  @Nonnull
  private Map<String, String> getHeadersToForward() {
    Map<String, String> result = new java.util.HashMap<>();
    for (final String header : headersToForward) {
      Optional<Map.Entry<String, String>> entry = fromRequest(header);
      entry.ifPresent(e -> result.put(e.getKey(), e.getValue()));
    }
    return result;
  }

  /** Try and retrieve the first non-null header with the given name from the request. */
  @Nonnull
  private Optional<Map.Entry<String, String>> fromRequest(@Nonnull final String headerName) {
    String value = httpServletRequest.getHeader(headerName);
    if (value != null) {
      return Optional.of(Maps.immutableEntry(headerName, value));
    } else {
      return Optional.empty();
    }
  }
}
