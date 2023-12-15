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

import java.util.List;
import javax.validation.Validation;
import javax.validation.Validator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(value = OAuthIdpConfiguration.class)
@TestPropertySource("classpath:application.properties")
class OAuthDemisIdpConfigurationTest {

  @Autowired OAuthIdpConfiguration oAuthIdpConfiguration;

  private static Validator validator;

  @BeforeAll
  protected static void initValidator() {
    try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
      validator = validatorFactory.getValidator();
    }
  }

  @Test
  void expectThatDemisIdpIsConfigured() {
    Assertions.assertNotNull(oAuthIdpConfiguration.getIssuers());
    Assertions.assertTrue(oAuthIdpConfiguration.getIssuers().contains("demis"));
  }

  @Test
  void expectThatIbmIdpIsConfigured() {
    Assertions.assertNotNull(oAuthIdpConfiguration.getIssuers());
    Assertions.assertTrue(oAuthIdpConfiguration.getIssuers().contains("ibm"));
  }

  @Test
  void expectThatDemisPropertiesAreLoaded() {
    Assertions.assertNotNull(oAuthIdpConfiguration.getDemisIssuerConfiguration());
  }

  @Test
  void givenValidOAuth2IBMPropertiesWhenBindingPropertiesFileThenAllFieldsAreSet() {
    org.assertj.core.api.Assertions.assertThat(oAuthIdpConfiguration.getIbmIssuerConfiguration())
        .isNotNull()
        .hasFieldOrPropertyWithValue(
            "jwkSetUri",
            "https://id.certify.demo.ubirch.com/auth/realms/gematik/protocol/openid-connect/certs")
        .hasFieldOrPropertyWithValue(
            "allowedIssuers",
            List.of(
                "https://id.certify.demo.ubirch.com/auth/realms/gematik",
                "https://id.ru.impfnachweis.info/auth/realms/gematik"));
  }
}
