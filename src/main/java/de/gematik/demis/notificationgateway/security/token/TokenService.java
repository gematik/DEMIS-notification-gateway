package de.gematik.demis.notificationgateway.security.token;

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

import de.gematik.demis.notificationgateway.common.enums.SupportedRealm;
import de.gematik.demis.notificationgateway.common.request.Metadata;
import jakarta.security.auth.message.AuthException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

/** Service for handling inbound and outbound authorization tokens */
@RequiredArgsConstructor
@Service
@Slf4j
public final class TokenService {

  static final String PREFIX = "Bearer ";

  private final AppTokenProxy appTokenProxy;

  /**
   * Create authorization token from HTTP headers
   *
   * @param headers inbound request HTTP headers
   * @return authorization token
   * @throws AuthException access is unauthorized as authentication failed
   */
  public Optional<Token> inboundToken(HttpHeaders headers) throws AuthException {
    final String authorization = headers.getFirst(HttpHeaders.AUTHORIZATION);
    if (StringUtils.isBlank(authorization)) {
      return Optional.empty();
    }
    if (!StringUtils.startsWith(authorization, PREFIX)) {
      throw new AuthException("Unsupported token type");
    }
    return Optional.of(createToken(authorization));
  }

  /**
   * Get user token or create gateway application token to be used as authorization token for
   * outbound requests
   *
   * @param realm supported realm
   * @param metadata inbound request metadata
   * @return authorization token
   * @throws AuthException access is unauthorized as authentication failed
   */
  public Token outboundToken(SupportedRealm realm, Metadata metadata) throws AuthException {
    final Optional<Token> userToken = metadata.token();
    if (userToken.isPresent()) {
      log.debug("Using user token for outbound request");
      return userToken.get();
    }
    log.debug("Using app token for outbound request");
    return createToken(this.appTokenProxy.fetchToken(realm, metadata.isTestUser()));
  }

  private Token createToken(String token) {
    return new LazyToken(StringUtils.removeStart(token, PREFIX));
  }
}
