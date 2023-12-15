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

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.PROFILE_DISEASE_INFORMATION_CVDD;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.QUESTIONAIRE_DISEASE_QUESTIONS_CVDD;

import de.gematik.demis.notificationgateway.common.dto.DiseaseInfoCVDD;
import de.gematik.demis.notificationgateway.common.dto.DiseaseInfoCVDD.ContactToInfectedEnum;
import de.gematik.demis.notificationgateway.common.dto.InfectionEnvironmentInfo;
import de.gematik.demis.notificationgateway.common.dto.InfectionEnvironmentQuestion.InfectionEnvironmentExistingEnum;
import de.gematik.demis.notificationgateway.common.dto.VaccinationQuestion;
import de.gematik.demis.notificationgateway.common.exceptions.BadRequestException;
import de.gematik.demis.notificationgateway.common.utils.ConfiguredCodeSystems;
import de.gematik.demis.notificationgateway.common.utils.DateUtils;
import de.gematik.demis.notificationgateway.common.utils.ReferenceUtils;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseStatus;
import org.springframework.stereotype.Service;

@Service
public class DiseaseInformationCVDDCreationService {

  public QuestionnaireResponse createDiseaseInformationCVDD(
      DiseaseInfoCVDD diseaseInfoCVDD, Patient notifiedPerson, List<Immunization> vaccinations)
      throws BadRequestException {
    final QuestionnaireResponse questionnaireResponse = new QuestionnaireResponse();
    questionnaireResponse.setId(UUID.randomUUID().toString());
    questionnaireResponse.setMeta(new Meta().addProfile(PROFILE_DISEASE_INFORMATION_CVDD));

    questionnaireResponse.setQuestionnaire(QUESTIONAIRE_DISEASE_QUESTIONS_CVDD);
    questionnaireResponse.setStatus(QuestionnaireResponseStatus.COMPLETED);
    questionnaireResponse.setSubject(ReferenceUtils.createReference(notifiedPerson));
    addItems(questionnaireResponse, diseaseInfoCVDD, vaccinations);

    return questionnaireResponse;
  }

  private void addItems(
      QuestionnaireResponse questionnaireResponse,
      DiseaseInfoCVDD diseaseInfoCVDD,
      List<Immunization> vaccinations)
      throws BadRequestException {
    addInfectionSource(questionnaireResponse, diseaseInfoCVDD);
    addInfectionEnvironmentSetting(questionnaireResponse, diseaseInfoCVDD);
    addImmunization(questionnaireResponse, diseaseInfoCVDD, vaccinations);
  }

  private void addImmunization(
      QuestionnaireResponse questionnaireResponse,
      DiseaseInfoCVDD diseaseInfoCVDD,
      List<Immunization> vaccinations) {
    final QuestionnaireResponseItemComponent immunization =
        questionnaireResponse.addItem().setLinkId("immunization");
    final QuestionnaireResponseItemAnswerComponent immunizationAnswer = immunization.addAnswer();

    final VaccinationQuestion.ImmunizationStatusEnum immunizationStatus =
        diseaseInfoCVDD.getVaccinationQuestion().getImmunizationStatus();
    Coding immunizationStatusCoding;
    switch (immunizationStatus) {
      case YES:
        immunizationStatusCoding = ConfiguredCodeSystems.getInstance().getYesOrNoCoding("yes");
        break;
      case NO:
        immunizationStatusCoding = ConfiguredCodeSystems.getInstance().getYesOrNoCoding("no");
        break;
      case INDETERMINATE:
        immunizationStatusCoding = ConfiguredCodeSystems.getInstance().getNullFlavor("ASKU");
        break;
      case UNKNOWN:
        immunizationStatusCoding = ConfiguredCodeSystems.getInstance().getNullFlavor("NASK");
        break;
      default:
        throw new IllegalStateException("unknown immunization status: " + immunizationStatus);
    }

    immunizationAnswer.setValue(immunizationStatusCoding);

    if (immunizationStatus == VaccinationQuestion.ImmunizationStatusEnum.YES) {
      for (Immunization vaccination : vaccinations) {
        final QuestionnaireResponseItemComponent immunizationRef =
            immunizationAnswer.addItem().setLinkId("immunizationRef");
        immunizationRef.addAnswer().setValue(ReferenceUtils.createReference(vaccination));
      }
    }
  }

  private void addInfectionSource(
      QuestionnaireResponse questionnaireResponse, DiseaseInfoCVDD diseaseInfoCVDD) {
    final QuestionnaireResponseItemComponent infectionSource =
        questionnaireResponse.addItem().setLinkId("infectionSource");
    final QuestionnaireResponseItemAnswerComponent infectionSourceAnswer =
        infectionSource.addAnswer();

    final ContactToInfectedEnum contactToInfected = diseaseInfoCVDD.getContactToInfected();
    Coding infectionSourceCoding;
    switch (contactToInfected) {
      case YES:
        infectionSourceCoding = ConfiguredCodeSystems.getInstance().getYesOrNoCoding("yes");
        break;
      case NO:
        infectionSourceCoding = ConfiguredCodeSystems.getInstance().getYesOrNoCoding("no");
        break;
      case INDETERMINATE:
        infectionSourceCoding = ConfiguredCodeSystems.getInstance().getNullFlavor("ASKU");
        break;
      case UNKNOWN:
        infectionSourceCoding = ConfiguredCodeSystems.getInstance().getNullFlavor("NASK");
        break;
      default:
        throw new IllegalStateException("unknown contact to infected value: " + contactToInfected);
    }

    infectionSourceAnswer.setValue(infectionSourceCoding);
  }

