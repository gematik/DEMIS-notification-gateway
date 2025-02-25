package de.gematik.demis.notificationgateway.common.request;

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

import de.gematik.demis.notificationgateway.common.exceptions.BadRequestException;
import de.gematik.demis.notificationgateway.common.properties.TestUserProperties;
import de.gematik.demis.notificationgateway.security.token.Token;
import de.gematik.demis.notificationgateway.security.token.TokenService;
import jakarta.security.auth.message.AuthException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class RequestService {

  private final TestUserProperties testUserProperties;
  private final TokenService tokenService;

  /**
   * Create inbound request metadata
   *
   * @param headers HTTP headers
   * @return inbound request metadata
   * @throws BadRequestException failed to get remote address from headers
   * @throws AuthException failed to process existing token
   */
  public Metadata createMetadata(HttpHeaders headers) throws BadRequestException, AuthException {
    final boolean testUser = testUser(headers);
    final Optional<Token> token = this.tokenService.inboundToken(headers);
    if (token.isPresent()) {
      log.debug("Creating authenticated metadata. TestUser: {}", testUser);
      return new Metadata(token.get(), testUser);
    }
    log.debug("Creating unauthenticated metadata. TestUser: {}", testUser);
    return new Metadata(testUser);
  }

  private boolean testUser(HttpHeaders headers) throws BadRequestException {
    return this.testUserProperties.isTestIp(this.testUserProperties.clientIp(headers));
  }
}
