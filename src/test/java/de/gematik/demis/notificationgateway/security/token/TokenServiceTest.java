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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import de.gematik.demis.exceptions.TokenException;
import de.gematik.demis.notificationgateway.common.enums.SupportedRealm;
import de.gematik.demis.notificationgateway.common.request.Metadata;
import jakarta.security.auth.message.AuthException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

  @Mock private AppTokenProxy appTokenProxy;
  @Mock private Token token;
  @Mock private Metadata metadata;
  @InjectMocks private TokenService tokenService;

  @Test
  void givenInboundTokenWhenOutboundTokenThenInboundToken() throws AuthException {
    final String userToken = "user-token";
    when(this.token.asText()).thenReturn(userToken);
    when(this.metadata.token()).thenReturn(Optional.of(this.token));

    Token outboundToken = this.tokenService.outboundToken(SupportedRealm.HOSPITAL, this.metadata);
    assertThat(outboundToken.asText()).isEqualTo(userToken);
  }

  @Test
  void givenNoInboundTokenWhenOutboundTokenThenAppToken() throws AuthException {
    final String appToken = "app-token";
    when(this.metadata.token()).thenReturn(Optional.empty());
    when(this.appTokenProxy.fetchToken(SupportedRealm.LAB, false)).thenReturn(appToken);

    final Token outboundToken = this.tokenService.outboundToken(SupportedRealm.LAB, this.metadata);
    assertThat(outboundToken.asText()).isEqualTo(appToken);
  }

  @Test
  void givenNoTokenWhenOutboundTokenThenThrowIllegalStateException() throws AuthException {
    when(this.metadata.token()).thenReturn(Optional.empty());
    when(this.appTokenProxy.fetchToken(SupportedRealm.LAB, false))
        .thenThrow(new TokenException("Timeout to server"));

    assertThatThrownBy(() -> this.tokenService.outboundToken(SupportedRealm.LAB, this.metadata))
        .isInstanceOf(TokenException.class)
        .hasMessage("Timeout to server");
  }

  @Test
  void givenTokenWhenCreateTokenThenToken() throws AuthException {
    final String userToken = "user-token";
    final String bearerUserToken = "Bearer " + userToken;
    final HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.AUTHORIZATION, bearerUserToken);
    Optional<Token> actualToken = this.tokenService.inboundToken(headers);
    assertThat(actualToken).isPresent();
    assertThat(actualToken.get().asText()).isEqualTo(userToken);
  }

  @Test
  void givenTokenWithoutPrefixWhenCreateTokenThenThrowAuthException() {
    final String brokenToken = "user-token";
    final HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.AUTHORIZATION, brokenToken);
    assertThatThrownBy(() -> this.tokenService.inboundToken(headers))
        .isInstanceOf(AuthException.class)
        .hasMessage("Unsupported token type");
  }

  @Test
  void givenNoTokenWhenCreateTokenThenEmpty() throws AuthException {
    final HttpHeaders headers = HttpHeaders.EMPTY;
    assertThat(this.tokenService.inboundToken(headers)).isEmpty();
  }
}