  private void addInfectionEnvironmentSetting(
      QuestionnaireResponse questionnaireResponse, DiseaseInfoCVDD diseaseInfoCVDD)
      throws BadRequestException {
    final QuestionnaireResponseItemComponent infectionEnvironmentSetting =
        questionnaireResponse.addItem().setLinkId("infectionEnvironmentSetting");
    final QuestionnaireResponseItemAnswerComponent infectionEnvironmentSettingAnswer =
        infectionEnvironmentSetting.addAnswer();

    final InfectionEnvironmentExistingEnum infectionEnvironmentSettingValue =
        diseaseInfoCVDD.getInfectionEnvironmentQuestion().getInfectionEnvironmentExisting();
    Coding infectionEnvironmentSettingCoding;
    switch (infectionEnvironmentSettingValue) {
      case YES:
        infectionEnvironmentSettingCoding =
            ConfiguredCodeSystems.getInstance().getYesOrNoCoding("yes");
        break;
      case NO:
        infectionEnvironmentSettingCoding =
            ConfiguredCodeSystems.getInstance().getYesOrNoCoding("no");
        break;
      case INDETERMINATE:
        infectionEnvironmentSettingCoding =
            ConfiguredCodeSystems.getInstance().getNullFlavor("ASKU");
        break;
      case UNKNOWN:
        infectionEnvironmentSettingCoding =
            ConfiguredCodeSystems.getInstance().getNullFlavor("NASK");
        break;
      default:
        throw new IllegalStateException(
            "unknown infection environment setting value: " + infectionEnvironmentSettingValue);
    }

    infectionEnvironmentSettingAnswer.setValue(infectionEnvironmentSettingCoding);

    final InfectionEnvironmentInfo infectionEnvironmentInfo =
        diseaseInfoCVDD.getInfectionEnvironmentQuestion().getInfectionEnvironmentInfo();
    if (infectionEnvironmentSettingValue == InfectionEnvironmentExistingEnum.YES
        && infectionEnvironmentInfo != null) {
      addInfectionEnvironmentSettingGroup(
          infectionEnvironmentSettingAnswer, infectionEnvironmentInfo);
    }
  }

  private void addInfectionEnvironmentSettingGroup(
      QuestionnaireResponseItemAnswerComponent infectionEnvironmentSettingAnswer,
      InfectionEnvironmentInfo infectionEnvironmentInfo)
      throws BadRequestException {
    final QuestionnaireResponseItemComponent infectionEnvironmentSettingGroup =
        infectionEnvironmentSettingAnswer.addItem().setLinkId("infectionEnvironmentSettingGroup");

    addInfectionEnvironmentSettingKind(infectionEnvironmentSettingGroup, infectionEnvironmentInfo);
    addInfectionEnvironmentSettingBegin(infectionEnvironmentSettingGroup, infectionEnvironmentInfo);
    addInfectionEnvironmentSettingEnd(infectionEnvironmentSettingGroup, infectionEnvironmentInfo);
  }

  private void addInfectionEnvironmentSettingKind(
      QuestionnaireResponseItemComponent infectionEnvironmentSettingGroup,
      InfectionEnvironmentInfo infectionEnvironmentInfo)
      throws BadRequestException {
    final String infectionEnvironmentKind = infectionEnvironmentInfo.getInfectionEnvironmentKind();
    if (StringUtils.isNotBlank(infectionEnvironmentKind)) {
      final QuestionnaireResponseItemComponent infectionEnvironmentSettingKind =
          infectionEnvironmentSettingGroup.addItem().setLinkId("infectionEnvironmentSettingKind");
      final QuestionnaireResponseItemAnswerComponent infectionEnvironmentSettingKindAnswer =
          infectionEnvironmentSettingKind.addAnswer();

      Coding infectionEnvironmentKindCoding =
          ConfiguredCodeSystems.getInstance()
              .getInfectionEnvironmentSettingCoding(infectionEnvironmentKind);
      if (infectionEnvironmentKindCoding == null) {
        throw new BadRequestException(
            "invalid infection environment kind: " + infectionEnvironmentKind);
      }
      infectionEnvironmentSettingKindAnswer.setValue(infectionEnvironmentKindCoding);
    }
  }

  private void addInfectionEnvironmentSettingBegin(
      QuestionnaireResponseItemComponent infectionEnvironmentSettingGroup,
      InfectionEnvironmentInfo infectionEnvironmentInfo) {
    final LocalDate infectionEnvironmentStartDate =
        infectionEnvironmentInfo.getInfectionEnvironmentStartDate();
    final QuestionnaireResponseItemComponent infectionEnvironmentSettingBegin =
        infectionEnvironmentSettingGroup.addItem().setLinkId("infectionEnvironmentSettingBegin");
    final QuestionnaireResponseItemAnswerComponent infectionEnvironmentSettingBeginAnswer =
        infectionEnvironmentSettingBegin.addAnswer();
    infectionEnvironmentSettingBeginAnswer.setValue(
        new DateType(DateUtils.createDate(infectionEnvironmentStartDate)));
  }

  private void addInfectionEnvironmentSettingEnd(
      QuestionnaireResponseItemComponent infectionEnvironmentSettingGroup,
      InfectionEnvironmentInfo infectionEnvironmentInfo) {
    final LocalDate infectionEnvironmentEndDate =
        infectionEnvironmentInfo.getInfectionEnvironmentEndDate();
    final QuestionnaireResponseItemComponent infectionEnvironmentSettingEnd =
        infectionEnvironmentSettingGroup.addItem().setLinkId("infectionEnvironmentSettingEnd");
    final QuestionnaireResponseItemAnswerComponent infectionEnvironmentSettingEndAnswer =
        infectionEnvironmentSettingEnd.addAnswer();
    infectionEnvironmentSettingEndAnswer.setValue(
        new DateType(DateUtils.createDate(infectionEnvironmentEndDate)));
  }
}
