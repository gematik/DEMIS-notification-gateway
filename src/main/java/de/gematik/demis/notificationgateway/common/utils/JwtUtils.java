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

package de.gematik.demis.notificationgateway.common.utils;

import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.PREFERRED_USERNAME;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import java.text.ParseException;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.oauth2.jwt.JwtException;

@UtilityClass
@Slf4j
public class JwtUtils {

  public static final String WHITE_SPACES_REGEX = "\\s+";
  public static final String EXACTLY_9_NUMBERS_REGEX = "^\\d{9}";
  public static final String IK_CLAIM = "ik";
  public static final String TEST_IK_PROPERTY = "test-ik";

  public static String extractIkNumber(String token, String testIKProperty) {
    return getIk(token, testIKProperty);
  }

  private static String getIk(String token, String testIKProperty) {
    try {
      final JWT jwt = JWTParser.parse(token);
      // first from ik-claim
      String ik = extractClaimFromToken(IK_CLAIM, jwt);
      if (StringUtils.isBlank(ik)) {
        // second from preferred_username claim
        ik = extractIkFromPreferredUsername(jwt);
        if (StringUtils.isBlank(ik)) {
          // third from test-ik property
          log.warn("no ik found in jwt, trying to use test-ik property instead");
          ik = testIKProperty;
        }
      }
      // clean white spaces
      if (!StringUtils.isBlank(ik)) {
        ik = ik.replaceAll(WHITE_SPACES_REGEX, "");
      }
      // validate ik pattern
      if (!isValidIkNumber(ik)) {
        return null;
      }
      return ik;
    } catch (ParseException pse) {
      throw new JwtException(pse.getMessage());
    }
  }

  private static String extractIkFromPreferredUsername(JWT jwt) throws ParseException {
    String preferredUsername = extractClaimFromToken(PREFERRED_USERNAME, jwt);
    if (StringUtils.isBlank(preferredUsername) || !preferredUsername.startsWith("5-2-")) {
      log.warn(
          "the preferredUsername '{}' is blank or does not start with '5-2-'", preferredUsername);
      return null;
    }
    try {
      return preferredUsername.split("-")[2];
    } catch (Exception ex) {
      log.warn(
          "failed to extract ik from preferredUsername {} - {}",
          preferredUsername,
          ex.getLocalizedMessage());
      return null;
    }
  }

  private static boolean isValidIkNumber(String ik) {
    if (StringUtils.isBlank(ik)) {
      log.warn("missing ik number");
      return false;
    }
    final boolean matches = Pattern.matches(EXACTLY_9_NUMBERS_REGEX, ik);
    if (!matches) {
      log.warn("the extracted ik does not match the specified pattern");
    }
    return matches;
  }

  private static String extractClaimFromToken(String claim, JWT jwt) throws ParseException {
    if (jwt.getJWTClaimsSet() != null && jwt.getJWTClaimsSet().getClaim(claim) != null) {
      return jwt.getJWTClaimsSet().getClaim(claim).toString();
    }
    return null;
  }
}
