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

import de.gematik.demis.notificationgateway.security.oauth2.authentication.demis.DemisIssuerConfiguration;
import de.gematik.demis.notificationgateway.security.oauth2.authentication.ibm.IbmIssuerConfiguration;
import java.util.List;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/** Configuration Properties for IDP Issuers. */
@ConfigurationProperties(prefix = "idp.issuers")
@Component
@Setter
public class OAuthIdpConfiguration {

  private List<String> issuers = List.of("ibm", "demis");

  private DemisIssuerConfiguration demis;

  private IbmIssuerConfiguration ibm;

  /**
   * Returns the configured IDP Issuers.
   *
   * @return a list of configured Issuers
   */
  public List<String> getIssuers() {
    return issuers;
  }

  /**
   * Returns the Demis Issuer configuration, if found and enable.
   *
   * @return an instance of {@link DemisIssuerConfiguration}, otherwise null
   */
  @Nullable
  public DemisIssuerConfiguration getDemisIssuerConfiguration() {
    if (!issuers.contains("demis")) {
      return null;
    }
    return demis;
  }

  /**
   * Returns the IBM Issuer configuration, if found and enable.
   *
   * @return an instance of {@link IbmIssuerConfiguration}, otherwise null
   */
  @Nullable
  public IbmIssuerConfiguration getIbmIssuerConfiguration() {
    if (!issuers.contains("ibm")) {
      return null;
    }
    return ibm;
  }
}
