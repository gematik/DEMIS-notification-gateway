/*
 * Copyright [2023], gematik GmbH
 *
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
 */

package de.gematik.demis.notificationgateway.security;

import static org.mockito.Mockito.mock;

import de.gematik.demis.notificationgateway.security.fidp.FederatedIdentityProviderAuthenticationManager;
import de.gematik.demis.notificationgateway.security.oauth2.OAuthIdpConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource("classpath:application.properties")
class AuthenticationManagerResolverTest {

  @Autowired private OAuthIdpConfiguration oAuthIdpConfiguration;
  private final JwtDecoder ibmJwtDecoderMock = mock(JwtDecoder.class);
  private final JwtDecoder demisJwtDecoderMock = mock(JwtDecoder.class);
  AuthenticationManagerResolver authenticationManagerResolver;

  @BeforeEach
  void init() {
    authenticationManagerResolver =
        new AuthenticationManagerResolver(
            oAuthIdpConfiguration, ibmJwtDecoderMock, demisJwtDecoderMock);
  }

  @Test
  void expectAuthenticationManagersNotNull() {
    Map<String, AuthenticationManager> authenticationManagers =
        authenticationManagerResolver.getAuthenticationManager();
    Assertions.assertNotNull(authenticationManagers);
  }

  @Test
  void expectAuthenticationManagersContainDemisIssuers() {
    Map<String, AuthenticationManager> authenticationManagers =
        authenticationManagerResolver.getAuthenticationManager();
    List<String> allowedDemisIssuers =
        Objects.requireNonNull(oAuthIdpConfiguration.getDemisIssuerConfiguration())
            .getAllowedIssuers();
    Assertions.assertTrue(authenticationManagers.keySet().containsAll(allowedDemisIssuers));
  }

  @Test
  void expectAuthenticationManagersContainIbmIssuers() {
    Map<String, AuthenticationManager> authenticationManagers =
        authenticationManagerResolver.getAuthenticationManager();
    List<String> allowedIbmIssuers =
        Objects.requireNonNull(oAuthIdpConfiguration.getIbmIssuerConfiguration())
            .getAllowedIssuers();
    Assertions.assertTrue(authenticationManagers.keySet().containsAll(allowedIbmIssuers));
  }

  @Test
  void expectOneFederatedIdentityProviderAuthenticationManager() {
    int expectedCount = 1;
    Map<String, AuthenticationManager> authenticationManagers =
        authenticationManagerResolver.getAuthenticationManager();
    long count =
        authenticationManagers.values().stream()
            .filter(manager -> manager instanceof FederatedIdentityProviderAuthenticationManager)
            .count();
    Assertions.assertEquals(expectedCount, count);
  }
}
