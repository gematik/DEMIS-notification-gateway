package de.gematik.demis.notificationgateway.common.utils;

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

import static org.junit.jupiter.api.Assertions.*;

import jakarta.security.auth.message.AuthException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class TokenTest {

  private static final String TOKEN = "test-token-without-interpretation";

  @Test
  void givenBearerTokenWhenOfThenReturnToken() throws AuthException {
    final HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(TOKEN);
    final Token token = Token.of(headers);
    Assertions.assertThat(token.asText()).isEqualTo(TOKEN);
  }

  @Test
  void givenNonBearerTokenWhenOfThenThrowAuthException() {
    final HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Basic " + TOKEN);
    assertThrows(AuthException.class, () -> Token.of(headers));
  }

  @Test
  void givenNoTokenWhenOfThenThrowAuthException() {
    final HttpHeaders headers = new HttpHeaders();
    assertThrows(AuthException.class, () -> Token.of(headers));
  }
}
