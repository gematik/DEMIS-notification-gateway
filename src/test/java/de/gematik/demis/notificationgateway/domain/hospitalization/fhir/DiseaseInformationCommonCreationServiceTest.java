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

package de.gematik.demis.notificationgateway.domain.hospitalization.fhir;

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.CODE_SYSTEM_GEOGRAPHIC_REGION;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.CODE_SYSTEM_NULL_FLAVOR;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.CODE_SYSTEM_YES_OR_NO_ANSWER;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.QUESTIONAIRE_DISEASE_QUESTIONS_COMMON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.gematik.demis.notificationgateway.common.constants.FhirConstants;
import de.gematik.demis.notificationgateway.common.dto.DiseaseInfoCommon;
import de.gematik.demis.notificationgateway.common.dto.Hospitalization;
import de.gematik.demis.notificationgateway.common.exceptions.BadRequestException;
import de.gematik.demis.notificationgateway.utils.FileUtils;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DiseaseInformationCommonCreationServiceTest {

  private final DiseaseInformationCommonCreationService creationService =
      new DiseaseInformationCommonCreationService();

  @SuppressWarnings("unused")
  private static Stream<Arguments> provideContentWithDifferentGeographicRegions() {
    return Stream.of(
        Arguments.of(
            "portal/disease/diseaseinformationcommon/region/notification_content_region_Maledives.json",
            CODE_SYSTEM_GEOGRAPHIC_REGION,
            "21000229",
            "Malediven"),
        Arguments.of(
            "portal/disease/diseaseinformationcommon/region/notification_content_region_NASK.json",
            CODE_SYSTEM_NULL_FLAVOR,
            "NASK",
            "not asked"),
        Arguments.of(
            "portal/disease/diseaseinformationcommon/region/notification_content_region_ASKU.json",
            CODE_SYSTEM_NULL_FLAVOR,
            "ASKU",
            "asked but unknown"));
  }

  @Test
  void testCreateDiseaseInformationCommonWithMinimumInput()
      throws JsonProcessingException, BadRequestException {
    Hospitalization hospitalization =
        FileUtils.createHospitalization("portal/disease/notification_content_min.json");

    final QuestionnaireResponse diseaseInformationCommon =
        creationService.createDiseaseInformationCommon(
            hospitalization.getDisease().getDiseaseInfoCommon(),
            new Patient(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            null);

    assertNotNull(diseaseInformationCommon);
    assertTrue(diseaseInformationCommon.hasId());

    assertTrue(diseaseInformationCommon.hasMeta());
    final Meta meta = diseaseInformationCommon.getMeta();
    assertTrue(meta.hasProfile());
    assertEquals(
        FhirConstants.PROFILE_DISEASE_INFORMATION_COMMON, meta.getProfile().get(0).asStringValue());

    assertEquals(
        QUESTIONAIRE_DISEASE_QUESTIONS_COMMON, diseaseInformationCommon.getQuestionnaire());
    assertEquals(QuestionnaireResponseStatus.COMPLETED, diseaseInformationCommon.getStatus());
    assertTrue(diseaseInformationCommon.hasSubject());

    final List<QuestionnaireResponseItemComponent> items = diseaseInformationCommon.getItem();
    assertEquals(7, items.size());

    final QuestionnaireResponseItemComponent deadItem = items.get(0);
    checkItem(deadItem, "isDead", CODE_SYSTEM_YES_OR_NO_ANSWER, "no", "Nein");

    final QuestionnaireResponseItemComponent militaryItem = items.get(1);
    checkItem(militaryItem, "militaryAffiliation", CODE_SYSTEM_NULL_FLAVOR, "NASK", "not asked");

    final QuestionnaireResponseItemComponent labSpecimenItem = items.get(2);
    checkItem(labSpecimenItem, "labSpecimenTaken", CODE_SYSTEM_YES_OR_NO_ANSWER, "no", "Nein");

    final QuestionnaireResponseItemComponent hospitalizedItem = items.get(3);
    checkItem(hospitalizedItem, "hospitalized", CODE_SYSTEM_YES_OR_NO_ANSWER, "no", "Nein");

    final QuestionnaireResponseItemComponent infectProtectFacilityItem = items.get(4);
    checkItem(
        infectProtectFacilityItem,
        "infectProtectFacility",
        CODE_SYSTEM_YES_OR_NO_ANSWER,
        "no",
        "Nein");

    final QuestionnaireResponseItemComponent placeExposureItem = items.get(5);
    checkItem(placeExposureItem, "placeExposure", CODE_SYSTEM_YES_OR_NO_ANSWER, "no", "Nein");

    final QuestionnaireResponseItemComponent organDonationItem = items.get(6);
    checkItem(organDonationItem, "organDonation", CODE_SYSTEM_NULL_FLAVOR, "NASK", "not asked");
  }

  @ParameterizedTest
  @MethodSource("provideContentWithDifferentGeographicRegions")
  void testCreateDiseaseInformationCommonContainsCorrectRegionCoding(
      String inputFile,
      String expectedCodingSystem,
      String expectedCodingCode,
      String expectedCodingDisplay)
      throws JsonProcessingException, BadRequestException {
    Hospitalization hospitalization = FileUtils.createHospitalization(inputFile);

    final QuestionnaireResponse diseaseInformationCommon =
        creationService.createDiseaseInformationCommon(
            hospitalization.getDisease().getDiseaseInfoCommon(),
            new Patient(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            null);

    final List<QuestionnaireResponseItemComponent> items = diseaseInformationCommon.getItem();
    final QuestionnaireResponseItemComponent placeExposureItem = items.get(5);
    final QuestionnaireResponseItemComponent placeExposureGroupItem =
        placeExposureItem.getAnswer().get(0).getItem().get(0);
    final QuestionnaireResponseItemComponent placeExposureRegionItem =
        placeExposureGroupItem.getItem().get(2);

    final Coding regionCoding = (Coding) placeExposureRegionItem.getAnswer().get(0).getValue();
    assertEquals(expectedCodingSystem, regionCoding.getSystem());
    assertEquals(expectedCodingCode, regionCoding.getCode());
    assertEquals(expectedCodingDisplay, regionCoding.getDisplay());
  }

  @Test
  void testCreateDiseaseInformationCommonWithInvalidGeographicRegionThrowsException()
      throws JsonProcessingException {
    Hospitalization hospitalization =
        FileUtils.createHospitalization(
            "portal/disease/diseaseinformationcommon/region/notification_content_region_invalid.json");
    final DiseaseInfoCommon diseaseInfoCommon = hospitalization.getDisease().getDiseaseInfoCommon();

    Assertions.assertThatThrownBy(
            () ->
                creationService.createDiseaseInformationCommon(
                    diseaseInfoCommon,
                    new Patient(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    null))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("invalid geographic region for place exposure: invalid");
  }

  private void checkItem(
      QuestionnaireResponseItemComponent itemToCheck,
      String expectedLinkId,
      String expectedCodingSystem,
      String expectedCodingCode,
      String expectedCodingDisplay) {
    assertEquals(expectedLinkId, itemToCheck.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent> deadAnswerList = itemToCheck.getAnswer();
    assertEquals(1, deadAnswerList.size());
    final QuestionnaireResponseItemAnswerComponent deadAnswer = deadAnswerList.get(0);
    final Coding deadCoding = (Coding) deadAnswer.getValue();
    assertEquals(expectedCodingSystem, deadCoding.getSystem());
    assertEquals(expectedCodingCode, deadCoding.getCode());
    assertEquals(expectedCodingDisplay, deadCoding.getDisplay());
  }
}
