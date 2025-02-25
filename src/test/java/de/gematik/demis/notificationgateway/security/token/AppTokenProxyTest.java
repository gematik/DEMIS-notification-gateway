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
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import de.gematik.demis.exceptions.TokenException;
import de.gematik.demis.notificationgateway.common.enums.SupportedRealm;
import de.gematik.demis.notificationgateway.common.properties.TLSProperties;
import de.gematik.demis.notificationgateway.common.properties.TestUserProperties;
import de.gematik.demis.token.TokenClient;
import de.gematik.demis.token.TokenResponse;
import jakarta.security.auth.message.AuthException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppTokenProxyTest {

  public static final String TOKEN = "token";
  @Mock private TokenProperties tokenProperties;
  @Mock private TLSProperties tlsProperties;
  @Mock private TestUserProperties testUserProperties;
  @Mock private TokenClient tokenClient;
  @Mock private TokenResponse tokenResponse;
  @InjectMocks private AppTokenProxy appTokenProxy;

  @Test
  void givenHospitalRealmWhenFetchTokenThenThrowAuthException() {
    Assertions.assertThatThrownBy(
            () -> this.appTokenProxy.fetchToken(SupportedRealm.HOSPITAL, false))
        .isInstanceOf(AuthException.class)
        .hasMessage("Unsupported realm for app-based authentication: HOSPITAL");
  }

  @Test
  void givenPathogenAuthenticationRequiredWhenFetchTokenThenThrowAuthException() {
    when(tokenProperties.isPathogenAuthenticationRequired()).thenReturn(true);
    Assertions.assertThatThrownBy(() -> this.appTokenProxy.fetchToken(SupportedRealm.LAB, false))
        .isInstanceOf(AuthException.class)
        .hasMessage("App-based authentication is disabled");
  }

  @Test
  void givenLabRealmAndPathogenAuthNotRequiredWhenFetchTokenThenReturnToken() throws AuthException {
    mockTokenClient();
    mockTokenProperties();
    when(this.tlsProperties.getAuthCertPath()).thenReturn("src/main/resources/certs/DEMIS.p12");
    when(this.tlsProperties.getAuthCertAlias()).thenReturn("demis-99999");
    when(this.tlsProperties.getAuthCertPassword()).thenReturn("-8qp08U4Zx_j$gLj-");

    final String appToken = this.appTokenProxy.fetchToken(SupportedRealm.LAB, false);
    Assertions.assertThat(appToken).isEqualTo(TOKEN);
  }

  @Test
  void givenTestUserWhenFetchTokenThenReturnToken() throws AuthException {
    mockTokenClient();
    mockTokenProperties();
    when(this.testUserProperties.getAuthCertPath())
        .thenReturn("src/main/resources/certs/Testuser.p12");
    when(this.testUserProperties.getAuthCertAlias()).thenReturn("demis-test-int");
    when(this.testUserProperties.getAuthCertPassword()).thenReturn("UY1Rrdk8s%tEugOT*");
    when(this.testUserProperties.getUsername()).thenReturn("test-int");

    final String appToken = this.appTokenProxy.fetchToken(SupportedRealm.LAB, true);
    assertThat(appToken).isEqualTo(TOKEN);
  }

  @Test
  void givenExceptionWhenTokenClientThenThrowTokenException() {
    final String message = "failed to fetch token";
    when(this.tokenClient.fetch(any())).thenThrow(new IllegalStateException(message));
    mockTokenProperties();
    when(this.tlsProperties.getAuthCertPath()).thenReturn("src/main/resources/certs/DEMIS.p12");
    when(this.tlsProperties.getAuthCertAlias()).thenReturn("demis-99999");
    when(this.tlsProperties.getAuthCertPassword()).thenReturn("-8qp08U4Zx_j$gLj-");

    assertThatThrownBy(() -> this.appTokenProxy.fetchToken(SupportedRealm.LAB, false))
        .isInstanceOf(TokenException.class)
        .hasMessage(message);
  }

  private void mockTokenProperties() {
    when(this.tokenProperties.isPathogenAuthenticationRequired()).thenReturn(false);
    when(this.tokenProperties.getTokenEndpointLabUrl()).thenReturn("https://token-endpoint");
    when(this.tokenProperties.getClientId()).thenReturn("client-id");
    when(this.tokenProperties.getUsername()).thenReturn("user");
    when(this.tokenProperties.getClientSecretLAB()).thenReturn("client-secret");
  }

  private void mockTokenClient() {
    when(this.tokenResponse.getAccessToken()).thenReturn(TOKEN);
    when(this.tokenClient.fetch(any())).thenReturn(this.tokenResponse);
  }
}
