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

package de.gematik.demis.notificationgateway.security.token;

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

import de.gematik.demis.exceptions.TokenException;
import de.gematik.demis.notificationgateway.common.enums.SupportedRealm;
import de.gematik.demis.notificationgateway.common.properties.TLSProperties;
import de.gematik.demis.notificationgateway.common.properties.TestUserProperties;
import de.gematik.demis.notificationgateway.common.utils.FileUtils;
import de.gematik.demis.tls.Keystore;
import de.gematik.demis.token.TokenClient;
import de.gematik.demis.token.data.RequestParameter;
import jakarta.security.auth.message.AuthException;
import java.io.InputStream;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Proxy for fetching generic gateway application tokens from the token endpoint */
@Slf4j
@Component
@RequiredArgsConstructor
class AppTokenProxy {

  private final TokenProperties tokenProperties;
  private final TLSProperties tlsProperties;
  private final TestUserProperties testUserProperties;
  private final TokenClient tokenClient;

  /**
   * Retrieves a token from the token endpoint, if component is enabled and the user is not
   * authenticated.
   *
   * @param realm the realm for which the token should be fetched
   * @param isTestUser if the test user configuration should be used
   * @return the token as a string
   * @throws AuthException access is unauthorized as app-based authentication is disabled or realm
   *     is unsupported
   */
  String fetchToken(SupportedRealm realm, boolean isTestUser) throws AuthException {
    if (realm != SupportedRealm.LAB) {
      throw new AuthException("Unsupported realm for app-based authentication: " + realm);
    }
    if (tokenProperties.isPathogenAuthenticationRequired()) {
      throw new AuthException("App-based authentication is disabled");
    }
    return fetch(isTestUser);
  }

  private String fetch(boolean isTestUser) {
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
    return fetch(
        authCertPath, keystorePassword, authCertAlias, tokenEndpointUrl, username, clientSecret);
  }

  private String fetch(
      String authCertPath,
      String keystorePassword,
      String authCertAlias,
      String tokenEndpointUrl,
      String username,
      String clientSecret) {
    try (final InputStream authCertInputStream = FileUtils.loadFileFromPath(authCertPath)) {
      Objects.requireNonNull(
          authCertInputStream, "Invalid Authentication Certificate path specified");
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
