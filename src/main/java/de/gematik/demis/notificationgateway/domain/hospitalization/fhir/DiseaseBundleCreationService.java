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

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.PROFILE_NOTIFICATION_BUNDLE_DISEASE;

import de.gematik.demis.notificationgateway.common.dto.ConditionInfo;
import de.gematik.demis.notificationgateway.common.dto.DiseaseInfoCVDD;
import de.gematik.demis.notificationgateway.common.dto.DiseaseInfoCommon;
import de.gematik.demis.notificationgateway.common.dto.Hospitalization;
import de.gematik.demis.notificationgateway.common.dto.HospitalizationQuestion;
import de.gematik.demis.notificationgateway.common.dto.InfectionProtectionFacilityQuestion;
import de.gematik.demis.notificationgateway.common.dto.InfectionProtectionFacilityQuestion.InfectionProtectionFacilityExistingEnum;
import de.gematik.demis.notificationgateway.common.dto.LabQuestion;
import de.gematik.demis.notificationgateway.common.dto.VaccinationQuestion;
import de.gematik.demis.notificationgateway.common.exceptions.BadRequestException;
import de.gematik.demis.notificationgateway.common.services.fhir.BundleCreationService;
import de.gematik.demis.notificationgateway.common.services.fhir.NotifiedPersonCreationService;
import de.gematik.demis.notificationgateway.common.services.fhir.OrganizationCreationService;
import de.gematik.demis.notificationgateway.common.services.fhir.PractitionerRoleCreationService;
import de.gematik.demis.notificationgateway.common.utils.ReferenceUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class DiseaseBundleCreationService extends BundleCreationService {

  private final NotifiedPersonCreationService notifiedPersonCreationService;
  private final OrganizationCreationService organizationCreationService;
  private final NotificationDiseaseCreationService notificationDiseaseCreationService;
  private final PractitionerRoleCreationService practitionerRoleCreationService;
  private final DiseaseCVDDCreationService diseaseCVDDCreationService;
  private final EncounterCreationService encounterCreationService;
  private final DiseaseInformationCommonCreationService diseaseInformationCommonCreationService;
  private final DiseaseInformationCVDDCreationService diseaseInformationCVDDCreationService;
  private final ImmunizationCreator immunizationCreator;

  /**
   * Creates a disease bundle based on the given hospitalization content.
   *
   * @param content the hospitalization content
   * @return bundle for hospitalization notification
   * @throws BadRequestException in case hospitalization content is invalid
   */
  public Bundle createDiseaseBundle(Hospitalization content) throws BadRequestException {
    final Bundle bundle = createBundle(PROFILE_NOTIFICATION_BUNDLE_DISEASE);

    final DiseaseInfoCommon diseaseInfoCommon = content.getDisease().getDiseaseInfoCommon();

    Patient notifiedPerson;
    Optional<Organization> notifiedPersonFacilityOptional = Optional.empty();
    Optional<Encounter> hospitalizationEncounterOptional = Optional.empty();
    Optional<Encounter> intensiveCareEncounterOptional = Optional.empty();
    if (diseaseInfoCommon.getHospitalizationQuestion().getHospitalized()
        == HospitalizationQuestion.HospitalizedEnum.YES) {
      notifiedPersonFacilityOptional =
          Optional.of(
              organizationCreationService.createNotifiedPersonFacility(
                  content.getNotifierFacility()));

      notifiedPerson =
          notifiedPersonCreationService.createNotifiedPersonHospitalizedInFacility(
              content.getNotifiedPerson(), notifiedPersonFacilityOptional.get());

      hospitalizationEncounterOptional =
          encounterCreationService.createHospitalizationEncounter(
              diseaseInfoCommon, notifiedPerson, notifiedPersonFacilityOptional.get());

      intensiveCareEncounterOptional =
          encounterCreationService.createIntensiveCareEncounter(
              diseaseInfoCommon, notifiedPerson, notifiedPersonFacilityOptional.get());
    } else {
      notifiedPerson =
          notifiedPersonCreationService.createNotifiedPerson(content.getNotifiedPerson());
    }

    final Organization notifierFacility =
        organizationCreationService.createHospitalNotifierFacility(content.getNotifierFacility());
    final PractitionerRole notifierRole =
        practitionerRoleCreationService.createNotifierRole(notifierFacility);
    final ConditionInfo conditionInfo = content.getDisease().getConditionInfo();

    Condition condition =
        diseaseCVDDCreationService.createDiseaseCVDD(conditionInfo, notifiedPerson);

    Optional<Organization> labOptional = Optional.empty();
    if (diseaseInfoCommon.getLabQuestion().getLabAssigned() == LabQuestion.LabAssignedEnum.YES) {
      labOptional =
          organizationCreationService.createLab(diseaseInfoCommon.getLabQuestion().getLabInfo());
    }

    Optional<Organization> infectProtectFacilityOptional = Optional.empty();
    InfectionProtectionFacilityQuestion infectionProtectionFacilityQuestion =
        diseaseInfoCommon.getInfectionProtectionFacilityQuestion();
    if (infectionProtectionFacilityQuestion.getInfectionProtectionFacilityExisting()
        == InfectionProtectionFacilityExistingEnum.YES) {
      infectProtectFacilityOptional =
          organizationCreationService.createInfectProtectFacility(
              infectionProtectionFacilityQuestion.getInfectionProtectionFacilityInfo());
    }

    QuestionnaireResponse diseaseInformationCommon;
    if (infectProtectFacilityOptional.isEmpty()) {
      diseaseInformationCommon =
          diseaseInformationCommonCreationService.createDiseaseInformationCommon(
              diseaseInfoCommon,
              notifiedPerson,
              hospitalizationEncounterOptional,
              intensiveCareEncounterOptional,
              labOptional,
              notifierFacility);
    } else {
      diseaseInformationCommon =
          diseaseInformationCommonCreationService.createDiseaseInformationCommon(
              diseaseInfoCommon,
              notifiedPerson,
              hospitalizationEncounterOptional,
              intensiveCareEncounterOptional,
              labOptional,
              infectProtectFacilityOptional.get());
    }

    final DiseaseInfoCVDD diseaseInfoCVDD = content.getDisease().getDiseaseInfoCVDD();

    List<Immunization> immunizations = new ArrayList<>();
    final VaccinationQuestion.ImmunizationStatusEnum immunizationStatus =
        diseaseInfoCVDD.getVaccinationQuestion().getImmunizationStatus();
    if ((VaccinationQuestion.ImmunizationStatusEnum.YES.equals(immunizationStatus))) {
      final Reference personReference = ReferenceUtils.createReference(notifiedPerson);
      immunizations =
          immunizationCreator.create(
              diseaseInfoCVDD.getVaccinationQuestion().getVaccinations(), personReference);
    }
    final QuestionnaireResponse diseaseInformationCVDD =
        diseaseInformationCVDDCreationService.createDiseaseInformationCVDD(
            diseaseInfoCVDD, notifiedPerson, immunizations);

    final Composition notificationDisease =
        notificationDiseaseCreationService.createNotificationDisease(
            notifiedPerson,
            notifierRole,
            condition,
            diseaseInformationCommon,
            diseaseInformationCVDD);

    addEntry(bundle, notificationDisease);
    notifiedPersonFacilityOptional.ifPresent(
        notifiedPersonFacility -> addEntry(bundle, notifiedPersonFacility));

    addEntry(bundle, notifiedPerson);
    addEntry(bundle, condition);
    addEntry(bundle, notifierFacility);
    addEntry(bundle, notifierRole);
    hospitalizationEncounterOptional.ifPresent(
        hospitalizationEncounter -> addEntry(bundle, hospitalizationEncounter));
    intensiveCareEncounterOptional.ifPresent(
        intensiveCareEncounter -> addEntry(bundle, intensiveCareEncounter));
    addEntry(bundle, diseaseInformationCommon);
    labOptional.ifPresent(lab -> addEntry(bundle, lab));
    infectProtectFacilityOptional.ifPresent(
        infectProtectFacility -> addEntry(bundle, infectProtectFacility));
    addEntry(bundle, diseaseInformationCVDD);
    immunizations.forEach(immunization -> addEntry(bundle, immunization));

    return bundle;
  }
}
