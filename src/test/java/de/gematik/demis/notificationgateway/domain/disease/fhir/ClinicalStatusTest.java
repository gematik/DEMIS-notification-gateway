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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import de.gematik.demis.notificationgateway.common.dto.DiseaseStatus;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.codesystems.ConditionClinical;
import org.junit.jupiter.api.Test;

class ClinicalStatusTest {

  @Test
  void testGet() {
    assertThat(ClinicalStatus.get(DiseaseStatus.StatusEnum.FINAL))
        .isSameAs(ConditionClinical.ACTIVE);
    assertThat(ClinicalStatus.get(DiseaseStatus.StatusEnum.PRELIMINARY))
        .isSameAs(ConditionClinical.ACTIVE);
    assertThat(ClinicalStatus.get(DiseaseStatus.StatusEnum.AMENDED))
        .isSameAs(ConditionClinical.ACTIVE);
    assertThat(ClinicalStatus.get(DiseaseStatus.StatusEnum.REFUTED))
        .isSameAs(ConditionClinical.INACTIVE);
    assertThat(ClinicalStatus.get(DiseaseStatus.StatusEnum.ERROR)).isNull();
  }

  @Test
  void testCreateCoding() {
    assertThat(ClinicalStatus.createCoding(DiseaseStatus.StatusEnum.FINAL))
        .isPresent()
        .map(Coding::getCode)
        .contains("active");
    assertThat(ClinicalStatus.createCoding(DiseaseStatus.StatusEnum.PRELIMINARY))
        .isPresent()
        .map(Coding::getCode)
        .contains("active");
    assertThat(ClinicalStatus.createCoding(DiseaseStatus.StatusEnum.AMENDED))
        .isPresent()
        .map(Coding::getCode)
        .contains("active");
    assertThat(ClinicalStatus.createCoding(DiseaseStatus.StatusEnum.REFUTED))
        .isPresent()
        .map(Coding::getCode)
        .contains("inactive");
    assertThat(ClinicalStatus.createCoding(DiseaseStatus.StatusEnum.ERROR)).isEmpty();
  }

  @Test
  void get_shouldThrowException_whenStatusIsNull() {
    assertThrows(NullPointerException.class, () -> ClinicalStatus.get(null));
  }
}
