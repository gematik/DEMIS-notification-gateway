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

import de.gematik.demis.exceptions.TokenException;
import de.gematik.demis.notificationgateway.common.enums.SupportedRealm;
import de.gematik.demis.notificationgateway.common.properties.ApplicationProperties;
import de.gematik.demis.notificationgateway.common.properties.TLSProperties;
import de.gematik.demis.notificationgateway.common.properties.TestUserProperties;
import de.gematik.demis.notificationgateway.common.properties.TokenProperties;
import de.gematik.demis.notificationgateway.common.utils.FileUtils;
import de.gematik.demis.notificationgateway.security.fidp.FederatedIdentityTokenChecker;
import de.gematik.demis.notificationgateway.security.oauth2.authentication.AuthenticationFacade;
import de.gematik.demis.tls.Keystore;
import de.gematik.demis.token.TokenClient;
import de.gematik.demis.token.data.RequestParameter;
import java.io.InputStream;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenProxy {
  private final TokenProperties tokenProperties;
  private final TLSProperties tlsProperties;
  private final TestUserProperties testUserProperties;
  private final ApplicationProperties applicationProperties;
  private final TokenClient tokenClient;
  private final AuthenticationFacade authenticationFacade;
  private final FederatedIdentityTokenChecker federatedIdentityTokenChecker;

  /**
   * @param realm the target realm
   * @return access token
   */
  public String fetchToken(SupportedRealm realm, boolean isTestUser) {

    final String authenticationToken = authenticationFacade.getAuthenticationToken();
    if (federatedIdentityTokenChecker.isFederatedIdpToken(authenticationToken)) {
      log.info("using token from federated identity provider");
      return authenticationToken;
    }

    String authCertPath = tlsProperties.getAuthCertPath();
    String keystorePassword = tlsProperties.getAuthCertPassword();
    String authCertAlias = tlsProperties.getAuthCertAlias();
    String username = tokenProperties.getUsername();
    if (isTestUser) {
      log.info("using test-user configuration");
      authCertPath = testUserProperties.getAuthCertPath();
      keystorePassword = testUserProperties.getAuthCertPassword();
      authCertAlias = testUserProperties.getAuthCertAlias();
      username = testUserProperties.getUsername();
    }

    String tokenEndpointUrl = tokenProperties.getTokenEndpointLabUrl();
    String clientSecret = tokenProperties.getClientSecretLAB();
    if (realm.equals(SupportedRealm.HOSPITAL)) {
      tokenEndpointUrl = tokenProperties.getTokenEndpointHospitalUrl();
      clientSecret = tokenProperties.getClientSecretHOSPITAL();
    }

    try (final InputStream authCertInputStream =
        FileUtils.getFileInput(authCertPath, applicationProperties.isLegacyModeEnabled())) {

      Objects.requireNonNull(authCertInputStream, "requireNonNull authCertInputStream");
      final byte[] bytes = authCertInputStream.readAllBytes();
      final Keystore userKeystore = Keystore.createPKCS12(bytes, keystorePassword, authCertAlias);
      final RequestParameter requestParameter =
          RequestParameter.create(
              tokenEndpointUrl,
              username,
              tokenProperties.getClientId(),
              clientSecret,
              userKeystore);

      return tokenClient.fetch(requestParameter).getAccessToken();
    } catch (Exception e) {
      throw new TokenException(e.getMessage());
    }
  }
}
