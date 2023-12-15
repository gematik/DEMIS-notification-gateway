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

import static de.gematik.demis.notificationgateway.BaseTestUtils.TOKEN_WITH_NO_IK_AND_PREFERRED_USERNAME;
import static de.gematik.demis.notificationgateway.BaseTestUtils.TOKEN_WITH_NO_IK_AND_PREFERRED_USERNAME_5_2_260550131;
import static de.gematik.demis.notificationgateway.BaseTestUtils.TOKEN_WITH_NO_IK_AND_PREFERRED_USERNAME_5_3_123456789;
import static de.gematik.demis.notificationgateway.common.utils.JwtUtils.WHITE_SPACES_REGEX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.jwt.JwtException;

class JwtUtilsTest {

  @ParameterizedTest
  @ValueSource(strings = {"123456789", "12 34 56 78 9"})
  void givenValidIKClaimWhenExtractIkNumberThenIkNotBlank(String ik) {
    final String token =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9"
            + ".eyJpc3MiOiJodHRwczovL2F1dGguaW5ncmV"
            + "zcy5sb2NhbC9yZWFsbXMvUE9SVEFMIiwiaWF"
            + "0IjoxNzAxNzgzNDY5LCJleHAiOjE3MDE3ODM"
            + "0NzEsImF1ZCI6Im1lbGRlcG9ydGFsIiwic3V"
            + "iIjoiNmRlZDlkZWItYTYyZi00MWI2LTlkZDY"
            + "tMmU2MzE0YjY2ZWVjIiwiaWsiOiIxMjM0NTY"
            + "3ODkifQ.zGkEOkyoXmXLDEcammvDEkA9Bh9e"
            + "oAbA7-rWbEjkqSM";

    final String expectedIK = JwtUtils.extractIkNumber(token, null);

    assertThat(expectedIK).isNotNull().isEqualTo(ik.replaceAll(WHITE_SPACES_REGEX, ""));
  }

  @Test
  void givenInValidIkClaimWhenExtractIkNumberThenBlankIk() {
    // ik claim is abcabc
    final String token =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9"
            + ".eyJpc3MiOiJodHRwczovL2F1dGguaW5ncmV"
            + "zcy5sb2NhbC9yZWFsbXMvUE9SVEFMIiwiaWF"
            + "0IjoxNzAxNzgzNDY5LCJleHAiOjE3MDE3ODM"
            + "0NzEsImF1ZCI6Im1lbGRlcG9ydGFsIiwic3V"
            + "iIjoiNmRlZDlkZWItYTYyZi00MWI2LTlkZDY"
            + "tMmU2MzE0YjY2ZWVjIiwiaWsiOiJhYmNhYmM"
            + "ifQ.yKe3bnfoBjEKa1ZtNcdxospM1xbFTIJ9"
            + "Xy5L1-ocWPE";

    final String expectedIK = JwtUtils.extractIkNumber(token, null);

    assertThat(expectedIK).isBlank();
  }

  @Test
  void givenNoIkClaimAndValidPreferredUsernameWhenExtractIkNumberThenIkNotBlank() {
    final String expectedIK = "260550131";
    // missing ik claim and preferred_username = "5-2-260550131"
    final String ik =
        JwtUtils.extractIkNumber(TOKEN_WITH_NO_IK_AND_PREFERRED_USERNAME_5_2_260550131, null);

    assertThat(ik).isNotNull().isEqualTo(expectedIK);
  }

  @Test
  void givenNoIKClaimAndInvalidPreferredUsernameWhenExtractIkNumberThenUseTestIKProperty() {
    // preferred_username = "5-3-123456789"
    String expectedIK = "123456789";

    final String ik =
        JwtUtils.extractIkNumber(TOKEN_WITH_NO_IK_AND_PREFERRED_USERNAME_5_3_123456789, expectedIK);

    assertThat(ik).isNotNull().isEqualTo(expectedIK);
  }

  @Test
  void givenMissingIkWhenExtractIkNumberThenBlankIk() {
    final String expectedIK =
        JwtUtils.extractIkNumber(TOKEN_WITH_NO_IK_AND_PREFERRED_USERNAME, null);

    assertThat(expectedIK).isBlank();
  }

  @Test
  void givenMalformedTokenThenThrowsException() {
    final String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwc.6-q0b";

    assertThatThrownBy(() -> JwtUtils.extractIkNumber(token, null))
        .isInstanceOf(JwtException.class);
  }
}
