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

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.PROFILE_DISEASE_INFORMATION_COMMON;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.QUESTIONAIRE_DISEASE_QUESTIONS_COMMON;

import de.gematik.demis.notificationgateway.common.dto.DeathQuestion.DeadEnum;
import de.gematik.demis.notificationgateway.common.dto.DiseaseInfoCommon;
import de.gematik.demis.notificationgateway.common.dto.DiseaseInfoCommon.MilitaryEnum;
import de.gematik.demis.notificationgateway.common.dto.DiseaseInfoCommon.OrganDonorEnum;
import de.gematik.demis.notificationgateway.common.dto.ExposurePlaceInfo;
import de.gematik.demis.notificationgateway.common.dto.ExposurePlaceQuestion.ExposurePlaceKnownEnum;
import de.gematik.demis.notificationgateway.common.dto.HospitalizationQuestion;
import de.gematik.demis.notificationgateway.common.dto.InfectionProtectionFacilityInfo;
import de.gematik.demis.notificationgateway.common.dto.InfectionProtectionFacilityInfo.RoleEnum;
import de.gematik.demis.notificationgateway.common.dto.InfectionProtectionFacilityQuestion.InfectionProtectionFacilityExistingEnum;
import de.gematik.demis.notificationgateway.common.dto.LabQuestion;
import de.gematik.demis.notificationgateway.common.exceptions.BadRequestException;
import de.gematik.demis.notificationgateway.common.utils.ConfiguredCodeSystems;
import de.gematik.demis.notificationgateway.common.utils.DateUtils;
import de.gematik.demis.notificationgateway.common.utils.ReferenceUtils;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseStatus;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Service
public class DiseaseInformationCommonCreationService {

  public QuestionnaireResponse createDiseaseInformationCommon(
      DiseaseInfoCommon diseaseInfoCommon,
      Patient notifiedPerson,
      Optional<Encounter> hospitalizationEncounter,
      Optional<Encounter> intensiveCareEncounter,
      Optional<Organization> lab,
      Organization infectProtectFacility)
      throws BadRequestException {
    final QuestionnaireResponse questionnaireResponse = new QuestionnaireResponse();
    questionnaireResponse.setId(UUID.randomUUID().toString());
    questionnaireResponse.setMeta(new Meta().addProfile(PROFILE_DISEASE_INFORMATION_COMMON));

    questionnaireResponse.setQuestionnaire(QUESTIONAIRE_DISEASE_QUESTIONS_COMMON);
    questionnaireResponse.setStatus(QuestionnaireResponseStatus.COMPLETED);
    questionnaireResponse.setSubject(ReferenceUtils.createReference(notifiedPerson));
    addItems(
        questionnaireResponse,
        diseaseInfoCommon,
        hospitalizationEncounter,
        intensiveCareEncounter,
        lab,
        infectProtectFacility);

    return questionnaireResponse;
  }

  private void addItems(
      QuestionnaireResponse questionnaireResponse,
      DiseaseInfoCommon diseaseInfoCommon,
      Optional<Encounter> hospitalizationEncounter,
      Optional<Encounter> intensiveCareEncounter,
      Optional<Organization> lab,
      @Nullable Organization infectProtectFacility)
      throws BadRequestException {
    addDeathInformation(questionnaireResponse, diseaseInfoCommon);
    addMilitaryAffiliation(questionnaireResponse, diseaseInfoCommon);
    addLabSpecimenTaken(questionnaireResponse, diseaseInfoCommon, lab);
    addHospitalizionInformation(
        questionnaireResponse, diseaseInfoCommon, hospitalizationEncounter, intensiveCareEncounter);
    addInfectProtectFacility(questionnaireResponse, diseaseInfoCommon, infectProtectFacility);
    addPlaceExposure(questionnaireResponse, diseaseInfoCommon);
    addOrganDonation(questionnaireResponse, diseaseInfoCommon);
    addAdditionalInformation(questionnaireResponse, diseaseInfoCommon);
  }

  private void addAdditionalInformation(
      QuestionnaireResponse questionnaireResponse, DiseaseInfoCommon diseaseInfoCommon) {
    final String additionalInfo = diseaseInfoCommon.getAdditionalInfo();
    if (StringUtils.isNotBlank(additionalInfo)) {
      final QuestionnaireResponseItemComponent additionalInformation =
          questionnaireResponse.addItem().setLinkId("additionalInformation");
      additionalInformation.addAnswer().setValue(new StringType(additionalInfo));
    }
  }

