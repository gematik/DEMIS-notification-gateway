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

import de.gematik.demis.notificationgateway.security.fidp.FederatedIdentityProviderAuthenticationManager;
import de.gematik.demis.notificationgateway.security.oauth2.OAuthIdpConfiguration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;

@RequiredArgsConstructor
@Configuration
@Getter
public class AuthenticationManagerResolver {

  private final OAuthIdpConfiguration idpConfiguration;

  @Qualifier("ibmJwtDecoder")
  private final JwtDecoder ibmJwtDecoder;

  @Qualifier("demisJwtDecoder")
  private final JwtDecoder demisJwtDecoder;

  public Map<String, AuthenticationManager> getAuthenticationManager() {
    final Map<String, AuthenticationManager> authenticationManagers = new HashMap<>();
    addFederatedIDPAuthenticationManager(authenticationManagers);
    addIbmAllowedTokenIssuers(authenticationManagers);
    addDemisAllowedTokenIssuers(authenticationManagers);
    return authenticationManagers;
  }

  private void addFederatedIDPAuthenticationManager(
      Map<String, AuthenticationManager> authenticationManagers) {
    final var demisConfiguration = idpConfiguration.getDemisIssuerConfiguration();
    if (Objects.nonNull(demisConfiguration)) {
      String federatedIssuer = demisConfiguration.getFederatedIssuer();
      if (federatedIssuer != null) {
        AuthenticationManager federatedIDPAuthenticationManager =
            new FederatedIdentityProviderAuthenticationManager();
        authenticationManagers.put(federatedIssuer, federatedIDPAuthenticationManager);
      }
    }
  }

  private void addDemisAllowedTokenIssuers(
      final Map<String, AuthenticationManager> authenticationManagers) {
    final var demisConfiguration = idpConfiguration.getDemisIssuerConfiguration();
    if (Objects.nonNull(demisConfiguration)) {
      /**
       * TODO: As security measure, trusted issuers should be defined beforehand and from configured
       * issuers the decoders should be safely constructed.
       */
      final JwtAuthenticationProvider demisAuthenticationProvider =
          new JwtAuthenticationProvider(demisJwtDecoder);
      for (final String issuer : demisConfiguration.getAllowedIssuers()) {
        authenticationManagers.put(issuer, demisAuthenticationProvider::authenticate);
      }
    }
  }

  private void addIbmAllowedTokenIssuers(
      final Map<String, AuthenticationManager> authenticationManagers) {
    final var ibmConfiguration = idpConfiguration.getIbmIssuerConfiguration();
    if (Objects.nonNull(ibmConfiguration)) {
      /**
       * TODO: As security measure, trusted issuers should be defined beforehand and from configured
       * issuers the decoders should be safely constructed.
       */
      final JwtAuthenticationProvider ibmAuthenticationProvider =
          new JwtAuthenticationProvider(ibmJwtDecoder);
      for (final String issuer : ibmConfiguration.getAllowedIssuers()) {
        authenticationManagers.put(issuer, ibmAuthenticationProvider::authenticate);
      }
    }
  }
}
