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

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import java.text.ParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
final class LazyToken implements Token {

  private final String token;

  private JWT jwt;

  @Override
  public JWT jwt() {
    return getJwt();
  }

  @Override
  public String asText() {
    return this.token;
  }

  private JWT getJwt() {
    if (this.jwt == null) {
      try {
        this.jwt = JWTParser.parse(this.token);
      } catch (ParseException e) {
        throw new IllegalArgumentException("Invalid token", e);
      }
    }
    return this.jwt;
  }
}
