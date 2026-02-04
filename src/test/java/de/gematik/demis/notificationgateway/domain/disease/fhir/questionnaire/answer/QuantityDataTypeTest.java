package de.gematik.demis.notificationgateway.domain.disease.fhir.questionnaire.answer;

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

import static org.assertj.core.api.Assertions.*;

import de.gematik.demis.notificationgateway.common.dto.Quantity;
import de.gematik.demis.notificationgateway.common.dto.QuestionnaireResponseAnswer;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class QuantityDataTypeTest {

  private final QuantityDataType quantityDataType = new QuantityDataType();

  @Test
  void toFhir_shouldMapAllFields() {
    Quantity dto =
        new Quantity()
            .value(BigDecimal.valueOf(42.5))
            .unit("mg")
            .system("http://unitsofmeasure.org")
            .code("mg");

    QuestionnaireResponseAnswer answer = new QuestionnaireResponseAnswer().valueQuantity(dto);

    org.hl7.fhir.r4.model.Quantity fhirQuantity = quantityDataType.toFhir(answer);

    assertThat(fhirQuantity.getValue().doubleValue()).isEqualTo(42.5);
    assertThat(fhirQuantity.getUnit()).isEqualTo("mg");
    assertThat(fhirQuantity.getSystem()).isEqualTo("http://unitsofmeasure.org");
    assertThat(fhirQuantity.getCode()).isEqualTo("mg");
  }

  @Test
  void toFhir_shouldMapNullComparator() {
    Quantity dto =
        new Quantity().value(BigDecimal.valueOf(1.23)).unit("g").system("test-system").code("g");

    QuestionnaireResponseAnswer answer = new QuestionnaireResponseAnswer().valueQuantity(dto);

    org.hl7.fhir.r4.model.Quantity fhirQuantity = quantityDataType.toFhir(answer);

    assertThat(fhirQuantity.getComparator()).isNull();
  }

  @Test
  void toFhir_shouldThrowException_whenValueQuantityIsNull() {
    QuestionnaireResponseAnswer answer = new QuestionnaireResponseAnswer().valueQuantity(null);

    assertThatThrownBy(() -> quantityDataType.toFhir(answer))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("quantity value not set");
  }

  @Test
  void test_shouldReturnTrue_whenValueQuantityIsNotNull() {
    Quantity dto = new Quantity().value(BigDecimal.ONE);
    QuestionnaireResponseAnswer answer = new QuestionnaireResponseAnswer().valueQuantity(dto);

    assertThat(quantityDataType.test(answer)).isTrue();
  }

  @Test
  void test_shouldReturnFalse_whenValueQuantityIsNull() {
    QuestionnaireResponseAnswer answer = new QuestionnaireResponseAnswer().valueQuantity(null);

    assertThat(quantityDataType.test(answer)).isFalse();
  }
}
