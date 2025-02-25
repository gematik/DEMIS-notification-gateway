package de.gematik.demis.notificationgateway.domain.disease.fhir.questionnaire.answer;

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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import de.gematik.demis.notificationgateway.common.dto.QuestionnaireResponseAnswer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NumberDataTypesTest {

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, -123, 123, 0})
  void testIntegerDataType(int input) {
    QuestionnaireResponseAnswer answer = new QuestionnaireResponseAnswer();
    answer.setValueInteger(input);
    IntegerDataType integerDataType = new IntegerDataType();
    assertThat(integerDataType.test(answer)).isTrue();
    String text = integerDataType.toFhir(answer).getValueAsString();
    assertThat(Integer.parseInt(text)).isEqualTo(input);
  }
}
