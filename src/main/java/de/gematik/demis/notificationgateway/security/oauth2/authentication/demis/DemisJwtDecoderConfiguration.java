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

import de.gematik.demis.notificationgateway.security.TlsVerifierConfigurator;
import de.gematik.demis.notificationgateway.security.oauth2.OAuthIdpConfiguration;
import de.gematik.demis.notificationgateway.security.oauth2.UnifiedOAuth2TokenValidator;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClients;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

@Slf4j
@AllArgsConstructor
@Configuration
@ConditionalOnProperty(
    matchIfMissing = true,
    name = "jwt.offline.decoder.enabled",
    havingValue = "false")
public class DemisJwtDecoderConfiguration {

  @NonNull private final OAuthIdpConfiguration idpConfiguration;

  @NonNull private final UnifiedOAuth2TokenValidator tokenValidator;

  /**
   * Creates a {@link NimbusJwtDecoder} instance for validating a DEMIS Token from Keycloak.
   * Depending on configuration, the TLS Certificate Validation can be disabled.
   *
   * @return a new {@link NimbusJwtDecoder} object
   */
  @Bean(name = "demisJwtDecoder")
  public NimbusJwtDecoder jwtDecoder() {
    log.info("Creating default DEMIS jwtDecoder");
    final var delegatingOAuth2TokenValidator =
        new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), tokenValidator);

    final var demisConfiguration = idpConfiguration.getDemisIssuerConfiguration();

    if (Objects.isNull(demisConfiguration)) {
      throw new IllegalStateException("Demis IDP Configuration is missing or invalid");
    }

    final var jwtDecoderBuilder =
        NimbusJwtDecoder.withJwkSetUri(demisConfiguration.getJwkSetUri())
            .jwsAlgorithm(SignatureAlgorithm.from(demisConfiguration.getJwsAlgorithm()));

    if (!demisConfiguration.isTlsVerify()) {
      try {
        jwtDecoderBuilder.restOperations(restTemplateWithoutCertificateValidation());
      } catch (final NoSuchAlgorithmException | KeyManagementException e) {
        log.warn("Could not disable the TLS Certificate Validation: {}", e.getLocalizedMessage());
      }
    }

    final NimbusJwtDecoder jwtDecoder = jwtDecoderBuilder.build();

    jwtDecoder.setJwtValidator(delegatingOAuth2TokenValidator);

    return jwtDecoder;
  }

  private RestOperations restTemplateWithoutCertificateValidation()
      throws NoSuchAlgorithmException, KeyManagementException {

    final var noopVerifier = new NoopHostnameVerifier();
    final var httpClient =
        HttpClients.custom()
            .setSSLHostnameVerifier(noopVerifier)
            .setSSLSocketFactory(TlsVerifierConfigurator.createUnsafeLayeredSecureSocketFactory())
            .build();

    final HttpComponentsClientHttpRequestFactory requestFactory =
        new HttpComponentsClientHttpRequestFactory();

    if (Objects.nonNull(httpClient)) {
      requestFactory.setHttpClient(httpClient);
    }

    requestFactory.setConnectTimeout(60);
    requestFactory.setConnectionRequestTimeout(60);
    return new RestTemplate(requestFactory);
  }
}
