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

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.CODE_SYSTEM_ACT_CODE;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.PROFILE_HOSPITALIZATION;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.STRUCTURE_DEFINITION_HOSPITALIZATION_NOTE;

import de.gematik.demis.notificationgateway.common.dto.DiseaseInfoCommon;
import de.gematik.demis.notificationgateway.common.dto.HospitalizationEncounterInfo;
import de.gematik.demis.notificationgateway.common.dto.HospitalizationQuestion;
import de.gematik.demis.notificationgateway.common.utils.ConfiguredCodeSystems;
import de.gematik.demis.notificationgateway.common.utils.DateUtils;
import de.gematik.demis.notificationgateway.common.utils.ReferenceUtils;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Encounter.EncounterStatus;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.stereotype.Service;

@Service
public class EncounterCreationService {

  public Optional<Encounter> createHospitalizationEncounter(
      DiseaseInfoCommon diseaseInfoCommon, Patient notifiedPerson, Organization notifierFacility) {
    final HospitalizationEncounterInfo hospitalizedEncounterInfo =
        diseaseInfoCommon.getHospitalizationQuestion().getHospitalizedEncounterInfo();

    if (diseaseInfoCommon.getHospitalizationQuestion().getHospitalized()
            != HospitalizationQuestion.HospitalizedEnum.YES
        || hospitalizedEncounterInfo == null) {
      return Optional.empty();
    }

    final Encounter encounter = createEncounter(notifiedPerson, notifierFacility);

    final LocalDate hospitalizationStartDate =
        hospitalizedEncounterInfo.getHospitalizationStartDate();
    final LocalDate hospitalizationEndDate = hospitalizedEncounterInfo.getHospitalizationEndDate();
    final Period period =
        new Period()
            .setStart(DateUtils.createDate(hospitalizationStartDate))
            .setEnd(DateUtils.createDate(hospitalizationEndDate));
    encounter.setPeriod(period);

    final String additionalInfo = hospitalizedEncounterInfo.getAdditionalInfo();
    if (StringUtils.isNotBlank(additionalInfo)) {
      encounter.addExtension(
          STRUCTURE_DEFINITION_HOSPITALIZATION_NOTE, new StringType(additionalInfo));
    }

    return Optional.of(encounter);
  }

  public Optional<Encounter> createIntensiveCareEncounter(
      DiseaseInfoCommon diseaseInfoCommon, Patient notifiedPerson, Organization notifierFacility) {
    final HospitalizationEncounterInfo hospitalizedEncounterInfo =
        diseaseInfoCommon.getHospitalizationQuestion().getHospitalizedEncounterInfo();

    if (diseaseInfoCommon.getHospitalizationQuestion().getHospitalized()
            != HospitalizationQuestion.HospitalizedEnum.YES
        || hospitalizedEncounterInfo == null
        || !hospitalizedEncounterInfo.getIntensiveCare()) {
      return Optional.empty();
    }

    final Encounter encounter = createEncounter(notifiedPerson, notifierFacility);

    encounter.setServiceType(
        new CodeableConcept(
            ConfiguredCodeSystems.getInstance().getHospitalizationServiceTypeCoding("3600")));

    final LocalDate intensiveCareStartDate = hospitalizedEncounterInfo.getIntensiveCareStartDate();
    final Period period = new Period().setStart(DateUtils.createDate(intensiveCareStartDate));
    encounter.setPeriod(period);

    return Optional.of(encounter);
  }

  private Encounter createEncounter(Patient notifiedPerson, Organization notifierFacility) {
    final Encounter encounter = new Encounter();
    encounter.setId(UUID.randomUUID().toString());
    encounter.setMeta(new Meta().addProfile(PROFILE_HOSPITALIZATION));

    encounter.setStatus(EncounterStatus.INPROGRESS);

    final Coding classCoding =
        new Coding()
            .setSystem(CODE_SYSTEM_ACT_CODE)
            .setCode("IMP")
            .setDisplay("inpatient encounter");
    encounter.setClass_(classCoding);

    encounter.setSubject(ReferenceUtils.createReference(notifiedPerson));
    encounter.setServiceProvider(ReferenceUtils.createReference(notifierFacility));
    return encounter;
  }
}
