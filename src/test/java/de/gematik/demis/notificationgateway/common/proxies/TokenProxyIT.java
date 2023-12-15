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

package de.gematik.demis.notificationgateway.common.proxies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import de.gematik.demis.exceptions.TokenException;
import de.gematik.demis.notificationgateway.common.enums.SupportedRealm;
import de.gematik.demis.notificationgateway.security.oauth2.authentication.AuthenticationFacade;
import de.gematik.demis.token.TokenClient;
import de.gematik.demis.token.TokenResponse;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class TokenProxyIT {

  @MockBean private TokenClient tokenClient;
  @MockBean private AuthenticationFacade authenticationFacade;

  @Autowired private TokenProxy tokenProxy;
  private TokenResponse tokenResponse;

  @BeforeEach
  public void init() {
    tokenResponse = new TokenResponse();
    tokenResponse.setAccessToken(RandomStringUtils.randomAlphabetic(20));
  }

  @Test
  void getLabToken() {
    when(authenticationFacade.getAuthenticationToken()).thenReturn(null);
    when(tokenClient.fetch(any())).thenReturn(tokenResponse);

    final String token = tokenProxy.fetchToken(SupportedRealm.LAB, true);

    assertThat(token).isNotBlank().isEqualTo(tokenResponse.getAccessToken());
  }

  @Test
  void getHospitalToken() {
    when(authenticationFacade.getAuthenticationToken()).thenReturn(null);
    when(tokenClient.fetch(any())).thenReturn(tokenResponse);

    final String token = tokenProxy.fetchToken(SupportedRealm.HOSPITAL, true);

    assertThat(token).isNotBlank().isEqualTo(tokenResponse.getAccessToken());
  }

  @Test
  void getTokenForUnsupportedRealm() {
    when(authenticationFacade.getAuthenticationToken()).thenReturn(null);
    doThrow(new IllegalArgumentException("failed to fetch token")).when(tokenClient).fetch(any());
    assertThatThrownBy(() -> tokenProxy.fetchToken(SupportedRealm.HOSPITAL, false))
        .isInstanceOf(TokenException.class)
        .hasMessage("failed to fetch token");
  }

  @Test
  void getTokenForFederatedIDP() {
    String token =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9"
            + ".eyJpc3MiOiJodHRwczovL3FzLmRlbWlzLnJ"
            + "raS5kZS9hdXRoL3JlYWxtcy9QT1JUQUwiLCJ"
            + "pYXQiOjE3MDExNzk4MzAsImV4cCI6MTcwMTE"
            + "3OTgzMCwiYXVkIjoieHh4LXh4eC14eHgiLCJ"
            + "zdWIiOiJ0ZXN0LXh4eCJ9.DhlOBszfQ_KiIT"
            + "aGc2SPYtZahz1JPKyqPMYHS9CneVo";

    when(authenticationFacade.getAuthenticationToken()).thenReturn(token);

    final String expectedToken = tokenProxy.fetchToken(SupportedRealm.HOSPITAL, false);

    assertThat(expectedToken).isEqualTo(token);
  }
}
