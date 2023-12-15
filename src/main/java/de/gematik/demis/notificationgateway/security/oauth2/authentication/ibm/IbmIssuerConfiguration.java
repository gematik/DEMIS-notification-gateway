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

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Configuration for IBM (Ulbrich) IDP. */
@Getter
@Setter
public class IbmIssuerConfiguration {

  /** list of allowed token issuers */
  private List<String> allowedIssuers;

  /** list of allowed token profession-OIDs */
  private List<String> professionOIDs;

  /** the jwk-set uri */
  private String jwkSetUri;

  /** the jws algorithm */
  private String jwsAlgorithm;
}