  private void addOrganDonation(
      QuestionnaireResponse questionnaireResponse, DiseaseInfoCommon diseaseInfoCommon) {
    final QuestionnaireResponseItemComponent organDonation =
        questionnaireResponse.addItem().setLinkId("organDonation");
    final QuestionnaireResponseItemAnswerComponent organDonationAnswer = organDonation.addAnswer();

    final OrganDonorEnum organDonor = diseaseInfoCommon.getOrganDonor();
    Coding organDonationCoding;
    switch (organDonor) {
      case YES:
        organDonationCoding = ConfiguredCodeSystems.getInstance().getYesOrNoCoding("yes");
        break;
      case NO:
        organDonationCoding = ConfiguredCodeSystems.getInstance().getYesOrNoCoding("no");
        break;
      case INDETERMINATE:
        organDonationCoding = ConfiguredCodeSystems.getInstance().getNullFlavor("ASKU");
        break;
      case UNKNOWN:
        organDonationCoding = ConfiguredCodeSystems.getInstance().getNullFlavor("NASK");
        break;
      default:
        throw new IllegalStateException("unkwon organ donation value: " + organDonation);
    }

    organDonationAnswer.setValue(organDonationCoding);
  }

  private void addPlaceExposure(
      QuestionnaireResponse questionnaireResponse, DiseaseInfoCommon diseaseInfoCommon)
      throws BadRequestException {
    final QuestionnaireResponseItemComponent placeExposure =
        questionnaireResponse.addItem().setLinkId("placeExposure");
    final QuestionnaireResponseItemAnswerComponent placeExposureAnswer = placeExposure.addAnswer();

    final ExposurePlaceKnownEnum placeExposureKnown =
        diseaseInfoCommon.getExposurePlaceQuestion().getExposurePlaceKnown();
    Coding placeExposureCoding;
    switch (placeExposureKnown) {
      case YES:
        placeExposureCoding = ConfiguredCodeSystems.getInstance().getYesOrNoCoding("yes");
        break;
      case NO:
        placeExposureCoding = ConfiguredCodeSystems.getInstance().getYesOrNoCoding("no");
        break;
      case INDETERMINATE:
        placeExposureCoding = ConfiguredCodeSystems.getInstance().getNullFlavor("ASKU");
        break;
      case UNKNOWN:
        placeExposureCoding = ConfiguredCodeSystems.getInstance().getNullFlavor("NASK");
        break;
      default:
        throw new IllegalStateException("unknown place exposure value: " + placeExposureKnown);
    }
    placeExposureAnswer.setValue(placeExposureCoding);

    final ExposurePlaceInfo placeExposureInfo =
        diseaseInfoCommon.getExposurePlaceQuestion().getExposurePlaceInfo();
    if (placeExposureKnown == ExposurePlaceKnownEnum.YES && placeExposureInfo != null) {
      addPlaceExposureGroup(placeExposureAnswer, placeExposureInfo);
    }
  }

  private void addPlaceExposureGroup(
      QuestionnaireResponseItemAnswerComponent placeExposureAnswer,
      ExposurePlaceInfo placeExposureInfo)
      throws BadRequestException {
    final QuestionnaireResponseItemComponent placeExposureGroupItem =
        placeExposureAnswer.addItem().setLinkId("placeExposureGroup");

    final LocalDate exposureStartDate = placeExposureInfo.getExposureStartDate();
    if (exposureStartDate != null) {
      final QuestionnaireResponseItemComponent placeExposureBegin =
          placeExposureGroupItem.addItem().setLinkId("placeExposureBegin");
      placeExposureBegin
          .addAnswer()
          .setValue(new DateType(DateUtils.createDate(exposureStartDate)));
    }

    final LocalDate exposureEndDate = placeExposureInfo.getExposureEndDate();
    if (exposureEndDate != null) {
      final QuestionnaireResponseItemComponent placeExposureEnd =
          placeExposureGroupItem.addItem().setLinkId("placeExposureEnd");
      placeExposureEnd.addAnswer().setValue(new DateType(DateUtils.createDate(exposureEndDate)));
    }

    final String region = placeExposureInfo.getRegion();
    if (StringUtils.isNotBlank(region)) {
      Coding geographicRegionCoding =
          ConfiguredCodeSystems.getInstance().getGeographicRegionCoding(region);
      if (geographicRegionCoding == null) {
        final Coding nullFlavor = ConfiguredCodeSystems.getInstance().getNullFlavor(region);
        if (nullFlavor == null) {
          throw new BadRequestException("invalid geographic region for place exposure: " + region);
        }
        geographicRegionCoding = nullFlavor;
      }

      final QuestionnaireResponseItemComponent placeExposureRegion =
          placeExposureGroupItem.addItem().setLinkId("placeExposureRegion");
      placeExposureRegion.addAnswer().setValue(geographicRegionCoding);
    } else {
      throw new BadRequestException("geographic region for place exposure is missing!");
    }

    final String hint = placeExposureInfo.getHint();
    if (StringUtils.isNotBlank(hint)) {
      final QuestionnaireResponseItemComponent placeExposureHint =
          placeExposureGroupItem.addItem().setLinkId("placeExposureHint");
      placeExposureHint.addAnswer().setValue(new StringType(hint));
    }
  }

