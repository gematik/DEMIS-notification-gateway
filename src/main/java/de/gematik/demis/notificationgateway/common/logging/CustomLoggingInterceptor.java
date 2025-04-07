package de.gematik.demis.notificationgateway.common.logging;

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

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@Interceptor
public class CustomLoggingInterceptor implements IClientInterceptor {
  private static final Set<String> ALLOWED_REQUEST_HEADERS =
      Set.of("user-agent", "fhir-profile", "fhir-profile-version");

  private boolean logRequestHeaders = false;
  private boolean logRequestSummary = false;
  private boolean logResponseHeaders = false;

  @Hook(value = Pointcut.CLIENT_REQUEST, order = 1001)
  public void interceptRequest(IHttpRequest httpRequest) {
    if (this.logRequestSummary) {
      log.info("Client request: {}", httpRequest);
    }

    if (this.logRequestHeaders) {
      final Map<String, List<String>> allHeaders = httpRequest.getAllHeaders();
      log.info("Client request headers: {}\n{}", allHeaders.size(), headersToString(allHeaders));
    }
  }

  @Hook(value = Pointcut.CLIENT_RESPONSE, order = -2)
  public void interceptResponse(IHttpResponse httpResponse) {
    if (this.logResponseHeaders) {
      final Map<String, List<String>> allHeaders = httpResponse.getAllHeaders();
      log.info("Client response headers: {}\n{}", allHeaders.size(), headersToString(allHeaders));
    }
  }

  private String headersToString(Map<String, List<String>> headers) {
    return headers.entrySet().stream()
        .filter(CustomLoggingInterceptor::isAllowedRequestHeader)
        .map(e -> String.format("%s:%s", e.getKey(), e.getValue()))
        .collect(Collectors.joining("\n"));
  }

  private static boolean isAllowedRequestHeader(Map.Entry<String, ?> entry) {
    return ALLOWED_REQUEST_HEADERS.contains(entry.getKey().toLowerCase(Locale.ROOT));
  }
}
