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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import de.gematik.demis.notificationgateway.common.exceptions.BadRequestException;
import de.gematik.demis.notificationgateway.common.properties.TestUserProperties;
import de.gematik.demis.notificationgateway.security.token.Token;
import de.gematik.demis.notificationgateway.security.token.TokenService;
import jakarta.security.auth.message.AuthException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

  private static final String TEST_USER_IP = "192.168.0.1";

  @Mock private TestUserProperties testUserProperties;
  @Mock private TokenService tokenService;
  @Mock private Token token;
  @InjectMocks private RequestService requestService;

  @Test
  void givenTestUserIpWhenCreateMetadataThenTestUserMetadata()
      throws BadRequestException, AuthException {
    HttpHeaders headers = new HttpHeaders();
    headers.set("x-real-ip", TEST_USER_IP);
    when(this.testUserProperties.clientIp(headers)).thenReturn(TEST_USER_IP);
    when(this.testUserProperties.isTestIp(TEST_USER_IP)).thenReturn(true);

    final Metadata metadata = this.requestService.createMetadata(headers);
    assertThat(metadata.isTestUser()).isTrue();
  }

  @Test
  void givenTokenFromTokenServiceWhenCreateMetadataThenAuthenticatedMetadata()
      throws BadRequestException, AuthException {
    HttpHeaders headers = HttpHeaders.EMPTY;
    when(this.tokenService.inboundToken(Mockito.same(headers))).thenReturn(Optional.of(this.token));
    when(this.testUserProperties.clientIp(headers)).thenReturn(TEST_USER_IP);
    when(this.testUserProperties.isTestIp(TEST_USER_IP)).thenReturn(false);

    final Metadata metadata = this.requestService.createMetadata(headers);
    assertThat(metadata.isTestUser()).isFalse();
    assertThat(metadata.token()).isPresent();
  }
}
