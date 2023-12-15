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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import de.gematik.demis.notificationgateway.common.exceptions.FederatedIDPTokenCheckException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class FederatedIdentityTokenCheckerTest {
  @Autowired FederatedIdentityTokenChecker tokenChecker;

  @Test
  void givenFederatedIDPTokenWhenCheckIssuerEqualsPortalThenTrue() {
    final String token =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9"
            + ".eyJpc3MiOiJodHRwczovL3FzLmRlbWlzLnJ"
            + "raS5kZS9hdXRoL3JlYWxtcy9QT1JUQUwiLCJ"
            + "pYXQiOjE3MDExNzk4MzAsImV4cCI6MTcwMTE"
            + "3OTgzMCwiYXVkIjoieHh4LXh4eC14eHgiLCJ"
            + "zdWIiOiJ0ZXN0LXh4eCJ9.DhlOBszfQ_KiIT"
            + "aGc2SPYtZahz1JPKyqPMYHS9CneVo";

    final boolean expectedTrue = tokenChecker.isFederatedIdpToken(token);

    assertThat(expectedTrue).isTrue();
  }

  @Test
  void givenRandomTokenWhenCheckIssuerEqualsPortalThenFalse() {
    final String token =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9"
            + ".eyJpc3MiOiJodHRwczovL2F1dGguaW5ncmV"
            + "zcy5sb2NhbC9yZWFsbXMvUkFORE9NUkVBTE0"
            + "iLCJpYXQiOjE3MDA4MzIwNzUsImV4cCI6MTc"
            + "wMDgzMzI3NCwiYXVkIjoieHh4eC1yZWNpcGl"
            + "lbnQiLCJzdWIiOiJ4eHh4LXVzZXIifQ.wg-h"
            + "lk48piGvXRmesvAKVjzM0seIg147ONAqLSlU"
            + "1v0";

    final boolean expected = tokenChecker.isFederatedIdpToken(token);

    assertThat(expected).isFalse();
  }

  @Test
  void givenNullTokenWhenCheckIssuerEqualsPortalThenFalse() {
    final boolean expected = tokenChecker.isFederatedIdpToken(null);

    assertThat(expected).isFalse();
  }

  @Test
  void givenEmptyIssuerThenFalse() {
    final String token =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9"
            + ".eyJpc3MiOiIiLCJpYXQiOjE3MDE3OTY0NDk"
            + "sImV4cCI6MTcwMTc5NjQ1MCwiYXVkIjoidGV"
            + "zdCIsInN1YiI6IiJ9.x4FNHGiJfyO62VjF_P"
            + "d1LoEEMoPHNnBzOeZQJVjVPqc";

    final boolean expected = tokenChecker.isFederatedIdpToken(token);

    assertThat(expected).isFalse();
  }

  @Test
  void givenMalformedTokenThenThrowsFederatedIDPTokenCheckException() {
    final String token =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9"
            + ".eyJpc3MiOiIiLCJpYXQiOjE3MDE3OTY0NDk"
            + "sImV4cCI6MTcwMTc5NjQ1MCwiYXVkIjoidGV"
            + "zdCIsInN1YDEGIiJ9.x4FNHGiJfyO62VjF_P"
            + "d1LoEEMoPHNnBz";

    assertThatThrownBy(() -> tokenChecker.isFederatedIdpToken(token))
        .isInstanceOf(FederatedIDPTokenCheckException.class);
  }
}