  private void addInfectProtectFacility(
      QuestionnaireResponse questionnaireResponse,
      DiseaseInfoCommon diseaseInfoCommon,
      @Nullable Organization infectProtectFacility) {
    final QuestionnaireResponseItemComponent infectProtectFacilityItem =
        questionnaireResponse.addItem().setLinkId("infectProtectFacility");
    final QuestionnaireResponseItemAnswerComponent infectProtectFacilityAnswer =
        infectProtectFacilityItem.addAnswer();

    // Properties getInfectionProtectionFacilityQuestion and getInfectionProtectionFacilityExisting
    // are @NotNull
    final InfectionProtectionFacilityExistingEnum infectProtectFacilityExisting =
        diseaseInfoCommon
            .getInfectionProtectionFacilityQuestion()
            .getInfectionProtectionFacilityExisting();
    Coding infectProtectFacilityCoding;
    switch (infectProtectFacilityExisting) {
      case YES:
        infectProtectFacilityCoding = ConfiguredCodeSystems.getInstance().getYesOrNoCoding("yes");
        break;
      case NO:
        infectProtectFacilityCoding = ConfiguredCodeSystems.getInstance().getYesOrNoCoding("no");
        break;
      case INDETERMINATE:
        infectProtectFacilityCoding = ConfiguredCodeSystems.getInstance().getNullFlavor("ASKU");
        break;
      case UNKNOWN:
        infectProtectFacilityCoding = ConfiguredCodeSystems.getInstance().getNullFlavor("NASK");
        break;
      default:
        throw new IllegalStateException(
            "unknown infect protect facility value: " + infectProtectFacilityExisting);
    }

    infectProtectFacilityAnswer.setValue(infectProtectFacilityCoding);

    final InfectionProtectionFacilityInfo infectionProtectionFacilityInfo =
        diseaseInfoCommon
            .getInfectionProtectionFacilityQuestion()
            .getInfectionProtectionFacilityInfo();
    if (infectProtectFacilityExisting == InfectionProtectionFacilityExistingEnum.YES
        && infectionProtectionFacilityInfo != null) {
      addInfectProtectFacilityGroup(
          infectProtectFacilityAnswer, infectionProtectionFacilityInfo, infectProtectFacility);
    }
  }

  private void addInfectProtectFacilityGroup(
      QuestionnaireResponseItemAnswerComponent infectProtectFacilityAnswer,
      InfectionProtectionFacilityInfo infectionProtectionFacilityInfo,
      @Nullable Organization infectProtectFacility) {
    final QuestionnaireResponseItemComponent infectProtectFacilityGroup =
        infectProtectFacilityAnswer.addItem().setLinkId("infectProtectFacilityGroup");

    final LocalDate startDate = infectionProtectionFacilityInfo.getStartDate();
    if (startDate != null) {
      final QuestionnaireResponseItemComponent infectProtectFacilityBegin =
          infectProtectFacilityGroup.addItem().setLinkId("infectProtectFacilityBegin");
      infectProtectFacilityBegin
          .addAnswer()
          .setValue(new DateType(DateUtils.createDate(startDate)));
    }

    final LocalDate endDate = infectionProtectionFacilityInfo.getEndDate();
    if (endDate != null) {
      final QuestionnaireResponseItemComponent infectProtectFacilityEnd =
          infectProtectFacilityGroup.addItem().setLinkId("infectProtectFacilityEnd");
      infectProtectFacilityEnd.addAnswer().setValue(new DateType(DateUtils.createDate(endDate)));
    }

    addInfectProtectFacilityRole(infectionProtectionFacilityInfo, infectProtectFacilityGroup);

    if (infectProtectFacility != null) {
      final QuestionnaireResponseItemComponent infectProtectFacilityOrganization =
          infectProtectFacilityGroup.addItem().setLinkId("infectProtectFacilityOrganization");
      infectProtectFacilityOrganization
          .addAnswer()
          .setValue(ReferenceUtils.createReference(infectProtectFacility));
    }
  }

