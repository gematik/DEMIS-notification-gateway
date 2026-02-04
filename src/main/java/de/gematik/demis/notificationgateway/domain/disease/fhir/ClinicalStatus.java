package de.gematik.demis.notificationgateway.domain.disease.fhir;

/*-
 * #%L
 * DEMIS Notification-Gateway
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
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
 * For additional notes and disclaimer from gematik and in case of changes by gematik,
 * find details in the "Readme" file.
 * #L%
 */

import de.gematik.demis.notificationgateway.common.dto.DiseaseStatus;
import java.util.Optional;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.codesystems.ConditionClinical;

/** Current clinical status of the notified person. */
final class ClinicalStatus {

  static final String CODE_SYSTEM = "http://terminology.hl7.org/CodeSystem/condition-clinical";

  private ClinicalStatus() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  /**
   * Get clinical status from combined disease notification status
   *
   * @param status combined disease notification status
   * @return clinical status FHIR enum or <code>null</code>
   */
  static ConditionClinical get(DiseaseStatus.StatusEnum status) {
    return switch (status) {
      case FINAL, PRELIMINARY, AMENDED -> ConditionClinical.ACTIVE;
      case REFUTED -> ConditionClinical.INACTIVE;
      case ERROR -> null;
    };
  }

  /**
   * Create clinical status coding
   *
   * @param status disease status
   * @return clinical status coding
   */
  static Optional<Coding> createCoding(DiseaseStatus.StatusEnum status) {
    ConditionClinical clinicalStatus = get(status);
    if (clinicalStatus == null) {
      return Optional.empty();
    }
    return Optional.of(new Coding().setSystem(CODE_SYSTEM).setCode(clinicalStatus.toCode()));
  }
}
