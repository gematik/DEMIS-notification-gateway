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

package de.gematik.demis.notificationgateway.domain.portal;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "config.notification-portal")
@Getter
@Setter
public class PortalConfigProperties {
  /** enable production mode */
  private boolean production;

  /** representing gateway endpoints */
  private Map<String, String> gatewayPaths;

  /** the list of supported identity providers */
  private List<IdentityProvider> identityProviders;

  /** configuration for ngx logger */
  private NGXLoggerConfig ngxLoggerConfig;

  /** representing feature flags for Meldeportal */
  private Map<String, Boolean> featureFlags;
}
