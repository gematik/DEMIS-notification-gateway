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
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import de.gematik.demis.notificationgateway.common.dto.QuestionnaireResponseAnswer;
import org.hl7.fhir.r4.model.DateTimeType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DateDataTypesTest {

  @ParameterizedTest
  @CsvSource({
    "24.01.2024,2024-01-24",
    "2024-01-24,2024-01-24",
    "2024,2024",
    "2024-01,2024-01",
    "01.2024,2024-01",
    "13.07.1998,1998-07-13",
    "1998-07-13,1998-07-13",
    "1998,1998",
    "1998-07,1998-07",
    "07.1998,1998-07",
    "31.10.2024,2024-10-31"
  })
  void testDateDataType(String input, String expected) {
    QuestionnaireResponseAnswer answer = new QuestionnaireResponseAnswer();
    answer.setValueDate(input);
    DateDataType dateDataType = new DateDataType();
    assertThat(dateDataType.test(answer)).isTrue();
    String text = dateDataType.toFhir(answer).getValueAsString();
    assertThat(text).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({"24.05.2001 13:45,2001-05-24T13:45:00+02:00"})
  void testDateTimeDataType(String input, String expected) {
    QuestionnaireResponseAnswer answer = new QuestionnaireResponseAnswer();
    answer.setValueDateTime(input);
    DateTimeDataType dateTimeDataType = new DateTimeDataType();
    assertThat(dateTimeDataType.test(answer)).isTrue();
    String text = dateTimeDataType.toFhir(answer).getValueAsString();
    assertThat(text).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
    "01.07.2011,2011-07-01",
    "02.07.2011,2011-07-02",
    "24.07.2011,2011-07-24",
    "31.10.2011,2011-10-31",
    "28.02.2023,2023-02-28",
    "31.01.2024,2024-01-31",
    "29.02.2024,2024-02-29",
    "31.03.2024,2024-03-31",
    "30.04.2024,2024-04-30",
    "31.10.2024,2024-10-31",
  })
  void testConvertingDateToDateTime(String input, String expected) {
    QuestionnaireResponseAnswer answer = new QuestionnaireResponseAnswer();
    answer.setValueDate(input);
    DateTimeDataType dateTimeDataType = new DateTimeDataType();
    assertThat(dateTimeDataType.test(answer)).isTrue();
    DateTimeType dateTimeType = dateTimeDataType.toFhir(answer);
    assertThat(dateTimeType.getPrecision()).isEqualTo(TemporalPrecisionEnum.DAY);
    String text = dateTimeType.getValueAsString();
    assertThat(text).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({"15:45,15:45"})
  void testTimeDataType(String input, String expected) {
    QuestionnaireResponseAnswer answer = new QuestionnaireResponseAnswer();
    answer.setValueTime(input);
    TimeDataType timeDataType = new TimeDataType();
    assertThat(timeDataType.test(answer)).isTrue();
    String text = timeDataType.toFhir(answer).getValueAsString();
    assertThat(text).isEqualTo(expected);
  }
}