  private void addInfectProtectFacilityRole(
      InfectionProtectionFacilityInfo infectionProtectionFacilityInfo,
      QuestionnaireResponseItemComponent infectProtectFacilityGroup) {
    final RoleEnum role = infectionProtectionFacilityInfo.getRole();
    Coding roleCoding;
    switch (role) {
      case EMPLOYMENT:
        roleCoding =
            ConfiguredCodeSystems.getInstance().getOrganizationAssociationCoding("employment");
        break;
      case CARE:
        roleCoding = ConfiguredCodeSystems.getInstance().getOrganizationAssociationCoding("care");
        break;
      case ACCOMMODATION:
        roleCoding =
            ConfiguredCodeSystems.getInstance().getOrganizationAssociationCoding("accommodation");
        break;
      default:
        throw new IllegalStateException("unknown infect protect facility role: " + role);
    }
    final QuestionnaireResponseItemComponent infectProtectFacilityRole =
        infectProtectFacilityGroup.addItem().setLinkId("infectProtectFacilityRole");
    infectProtectFacilityRole.addAnswer().setValue(roleCoding);
  }

  private void addHospitalizionInformation(
      QuestionnaireResponse questionnaireResponse,
      DiseaseInfoCommon diseaseInfoCommon,
      Optional<Encounter> hospitalizationEncounter,
      Optional<Encounter> intensiveCareEncounter) {
    final QuestionnaireResponseItemComponent hospitalizedItem =
        questionnaireResponse.addItem().setLinkId("hospitalized");
    final QuestionnaireResponseItemAnswerComponent hospitalizedAnswer =
        hospitalizedItem.addAnswer();

    final HospitalizationQuestion.HospitalizedEnum hospitalized =
        diseaseInfoCommon.getHospitalizationQuestion().getHospitalized();
    Coding hospitalizedCoding;
    switch (hospitalized) {
      case YES:
        hospitalizedCoding = ConfiguredCodeSystems.getInstance().getYesOrNoCoding("yes");
        break;
      case NO:
        hospitalizedCoding = ConfiguredCodeSystems.getInstance().getYesOrNoCoding("no");
        break;
      case INDETERMINATE:
        hospitalizedCoding = ConfiguredCodeSystems.getInstance().getNullFlavor("ASKU");
        break;
      case UNKNOWN:
        hospitalizedCoding = ConfiguredCodeSystems.getInstance().getNullFlavor("NASK");
        break;
      default:
        throw new IllegalStateException("unkown hospitalized value: " + hospitalized);
    }
    hospitalizedAnswer.setValue(hospitalizedCoding);

    if (hospitalized == HospitalizationQuestion.HospitalizedEnum.YES) {
      hospitalizationEncounter.ifPresent(
          encounter -> addHospitalizedEncounter(hospitalizedAnswer, encounter));

      intensiveCareEncounter.ifPresent(
          encounter -> addHospitalizedEncounter(hospitalizedAnswer, encounter));
    }
  }

  private void addHospitalizedEncounter(
      QuestionnaireResponseItemAnswerComponent hospitalizedAnswer,
      Encounter hospitalizationEncounter) {
    QuestionnaireResponseItemComponent hospitalizedGroupItem =
        hospitalizedAnswer.addItem().setLinkId("hospitalizedGroup");
    final QuestionnaireResponseItemComponent hospitalizedEncounterItem =
        hospitalizedGroupItem.addItem().setLinkId("hospitalizedEncounter");
    final QuestionnaireResponseItemAnswerComponent hospitalizationEncounterAnswer =
        hospitalizedEncounterItem.addAnswer();
    hospitalizationEncounterAnswer.setValue(
        ReferenceUtils.createReference(hospitalizationEncounter));
  }

  private void addLabSpecimenTaken(
      QuestionnaireResponse questionnaireResponse,
      DiseaseInfoCommon diseaseInfoCommon,
      Optional<Organization> lab) {
    final QuestionnaireResponseItemComponent labSpecimenTakenItem =
        questionnaireResponse.addItem().setLinkId("labSpecimenTaken");
    final QuestionnaireResponseItemAnswerComponent labSpecimenTakenAnswer =
        labSpecimenTakenItem.addAnswer();

    Coding labSpecimenTakenCoding;
    final LabQuestion.LabAssignedEnum labAssigned =
        diseaseInfoCommon.getLabQuestion().getLabAssigned();
    switch (labAssigned) {
      case YES:
        labSpecimenTakenCoding = ConfiguredCodeSystems.getInstance().getYesOrNoCoding("yes");
        break;
      case NO:
        labSpecimenTakenCoding = ConfiguredCodeSystems.getInstance().getYesOrNoCoding("no");
        break;
      case INDETERMINATE:
        labSpecimenTakenCoding = ConfiguredCodeSystems.getInstance().getNullFlavor("ASKU");
        break;
      case UNKNOWN:
        labSpecimenTakenCoding = ConfiguredCodeSystems.getInstance().getNullFlavor("NASK");
        break;
      default:
        throw new IllegalStateException("unkown lab assigned enum: " + labAssigned);
    }
    labSpecimenTakenAnswer.setValue(labSpecimenTakenCoding);

    if (labAssigned == LabQuestion.LabAssignedEnum.YES && lab.isPresent()) {
      final QuestionnaireResponseItemComponent labSpecimenLab =
          labSpecimenTakenAnswer.addItem().setLinkId("labSpecimenLab");
      final QuestionnaireResponseItemAnswerComponent hospitalizationEncounterAnswer =
          labSpecimenLab.addAnswer();
      hospitalizationEncounterAnswer.setValue(ReferenceUtils.createReference(lab.get()));
    }
  }

