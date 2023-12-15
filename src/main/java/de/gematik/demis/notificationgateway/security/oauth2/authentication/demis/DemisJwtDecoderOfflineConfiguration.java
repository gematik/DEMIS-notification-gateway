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

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import de.gematik.demis.notificationgateway.security.oauth2.UnifiedOAuth2TokenValidator;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Slf4j
@AllArgsConstructor
@Configuration
@ConditionalOnProperty(name = "jwt.offline.decoder.enabled", havingValue = "true")
public class DemisJwtDecoderOfflineConfiguration {
  private final UnifiedOAuth2TokenValidator tokenValidator;

  @Bean(name = "demisJwtDecoder")
  public NimbusJwtDecoder jwtDecoder() throws IOException, ParseException, URISyntaxException {
    log.warn("creating development DEMIS jwtDecoder");
    final URL jwkSetUrl = getClass().getClassLoader().getResource("idp/demis-jwk-set.json");
    final JWKSet jwkSet = JWKSet.load(new File(Objects.requireNonNull(jwkSetUrl).toURI()));
    ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
    jwtProcessor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(new JOSEObjectType("jwt")));
    JWSAlgorithm jwsAlgorithm =
        JWSAlgorithm.parse(jwkSet.getKeys().get(0).getAlgorithm().getName());
    JWSKeySelector<SecurityContext> keySelector =
        new JWSVerificationKeySelector<>(jwsAlgorithm, new ImmutableJWKSet<>(jwkSet));
    jwtProcessor.setJWSKeySelector(keySelector);
    jwtProcessor.setJWTClaimsSetVerifier(null);
    OAuth2TokenValidator<Jwt> delegatingOAuth2TokenValidator =
        new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), tokenValidator);
    final NimbusJwtDecoder jwtDecoder = new NimbusJwtDecoder(jwtProcessor);
    jwtDecoder.setJwtValidator(delegatingOAuth2TokenValidator);

    return jwtDecoder;
  }
}
