package de.gematik.demis.notificationgateway.domain.disease.fhir;

/*-
 * #%L
 * DEMIS Notification-Gateway
 * %%
 * Copyright (C) 2025 gematik GmbH
 * %%
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
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.disease.DiseaseDataBuilder;
import de.gematik.demis.notificationgateway.common.dto.CodeDisplay;
import de.gematik.demis.notificationgateway.common.dto.Condition;
import de.gematik.demis.notificationgateway.common.dto.DiseaseNotification;
import de.gematik.demis.notificationgateway.common.dto.DiseaseStatus;
import de.gematik.demis.notificationgateway.common.utils.DateUtils;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Service;

/** Factory of the disease block, which is the diagnosis or FHIR condition resource. */
@Service
class Diseases {

  /**
   * Create and add disease block to the bundle
   *
   * @param context context
   */
  void addDisease(DiseaseNotificationContext context) {
    DiseaseNotification notification = context.notification();
    DiseaseDataBuilder condition = new DiseaseDataBuilder();
    setGeneralInfo(notification, context.notifiedPerson(), condition);
    setConditionInfo(notification, condition);
    context.bundle().setDisease(condition.build());
  }

  private void setClinicalStatus(DiseaseNotification notification, DiseaseDataBuilder condition) {
    DiseaseStatus.StatusEnum status = notification.getStatus().getStatus();
    Optional<Coding> clinicalStatus = ClinicalStatus.createCoding(status);
    if (clinicalStatus.isPresent()) {
      condition.setClinicalStatus(clinicalStatus.get());
    } else {
      condition.setClinicalStatus(null);
    }
  }

  private void setDisease(DiseaseNotification notification, DiseaseDataBuilder condition) {
    condition.setDiseaseAndProfileUrl(notification.getStatus().getCategory());
  }

  private void setNotifiedPerson(Patient notifiedPerson, DiseaseDataBuilder condition) {
    condition.setNotifiedPerson(notifiedPerson);
  }

  private void setVerificationStatus(
      DiseaseNotification notification, DiseaseDataBuilder condition) {
    DiseaseStatus.StatusEnum status = notification.getStatus().getStatus();
    condition.setVerificationStatus(VerificationStatus.createCoding(status));
  }

  private void setGeneralInfo(
      DiseaseNotification notification, Patient notifiedPerson, DiseaseDataBuilder condition) {
    condition.setDefaults();
    setDisease(notification, condition);
    setNotifiedPerson(notifiedPerson, condition);
    setClinicalStatus(notification, condition);
    setVerificationStatus(notification, condition);
    setStatusNote(notification, condition);
  }

  private void setStatusNote(DiseaseNotification notification, DiseaseDataBuilder condition) {
    final String note = notification.getStatus().getNote();
    if (StringUtils.isNotBlank(note)) {
      condition.addNote(note);
    }
  }

  private void setConditionInfo(DiseaseNotification notification, DiseaseDataBuilder condition) {
    Condition info = notification.getCondition();
    if (info != null) {
      setOnset(condition, info);
      setRecordedDate(condition, info);
      setNote(condition, info);
      setEvidences(condition, info);
    }
  }

  private void setNote(DiseaseDataBuilder condition, Condition info) {
    final String note = info.getNote();
    if (StringUtils.isNotBlank(note)) {
      condition.addNote(note);
    }
  }

  private void setOnset(
      DiseaseDataBuilder condition,
      de.gematik.demis.notificationgateway.common.dto.Condition info) {
    condition.setOnset(
        new DateTimeType(DateUtils.createDate(info.getOnset()), TemporalPrecisionEnum.DAY));
  }

  private void setRecordedDate(
      DiseaseDataBuilder condition,
      de.gematik.demis.notificationgateway.common.dto.Condition info) {
    condition.setRecordedDate(
        new DateTimeType(DateUtils.createDate(info.getRecordedDate()), TemporalPrecisionEnum.DAY));
  }

  private void setEvidences(
      DiseaseDataBuilder condition,
      de.gematik.demis.notificationgateway.common.dto.Condition info) {
    List<CodeDisplay> evidence = info.getEvidence();
    if (evidence != null) {
      evidence.stream().map(this::toCoding).forEach(condition::addEvidence);
    }
  }

  private Coding toCoding(CodeDisplay evidence) {
    return new Coding(evidence.getSystem(), evidence.getCode(), evidence.getDisplay());
  }
}
