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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.demis.notificationgateway.common.dto.CodeDisplay;
import de.gematik.demis.notificationgateway.common.dto.Quantity;
import de.gematik.demis.notificationgateway.common.dto.QuestionnaireResponseAnswer;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnswersTest {

  private Answers answers;

  @BeforeEach
  void setUp() {
    answers = new AnswersFactory().get();
  }

  @Test
  void testContainsValue_withStringValue() {
    QuestionnaireResponseAnswer answer = mock(QuestionnaireResponseAnswer.class);
    when(answer.getValueString()).thenReturn("test");
    assertThat(answers.containsValue(answer)).isTrue();
  }

  @Test
  void testContainsValue_withIntegerValue() {
    QuestionnaireResponseAnswer answer = mock(QuestionnaireResponseAnswer.class);
    when(answer.getValueInteger()).thenReturn(1);
    assertThat(answers.containsValue(answer)).isTrue();
  }

  @Test
  void testContainsValue_withNullValues() {
    QuestionnaireResponseAnswer answer = mock(QuestionnaireResponseAnswer.class);
    when(answer.getValueString()).thenReturn(null);
    when(answer.getValueBoolean()).thenReturn(null);
    when(answer.getValueDecimal()).thenReturn(null);
    when(answer.getValueInteger()).thenReturn(null);
    when(answer.getValueDecimal()).thenReturn(null);
    when(answer.getValueDate()).thenReturn(null);
    when(answer.getValueDateTime()).thenReturn(null);
    when(answer.getValueTime()).thenReturn(null);
    when(answer.getValueUri()).thenReturn(null);
    when(answer.getValueCoding()).thenReturn(null);
    when(answer.getValueQuantity()).thenReturn(null);
    when(answer.getValueReference()).thenReturn(null);

    assertThat(answers.containsValue(answer)).isFalse();
  }

  @Test
  void testCreateFhirAnswer_returnsFhirAnswerInstance() {
    QuestionnaireResponseAnswer answer = mock(QuestionnaireResponseAnswer.class);
    assertThat(answers.createFhirAnswer(answer)).isNotNull();
  }

  @Test
  void testToStringType_returnsExpectedStringType() {
    QuestionnaireResponseAnswer answer = mock(QuestionnaireResponseAnswer.class);
    when(answer.getValueString()).thenReturn("Hallo Test!");
    FhirAnswer fhirAnswer = answers.createFhirAnswer(answer);

    assertThat(fhirAnswer).isInstanceOf(Answers.FhirAnswerImpl.class);
    assertThat(((Answers.FhirAnswerImpl) fhirAnswer).toStringType().getValue())
        .isEqualTo("Hallo Test!");
  }

  @Test
  void testToBooleanType_returnsExpectedBooleanType() {
    QuestionnaireResponseAnswer answer = mock(QuestionnaireResponseAnswer.class);
    when(answer.getValueBoolean()).thenReturn(true);
    FhirAnswer fhirAnswer = answers.createFhirAnswer(answer);

    assertThat(fhirAnswer).isInstanceOf(Answers.FhirAnswerImpl.class);
    assertThat(((Answers.FhirAnswerImpl) fhirAnswer).toBooleanType().booleanValue()).isTrue();
  }

  @Test
  void testToDecimalType_returnsExpectedDecimalType() {
    QuestionnaireResponseAnswer answer = mock(QuestionnaireResponseAnswer.class);
    when(answer.getValueDecimal()).thenReturn(new BigDecimal(42.5));
    FhirAnswer fhirAnswer = answers.createFhirAnswer(answer);

    assertThat(fhirAnswer).isInstanceOf(Answers.FhirAnswerImpl.class);
    assertThat(((Answers.FhirAnswerImpl) fhirAnswer).toDecimalType().getValue().doubleValue())
        .isEqualTo(42.5);
  }

  @Test
  void testToIntegerType_returnsExpectedIntegerType() {
    QuestionnaireResponseAnswer answer = mock(QuestionnaireResponseAnswer.class);
    when(answer.getValueInteger()).thenReturn(123);
    FhirAnswer fhirAnswer = answers.createFhirAnswer(answer);

    assertThat(fhirAnswer).isInstanceOf(Answers.FhirAnswerImpl.class);
    assertThat(((Answers.FhirAnswerImpl) fhirAnswer).toIntegerType().getValue().intValue())
        .isEqualTo(123);
  }

  @Test
  void testToDateType_returnsExpectedDateType() {
    QuestionnaireResponseAnswer answer = mock(QuestionnaireResponseAnswer.class);
    when(answer.getValueDate()).thenReturn("2024-07-10");
    FhirAnswer fhirAnswer = answers.createFhirAnswer(answer);

    assertThat(fhirAnswer).isInstanceOf(Answers.FhirAnswerImpl.class);
    assertThat(((Answers.FhirAnswerImpl) fhirAnswer).toDateType().getValueAsString())
        .isEqualTo("2024-07-10");
  }

  @Test
  void testToDateTimeType_returnsExpectedDateTimeType() {
    QuestionnaireResponseAnswer answer = mock(QuestionnaireResponseAnswer.class);
    when(answer.getValueDateTime()).thenReturn("10.07.2024 15:30:00");
    FhirAnswer fhirAnswer = answers.createFhirAnswer(answer);

    assertThat(fhirAnswer).isInstanceOf(Answers.FhirAnswerImpl.class);
    assertThat(((Answers.FhirAnswerImpl) fhirAnswer).toDateTimeType().getValueAsString())
        .isEqualTo("2024-07-10T15:30:00+02:00");
  }

  @Test
  void testToTimeType_returnsExpectedTimeType() {
    QuestionnaireResponseAnswer answer = mock(QuestionnaireResponseAnswer.class);
    when(answer.getValueTime()).thenReturn("12:34:56");
    FhirAnswer fhirAnswer = answers.createFhirAnswer(answer);

    assertThat(fhirAnswer).isInstanceOf(Answers.FhirAnswerImpl.class);
    assertThat(((Answers.FhirAnswerImpl) fhirAnswer).toTimeType().getValueAsString())
        .isEqualTo("12:34:56");
  }

  @Test
  void testToUriType_returnsExpectedUriType() {
    QuestionnaireResponseAnswer answer = mock(QuestionnaireResponseAnswer.class);
    when(answer.getValueUri()).thenReturn("http://example.org/test");
    FhirAnswer fhirAnswer = answers.createFhirAnswer(answer);

    assertThat(fhirAnswer).isInstanceOf(Answers.FhirAnswerImpl.class);
    assertThat(((Answers.FhirAnswerImpl) fhirAnswer).toUriType().getValue())
        .isEqualTo("http://example.org/test");
  }

  @Test
  void testToQuantity_returnsNotNullQuantity() {
    QuestionnaireResponseAnswer answer = mock(QuestionnaireResponseAnswer.class);
    BigDecimal value = new BigDecimal(100);
    when(answer.getValueQuantity()).thenReturn(new Quantity(value));
    FhirAnswer fhirAnswer = answers.createFhirAnswer(answer);

    assertThat(fhirAnswer).isInstanceOf(Answers.FhirAnswerImpl.class);
    assertThat(((Answers.FhirAnswerImpl) fhirAnswer).toQuantity())
        .isInstanceOf(org.hl7.fhir.r4.model.Quantity.class)
        .hasFieldOrPropertyWithValue("value", value);
  }

  @Test
  void testToCoding_returnsNotNullCoding() {
    QuestionnaireResponseAnswer answer = mock(QuestionnaireResponseAnswer.class);
    when(answer.getValueCoding()).thenReturn(new CodeDisplay("code"));
    FhirAnswer fhirAnswer = answers.createFhirAnswer(answer);

    assertThat(fhirAnswer).isInstanceOf(Answers.FhirAnswerImpl.class);
    assertThat(((Answers.FhirAnswerImpl) fhirAnswer).toCoding())
        .isInstanceOf(org.hl7.fhir.r4.model.Coding.class)
        .hasFieldOrPropertyWithValue("code", "code");
  }

  @Test
  void testToReference_returnsNotNullReference() {
    QuestionnaireResponseAnswer answer = mock(QuestionnaireResponseAnswer.class);
    when(answer.getValueReference()).thenReturn("123456789");
    FhirAnswer fhirAnswer = answers.createFhirAnswer(answer);

    assertThat(fhirAnswer).isInstanceOf(Answers.FhirAnswerImpl.class);
    assertThat(((Answers.FhirAnswerImpl) fhirAnswer).toReference().getReference())
        .isEqualTo("123456789");
  }
}
