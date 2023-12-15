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

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.CODE_SYSTEM_NULL_FLAVOR;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.CODE_SYSTEM_YES_OR_NO_ANSWER;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.QUESTIONAIRE_DISEASE_QUESTIONS_CVDD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.gematik.demis.notificationgateway.common.constants.FhirConstants;
import de.gematik.demis.notificationgateway.common.dto.Hospitalization;
import de.gematik.demis.notificationgateway.common.exceptions.BadRequestException;
import de.gematik.demis.notificationgateway.common.utils.ConfiguredCodeSystems;
import de.gematik.demis.notificationgateway.utils.FileUtils;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
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

class DiseaseInformationCVDDCreationServiceTest {

  private final DiseaseInformationCVDDCreationService creationService =
      new DiseaseInformationCVDDCreationService();

  @Test
  void testCreateDiseaseInfoCVDDWithMinimumInput()
      throws JsonProcessingException, BadRequestException {
    Hospitalization hospitalization =
        FileUtils.createHospitalization("portal/disease/notification_content_min.json");

    final QuestionnaireResponse diseaseInformationCVDD =
        creationService.createDiseaseInformationCVDD(
            hospitalization.getDisease().getDiseaseInfoCVDD(),
            new Patient(),
            Collections.emptyList());

    assertNotNull(diseaseInformationCVDD);
    assertTrue(diseaseInformationCVDD.hasId());

    assertTrue(diseaseInformationCVDD.hasMeta());
    final Meta meta = diseaseInformationCVDD.getMeta();
    assertTrue(meta.hasProfile());
    assertEquals(
        FhirConstants.PROFILE_DISEASE_INFORMATION_CVDD, meta.getProfile().get(0).asStringValue());

    assertEquals(QUESTIONAIRE_DISEASE_QUESTIONS_CVDD, diseaseInformationCVDD.getQuestionnaire());
    assertEquals(QuestionnaireResponseStatus.COMPLETED, diseaseInformationCVDD.getStatus());
    assertTrue(diseaseInformationCVDD.hasSubject());

    final List<QuestionnaireResponseItemComponent> items = diseaseInformationCVDD.getItem();
    assertEquals(3, items.size());

    final QuestionnaireResponseItemComponent infectionSourceItem = items.get(0);
    assertEquals("infectionSource", infectionSourceItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent> infectionSourceAnswerList =
        infectionSourceItem.getAnswer();
    assertEquals(1, infectionSourceAnswerList.size());
    final QuestionnaireResponseItemAnswerComponent infectionSourceAnswer =
        infectionSourceAnswerList.get(0);
    final Coding infectionSourceCoding = (Coding) infectionSourceAnswer.getValue();
    assertEquals(CODE_SYSTEM_NULL_FLAVOR, infectionSourceCoding.getSystem());
    assertEquals("NASK", infectionSourceCoding.getCode());
    assertEquals("not asked", infectionSourceCoding.getDisplay());

    final QuestionnaireResponseItemComponent infectionEnvironmentSettingItem = items.get(1);
    assertEquals("infectionEnvironmentSetting", infectionEnvironmentSettingItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent> infectionEnvironmentSettingAnswerList =
        infectionEnvironmentSettingItem.getAnswer();
    assertEquals(1, infectionEnvironmentSettingAnswerList.size());
    final QuestionnaireResponseItemAnswerComponent infectionEnvironmentSettingAnswer =
        infectionEnvironmentSettingAnswerList.get(0);
    final Coding infectionEnvironmentSettingCoding =
        (Coding) infectionEnvironmentSettingAnswer.getValue();
    assertEquals(CODE_SYSTEM_YES_OR_NO_ANSWER, infectionEnvironmentSettingCoding.getSystem());
    assertEquals("no", infectionEnvironmentSettingCoding.getCode());
    assertEquals("Nein", infectionEnvironmentSettingCoding.getDisplay());

    final QuestionnaireResponseItemComponent immunizationItem = items.get(2);
    assertEquals("immunization", immunizationItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent> immunizationAnswerList =
        immunizationItem.getAnswer();
    assertEquals(1, immunizationAnswerList.size());
    final QuestionnaireResponseItemAnswerComponent immunizationAnswer =
        immunizationAnswerList.get(0);
    final Coding immunizationStatusCoding = (Coding) immunizationAnswer.getValue();
    assertEquals(CODE_SYSTEM_NULL_FLAVOR, immunizationStatusCoding.getSystem());
    assertEquals("ASKU", immunizationStatusCoding.getCode());
    assertEquals("asked but unknown", immunizationStatusCoding.getDisplay());
  }

  @SuppressWarnings("unused")
  private static Stream<Arguments> provideContactToInfectedValues() {
    return Stream.of(
        Arguments.of(
            "portal/disease/diseaseinformationcvdd/contacttoinfected/notification_content_contact_yes.json",
            ConfiguredCodeSystems.getInstance().getYesOrNoCoding("yes")),
        Arguments.of(
            "portal/disease/diseaseinformationcvdd/contacttoinfected/notification_content_contact_no.json",
            ConfiguredCodeSystems.getInstance().getYesOrNoCoding("no")),
        Arguments.of(
            "portal/disease/diseaseinformationcvdd/contacttoinfected/notification_content_contact_indeterminate.json",
            ConfiguredCodeSystems.getInstance().getNullFlavor("ASKU")),
        Arguments.of(
            "portal/disease/diseaseinformationcvdd/contacttoinfected/notification_content_contact_unknown.json",
            ConfiguredCodeSystems.getInstance().getNullFlavor("NASK")));
  }

  @ParameterizedTest
  @MethodSource("provideContactToInfectedValues")
  void testContactToInfected(String inputFile, Coding expectedCoding)
      throws JsonProcessingException, BadRequestException {
    Hospitalization hospitalization = FileUtils.createHospitalization(inputFile);
    final QuestionnaireResponse diseaseInformationCVDD =
        creationService.createDiseaseInformationCVDD(
            hospitalization.getDisease().getDiseaseInfoCVDD(),
            new Patient(),
            Collections.emptyList());

    assertNotNull(diseaseInformationCVDD);
    assertTrue(diseaseInformationCVDD.hasItem());

    final QuestionnaireResponseItemComponent infectionSourceItem =
        diseaseInformationCVDD.getItem().get(0);
    assertEquals("infectionSource", infectionSourceItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent> infectionSourceAnswerList =
        infectionSourceItem.getAnswer();
    assertEquals(1, infectionSourceAnswerList.size());
    final QuestionnaireResponseItemAnswerComponent infectionSourceAnswer =
        infectionSourceAnswerList.get(0);
    final Coding infectionSourceCoding = (Coding) infectionSourceAnswer.getValue();
    assertEquals(expectedCoding.getSystem(), infectionSourceCoding.getSystem());
    assertEquals(expectedCoding.getCode(), infectionSourceCoding.getCode());
    assertEquals(expectedCoding.getDisplay(), infectionSourceCoding.getDisplay());
  }
}
