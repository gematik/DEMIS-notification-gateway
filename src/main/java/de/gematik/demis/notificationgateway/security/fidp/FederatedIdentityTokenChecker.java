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

package de.gematik.demis.notificationgateway.security.fidp;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import de.gematik.demis.notificationgateway.common.exceptions.FederatedIDPTokenCheckException;
import java.text.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource("classpath:application.properties")
public class FederatedIdentityTokenChecker {
  private final String federatedIssuer;

  @Autowired
  public FederatedIdentityTokenChecker(
      @Value("${idp.issuers.demis.federated-issuer}") String federatedIssuer) {
    this.federatedIssuer = federatedIssuer;
  }

  public boolean isFederatedIdpToken(String bearerToken) {
    try {
      if (bearerToken == null) {
        return false;
      }
      final JWT jwt = JWTParser.parse(bearerToken);
      final String issuer = jwt.getJWTClaimsSet().getIssuer();
      return issuer != null && issuer.contains(federatedIssuer);
    } catch (ParseException pse) {
      throw new FederatedIDPTokenCheckException(pse);
    }
  }
}
