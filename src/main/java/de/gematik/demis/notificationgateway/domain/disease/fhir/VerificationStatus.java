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
 * #L%
 */

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.CODE_SYSTEM_CONDITION_VERIFICATION_STATUS;

import de.gematik.demis.notificationgateway.common.dto.DiseaseStatus;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Coding;

/**
 * Verification status of the disease form that contains the diagnosis and is a FHIR condition
 * structure.
 */
@RequiredArgsConstructor
enum VerificationStatus {
  CONFIRMED("confirmed"),

  UNCONFIRMED("unconfirmed"),

  REFUTED("refuted"),

  ENTERED_IN_ERROR("entered-in-error");

  static final String CODE_SYSTEM = CODE_SYSTEM_CONDITION_VERIFICATION_STATUS;

  private final String code;

  /**
   * Get verification status from combined disease notification status
   *
   * @param status combined disease notification status
   * @return verification status
   */
  static VerificationStatus get(DiseaseStatus.StatusEnum status) {
    return switch (status) {
      case FINAL, AMENDED -> CONFIRMED;
      case PRELIMINARY -> UNCONFIRMED;
      case REFUTED -> REFUTED;
      case ERROR -> ENTERED_IN_ERROR;
    };
  }

  /**
   * Create verification status coding
   *
   * @param status disease status
   * @return verification status coding
   */
  static Coding createCoding(DiseaseStatus.StatusEnum status) {
    VerificationStatus verificationStatus = get(status);
    return new Coding().setSystem(CODE_SYSTEM).setCode(verificationStatus.code);
  }
}
