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

package de.gematik.demis.notificationgateway.security.oauth2.authentication.ibm;

import de.gematik.demis.notificationgateway.security.oauth2.UnifiedOAuth2TokenValidator;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Slf4j
@AllArgsConstructor
@Configuration
@ConditionalOnProperty(
    matchIfMissing = true,
    name = "jwt.offline.decoder.enabled",
    havingValue = "false")
public class IbmJwtDecoderConfiguration {

  @NonNull private final UnifiedOAuth2TokenValidator tokenValidator;

  @Bean(name = "ibmJwtDecoder")
  public NimbusJwtDecoder jwtDecoder() {
    log.info("creating default IBM jwtDecoder");
    var delegatingOAuth2TokenValidator =
        new DelegatingOAuth2TokenValidator<Jwt>(new JwtTimestampValidator(), tokenValidator);

    final var ibmConfiguration = tokenValidator.getIdpConfiguration().getIbmIssuerConfiguration();

    if (Objects.isNull(ibmConfiguration)) {
      throw new IllegalStateException("IBM IDP Configuration is missing or invalid");
    }

    final NimbusJwtDecoder jwtDecoder =
        NimbusJwtDecoder.withJwkSetUri(ibmConfiguration.getJwkSetUri())
            .jwsAlgorithm(SignatureAlgorithm.from(ibmConfiguration.getJwsAlgorithm()))
            .build();
    jwtDecoder.setJwtValidator(delegatingOAuth2TokenValidator);

    return jwtDecoder;
  }
}
