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

import static org.junit.jupiter.api.Assertions.*;

import de.gematik.demis.notificationgateway.common.dto.DiseaseStatus;
import org.hl7.fhir.r4.model.Coding;
import org.junit.jupiter.api.Test;

class VerificationStatusTest {

  @Test
  void testGet() {
    assertEquals(
        VerificationStatus.CONFIRMED, VerificationStatus.get(DiseaseStatus.StatusEnum.FINAL));
    assertEquals(
        VerificationStatus.CONFIRMED, VerificationStatus.get(DiseaseStatus.StatusEnum.AMENDED));
    assertEquals(
        VerificationStatus.UNCONFIRMED,
        VerificationStatus.get(DiseaseStatus.StatusEnum.PRELIMINARY));
    assertEquals(
        VerificationStatus.REFUTED, VerificationStatus.get(DiseaseStatus.StatusEnum.REFUTED));
    assertEquals(
        VerificationStatus.ENTERED_IN_ERROR,
        VerificationStatus.get(DiseaseStatus.StatusEnum.ERROR));
  }

  @Test
  void testCreateCoding() {
    Coding coding = VerificationStatus.createCoding(DiseaseStatus.StatusEnum.FINAL);
    assertEquals(VerificationStatus.CODE_SYSTEM, coding.getSystem());
    assertEquals("confirmed", coding.getCode());
    assertEquals(
        "confirmed", VerificationStatus.createCoding(DiseaseStatus.StatusEnum.AMENDED).getCode());
    assertEquals(
        "unconfirmed",
        VerificationStatus.createCoding(DiseaseStatus.StatusEnum.PRELIMINARY).getCode());
    assertEquals(
        "refuted", VerificationStatus.createCoding(DiseaseStatus.StatusEnum.REFUTED).getCode());
    assertEquals(
        "entered-in-error",
        VerificationStatus.createCoding(DiseaseStatus.StatusEnum.ERROR).getCode());
  }

  @Test
  void get_shouldThrowException_whenStatusIsNull() {
    assertThrows(NullPointerException.class, () -> VerificationStatus.get(null));
  }
}
