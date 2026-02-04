package de.gematik.demis.notificationgateway.common.utils;

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

import jakarta.security.auth.message.AuthException;
import java.util.List;
import org.springframework.http.HttpHeaders;

public final class Token {

  private static final String BEARER_PREFIX = "Bearer ";
  private final String text;

  private Token(String text) {
    this.text = text;
  }

  /**
   * Extracts the Authorization header and creates a {@link Token}.
   *
   * @param headers the HTTP headers containing the Authorization header.
   * @return a {@link Token} object.
   * @throws AuthException if the Authorization header is missing or invalid.
   */
  public static Token of(HttpHeaders headers) throws AuthException {
    final List<String> tokens = headers.get("Authorization");
    if (tokens == null || tokens.size() != 1) {
      throw new AuthException("Exactly one Authorization header expected");
    }
    final String token = tokens.get(0);
    if (token == null || !token.startsWith(BEARER_PREFIX)) {
      throw new AuthException("Authorization header must contain a valid Bearer token");
    }
    return new Token(token.substring(BEARER_PREFIX.length()));
  }

  /**
   * Returns the encoded token without the "Bearer" prefix.
   *
   * @return the encoded token.
   */
  public String asText() {
    return text;
  }
}
