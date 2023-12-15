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

package de.gematik.demis.notificationgateway.security.oauth2.authentication.demis;

import static de.gematik.demis.notificationgateway.BaseTestUtils.EXPIRED_DEMIS_HOSPITAL_TOKEN;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import de.gematik.demis.notificationgateway.security.oauth2.UnifiedOAuth2TokenValidator;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class DemisJwtDecoderOfflineConfigurationIT {

  @Autowired private UnifiedOAuth2TokenValidator oAuth2TokenValidator;

  @Autowired
  @Qualifier("demisJwtDecoder")
  private NimbusJwtDecoder jwtDecoder;

  @Test
  void givenExpiredBearerTokenWhenDecodeWithoutExpireValidationThenNoError() {
    final Instant expiresAt = Instant.parse("2023-10-25T09:13:44Z");
    jwtDecoder.setJwtValidator(oAuth2TokenValidator);

    final Jwt jwt = jwtDecoder.decode(EXPIRED_DEMIS_HOSPITAL_TOKEN);

    assertThat(jwt).isNotNull().hasFieldOrPropertyWithValue("expiresAt", expiresAt);
  }

  @Test
  void givenExpiredBearerTokenWhenDecodeWithNoDurationSetThenError() {
    final OAuth2TokenValidator<Jwt> delegatingOAuth2TokenValidator =
        new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), oAuth2TokenValidator);
    jwtDecoder.setJwtValidator(delegatingOAuth2TokenValidator);
    assertThatThrownBy(() -> jwtDecoder.decode(EXPIRED_DEMIS_HOSPITAL_TOKEN))
        .isInstanceOf(JwtValidationException.class)
        .hasMessage(
            "An error occurred while attempting to decode the Jwt: Jwt expired at 2023-10-25T09:13:44Z");
  }

  @Test
  void givenExpiredBearerTokenWhenDecodeWithLongDurationThenNoError() {
    final OAuth2TokenValidator<Jwt> delegatingOAuth2TokenValidator =
        new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(Duration.ofDays(100000)), oAuth2TokenValidator);
    jwtDecoder.setJwtValidator(delegatingOAuth2TokenValidator);
    final Jwt jwt = jwtDecoder.decode(EXPIRED_DEMIS_HOSPITAL_TOKEN);

    assertThat(jwt).isNotNull();
  }
}
