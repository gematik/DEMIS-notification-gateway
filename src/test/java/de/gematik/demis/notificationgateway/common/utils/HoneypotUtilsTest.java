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

import static de.gematik.demis.notificationgateway.utils.FileUtils.loadJsonFromFile;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.demis.notificationgateway.BaseTestUtils;
import de.gematik.demis.notificationgateway.common.dto.BedOccupancy;
import de.gematik.demis.notificationgateway.common.dto.Hospitalization;
import de.gematik.demis.notificationgateway.common.dto.PathogenTest;
import de.gematik.demis.notificationgateway.common.dto.QuickTest;
import java.io.IOException;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class HoneypotUtilsTest implements BaseTestUtils {

  private static final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @ParameterizedTest
  @MethodSource("pathogenTestProvider")
  void givenPathogenTestWhenIsSpammerThenUseExpected(PathogenTest pathogenTest, boolean expected) {
    final boolean isSpammer = HoneypotUtils.isPathogenSpammer(pathogenTest);
    assertThat(isSpammer).isEqualTo(expected);
  }

  private static Stream<Arguments> pathogenTestProvider() throws IOException {
    String jsonContent = loadJsonFromFile("/portal/pathogen/pathogen-test.json");
    assert jsonContent != null;

    PathogenTest facilitySpammer = objectMapper.readValue(jsonContent, PathogenTest.class);
    facilitySpammer.getNotifierFacility().oneTimeCode(RandomStringUtils.randomAlphabetic(5));

    PathogenTest personSpammer = objectMapper.readValue(jsonContent, PathogenTest.class);
    personSpammer.getNotifiedPerson().oneTimeCode(RandomStringUtils.randomAlphabetic(5));

    PathogenTest pathogenSpammer = objectMapper.readValue(jsonContent, PathogenTest.class);
    pathogenSpammer.getPathogenDTO().oneTimeCode(RandomStringUtils.randomAlphabetic(5));

    PathogenTest fullSpammer = objectMapper.readValue(jsonContent, PathogenTest.class);
    fullSpammer.getNotifierFacility().oneTimeCode(RandomStringUtils.randomAlphabetic(5));
    fullSpammer.getNotifiedPerson().oneTimeCode(RandomStringUtils.randomAlphabetic(5));
    fullSpammer.getPathogenDTO().oneTimeCode(RandomStringUtils.randomAlphabetic(5));

    PathogenTest noSpammer = objectMapper.readValue(jsonContent, PathogenTest.class);

    return Stream.of(
        Arguments.of(noSpammer, false),
        Arguments.of(facilitySpammer, true),
        Arguments.of(personSpammer, true),
        Arguments.of(pathogenSpammer, true),
        Arguments.of(fullSpammer, true));
  }

  @ParameterizedTest
  @MethodSource("quickTestProvider")
  void givenQuickTestWhenIsSpammerThenUseExpected(QuickTest quickTest, boolean expected) {
    final boolean isSpammer = HoneypotUtils.isQuickTestSpammer(quickTest);
    assertThat(isSpammer).isEqualTo(expected);
  }

  private static Stream<Arguments> quickTestProvider() throws IOException {
    String jsonContent = loadJsonFromFile("/portal/laboratory/notification_content.json");
    assert jsonContent != null;

    QuickTest facilitySpammer = objectMapper.readValue(jsonContent, QuickTest.class);
    facilitySpammer.getNotifierFacility().oneTimeCode(RandomStringUtils.randomAlphabetic(5));

    QuickTest personSpammer = objectMapper.readValue(jsonContent, QuickTest.class);
    personSpammer.getNotifiedPerson().oneTimeCode(RandomStringUtils.randomAlphabetic(5));

    QuickTest diagnosticSpammer = objectMapper.readValue(jsonContent, QuickTest.class);
    diagnosticSpammer.getDiagnostic().oneTimeCode(RandomStringUtils.randomAlphabetic(5));

    QuickTest fullSpammer = objectMapper.readValue(jsonContent, QuickTest.class);
    fullSpammer.getNotifierFacility().oneTimeCode(RandomStringUtils.randomAlphabetic(5));
    fullSpammer.getNotifiedPerson().oneTimeCode(RandomStringUtils.randomAlphabetic(5));
    fullSpammer.getDiagnostic().oneTimeCode(RandomStringUtils.randomAlphabetic(5));

    QuickTest noSpammer = objectMapper.readValue(jsonContent, QuickTest.class);

    return Stream.of(
        Arguments.of(noSpammer, false),
        Arguments.of(facilitySpammer, true),
        Arguments.of(personSpammer, true),
        Arguments.of(diagnosticSpammer, true),
        Arguments.of(fullSpammer, true));
  }

  @ParameterizedTest
  @MethodSource("hospitalizationProvider")
  void givenQuickTestWhenIsSpammerThenUseExpected(
      Hospitalization hospitalization, boolean expected) {
    final boolean isSpammer = HoneypotUtils.isHospitalizationSpammer(hospitalization);
    assertThat(isSpammer).isEqualTo(expected);
  }

  private static Stream<Arguments> hospitalizationProvider() throws IOException {
    String jsonContent = loadJsonFromFile("/portal/disease/notification_content_min.json");
    assert jsonContent != null;

    Hospitalization facilitySpammer = objectMapper.readValue(jsonContent, Hospitalization.class);
    facilitySpammer.getNotifierFacility().oneTimeCode(RandomStringUtils.randomAlphabetic(5));

    Hospitalization personSpammer = objectMapper.readValue(jsonContent, Hospitalization.class);
    personSpammer.getNotifiedPerson().oneTimeCode(RandomStringUtils.randomAlphabetic(5));

    Hospitalization infoSpammer = objectMapper.readValue(jsonContent, Hospitalization.class);
    infoSpammer.getDisease().getConditionInfo().oneTimeCode(RandomStringUtils.randomAlphabetic(5));

    Hospitalization commonSpammer = objectMapper.readValue(jsonContent, Hospitalization.class);
    commonSpammer
        .getDisease()
        .getDiseaseInfoCommon()
        .oneTimeCode(RandomStringUtils.randomAlphabetic(5));

    Hospitalization cvddSpammer = objectMapper.readValue(jsonContent, Hospitalization.class);
    cvddSpammer
        .getDisease()
        .getDiseaseInfoCVDD()
        .oneTimeCode(RandomStringUtils.randomAlphabetic(5));

    Hospitalization fullSpammer = objectMapper.readValue(jsonContent, Hospitalization.class);
    fullSpammer.getNotifierFacility().oneTimeCode(RandomStringUtils.randomAlphabetic(5));
    fullSpammer.getNotifiedPerson().oneTimeCode(RandomStringUtils.randomAlphabetic(5));
    fullSpammer.getDisease().getConditionInfo().oneTimeCode(RandomStringUtils.randomAlphabetic(5));
    fullSpammer
        .getDisease()
        .getDiseaseInfoCommon()
        .oneTimeCode(RandomStringUtils.randomAlphabetic(5));
    fullSpammer
        .getDisease()
        .getDiseaseInfoCVDD()
        .oneTimeCode(RandomStringUtils.randomAlphabetic(5));

    Hospitalization noSpammer = objectMapper.readValue(jsonContent, Hospitalization.class);

    return Stream.of(
        Arguments.of(noSpammer, false),
        Arguments.of(facilitySpammer, true),
        Arguments.of(personSpammer, true),
        Arguments.of(infoSpammer, true),
        Arguments.of(commonSpammer, true),
        Arguments.of(cvddSpammer, true),
        Arguments.of(fullSpammer, true));
  }

  @ParameterizedTest
  @MethodSource("bedOccupancyProvider")
  void givenPathogenTestWhenIsSpammerThenUseExpected(BedOccupancy bedOccupancy, boolean expected) {
    final boolean isSpammer = HoneypotUtils.isBedOccupancySpammer(bedOccupancy);
    assertThat(isSpammer).isEqualTo(expected);
  }

  private static Stream<Arguments> bedOccupancyProvider() throws IOException {
    String jsonContent = loadJsonFromFile("/portal/bedoccupancy/report_content_min.json");
    assert jsonContent != null;

    BedOccupancy facilitySpammer = objectMapper.readValue(jsonContent, BedOccupancy.class);
    facilitySpammer.getNotifierFacility().oneTimeCode(RandomStringUtils.randomAlphabetic(5));

    BedOccupancy personSpammer = objectMapper.readValue(jsonContent, BedOccupancy.class);
    personSpammer.getBedOccupancyQuestion().oneTimeCode(RandomStringUtils.randomAlphabetic(5));

    BedOccupancy fullSpammer = objectMapper.readValue(jsonContent, BedOccupancy.class);
    fullSpammer.getNotifierFacility().oneTimeCode(RandomStringUtils.randomAlphabetic(5));
    fullSpammer.getBedOccupancyQuestion().oneTimeCode(RandomStringUtils.randomAlphabetic(5));

    BedOccupancy noSpammer = objectMapper.readValue(jsonContent, BedOccupancy.class);

    return Stream.of(
        Arguments.of(noSpammer, false),
        Arguments.of(facilitySpammer, true),
        Arguments.of(personSpammer, true),
        Arguments.of(fullSpammer, true));
  }
}