  private void addMilitaryAffiliation(
      QuestionnaireResponse questionnaireResponse, DiseaseInfoCommon diseaseInfoCommon) {
    final QuestionnaireResponseItemComponent militaryAffiliationItem =
        questionnaireResponse.addItem().setLinkId("militaryAffiliation");
    final QuestionnaireResponseItemAnswerComponent militiaryAffiliationAnswer =
        militaryAffiliationItem.addAnswer();

    final MilitaryEnum military = diseaseInfoCommon.getMilitary();
    Coding militaryAffiliationCoding;
    switch (military) {
      case INDETERMINATE:
        militaryAffiliationCoding = ConfiguredCodeSystems.getInstance().getNullFlavor("ASKU");
        break;
      case UNKNOWN:
        militaryAffiliationCoding = ConfiguredCodeSystems.getInstance().getNullFlavor("NASK");
        break;
      case SOLDIER:
        militaryAffiliationCoding =
            ConfiguredCodeSystems.getInstance().getMilitaryAffiliationCoding("memberOfBundeswehr");
        break;
      case CIVILIAN_IN_MILITARY_FACILITY:
        militaryAffiliationCoding =
            ConfiguredCodeSystems.getInstance()
                .getMilitaryAffiliationCoding("civilPersonActiveInBundeswehr");
        break;
      case UNRELATED_TO_MILITARY:
        militaryAffiliationCoding =
            ConfiguredCodeSystems.getInstance()
                .getMilitaryAffiliationCoding("noReferenceToBundeswehr");
        break;
      default:
        throw new IllegalStateException("unkown military value: " + military);
    }

    militiaryAffiliationAnswer.setValue(militaryAffiliationCoding);
  }

  private void addDeathInformation(
      QuestionnaireResponse questionnaireResponse, DiseaseInfoCommon diseaseInfoCommon) {
    final DeadEnum dead = diseaseInfoCommon.getDeathQuestion().getDead();
    final QuestionnaireResponseItemAnswerComponent deadItemAnswer =
        addIsDead(questionnaireResponse, dead);

    final LocalDate deathDate = diseaseInfoCommon.getDeathQuestion().getDeathDate();
    if (dead == DeadEnum.YES && deathDate != null) {
      final QuestionnaireResponseItemComponent deathDateItem =
          deadItemAnswer.addItem().setLinkId("deathDate");
      final QuestionnaireResponseItemAnswerComponent deathDateAnswer = deathDateItem.addAnswer();
      deathDateAnswer.setValue(new DateType(DateUtils.createDate(deathDate)));
    }
  }

  private QuestionnaireResponseItemAnswerComponent addIsDead(
      QuestionnaireResponse questionnaireResponse, DeadEnum dead) {
    final QuestionnaireResponseItemComponent deadItem =
        questionnaireResponse.addItem().setLinkId("isDead");
    final QuestionnaireResponseItemAnswerComponent deadItemAnswer = deadItem.addAnswer();

    Coding deadCoding;
    switch (dead) {
      case YES:
        deadCoding = ConfiguredCodeSystems.getInstance().getYesOrNoCoding("yes");
        break;
      case NO:
        deadCoding = ConfiguredCodeSystems.getInstance().getYesOrNoCoding("no");
        break;
      case INDETERMINATE:
        deadCoding = ConfiguredCodeSystems.getInstance().getNullFlavor("ASKU");
        break;
      case UNKNOWN:
        deadCoding = ConfiguredCodeSystems.getInstance().getNullFlavor("NASK");
        break;
      default:
        throw new IllegalStateException("unkown dead enum: " + dead);
    }

    deadItemAnswer.setValue(deadCoding);
    return deadItemAnswer;
  }
}
