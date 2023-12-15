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

package de.gematik.demis.notificationgateway.security.oauth2;

import static de.gematik.demis.notificationgateway.BaseTestUtils.EXPIRED_IBM_TOKEN;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@EnableAutoConfiguration
class UnifiedOAuth2TokenValidatorIT {

  @Autowired
  @Qualifier("ibmJwtDecoder")
  private NimbusJwtDecoder jwtDecoder;

  @Autowired private UnifiedOAuth2TokenValidator oAuth2TokenValidator;

  @Test
  void givenExpiredBearerTokenWhenValidateIgnoringExpirationThenNoError() {
    jwtDecoder.setJwtValidator(oAuth2TokenValidator);
    final Jwt jwt = jwtDecoder.decode(EXPIRED_IBM_TOKEN);

    final OAuth2TokenValidatorResult result = oAuth2TokenValidator.validate(jwt);

    assertThat(result).isNotNull();
    assertThat(result.getErrors().toArray()).isEmpty();
  }
}
