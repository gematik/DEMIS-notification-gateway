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

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.CODE_SYSTEM_CONDITION_VERIFICATION_STATUS;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.PROFILE_DISEASE_CVDD;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import de.gematik.demis.notificationgateway.common.dto.ConditionInfo;
import de.gematik.demis.notificationgateway.common.dto.Symptom;
import de.gematik.demis.notificationgateway.common.dto.SymptomQuestion;
import de.gematik.demis.notificationgateway.common.enums.SymptomCoding;
import de.gematik.demis.notificationgateway.common.exceptions.BadRequestException;
import de.gematik.demis.notificationgateway.common.utils.ConfiguredCodeSystems;
import de.gematik.demis.notificationgateway.common.utils.DateUtils;
import de.gematik.demis.notificationgateway.common.utils.ReferenceUtils;
import java.time.LocalDate;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Condition.ConditionEvidenceComponent;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Service
public class DiseaseCVDDCreationService {

  public Condition createDiseaseCVDD(@Nullable ConditionInfo conditionInfo, Patient notifiedPerson)
      throws BadRequestException {
    final Condition condition = new Condition();
    condition.setId(UUID.randomUUID().toString());
    condition.setMeta(new Meta().addProfile(PROFILE_DISEASE_CVDD));

    setVerificationStatus(condition);
    condition.setCode(
        new CodeableConcept(
            ConfiguredCodeSystems.getInstance().getNotificationDiseaseCategoryCoding("cvdd")));
    condition.setSubject(ReferenceUtils.createReference(notifiedPerson));
    if (conditionInfo == null) {
      return condition;
    }

    final LocalDate diagnosisDate = conditionInfo.getDiagnosisDate();
    if (diagnosisDate != null) {
      condition.setRecordedDateElement(
          new DateTimeType(DateUtils.createDate(diagnosisDate), TemporalPrecisionEnum.DAY));
    }

    final LocalDate diseaseStart = conditionInfo.getDiseaseStart();
    if (diseaseStart != null) {
      condition.setOnset(
          new DateTimeType(DateUtils.createDate(diseaseStart), TemporalPrecisionEnum.DAY));
    }

    addSymptoms(condition, conditionInfo.getSymptomQuestion());

    final String note = conditionInfo.getNote();
    if (StringUtils.isNotBlank(note)) {
      condition.addNote().setText(note);
    }

    return condition;
  }

  private void addSymptoms(Condition condition, SymptomQuestion symptomQuestion)
      throws BadRequestException {
    if (symptomQuestion == null
        || SymptomQuestion.SymptomStatusEnum.NO.equals(symptomQuestion.getSymptomStatus())
        || symptomQuestion.getSymptoms() == null) {
      return;
    }
    for (Symptom symptom : symptomQuestion.getSymptoms()) {
      final ConditionEvidenceComponent evidence = new ConditionEvidenceComponent();
      final Coding coding = SymptomCoding.bySymptom(symptom).getCoding();
      evidence.addCode(new CodeableConcept(coding));
      condition.addEvidence(evidence);
    }
  }

  private void setVerificationStatus(Condition condition) {
    final Coding verificationStatusCoding =
        new Coding().setSystem(CODE_SYSTEM_CONDITION_VERIFICATION_STATUS).setCode("confirmed");
    condition.setVerificationStatus(new CodeableConcept(verificationStatusCoding));
  }
}
