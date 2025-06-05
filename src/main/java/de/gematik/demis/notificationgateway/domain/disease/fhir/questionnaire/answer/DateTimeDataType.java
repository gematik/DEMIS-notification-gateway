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

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import de.gematik.demis.notificationgateway.common.dto.QuestionnaireResponseAnswer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;

final class DateTimeDataType implements DataType<DateTimeType> {

  private static final String DATE_TIME_PATTERN_GERMAN = "dd.MM.yyyy HH:mm";

  private final DateDataType dateType = new DateDataType();

  @Override
  public DateTimeType toFhir(QuestionnaireResponseAnswer answer) {
    final String dateTime = answer.getValueDateTime();
    if (StringUtils.isNotBlank(dateTime)) {
      return parseDateTime(dateTime);
    }
    final String date = answer.getValueDate();
    if (StringUtils.isNotBlank(date)) {
      return createFromDate(answer);
    }
    throw new IllegalArgumentException("Invalid date time answer without value");
  }

  @Override
  public boolean test(QuestionnaireResponseAnswer answer) {
    return StringUtils.isNotBlank(answer.getValueDateTime())
        || StringUtils.isNotBlank(answer.getValueDate());
  }

  private DateTimeType parseDateTime(String text) {
    DateTimeType dateTimeType = new DateTimeType();
    dateTimeType.setValue(parseGermanDateTime(text), TemporalPrecisionEnum.SECOND);
    return dateTimeType;
  }

  private Date parseGermanDateTime(String text) {
    try {
      final SimpleDateFormat parser = new SimpleDateFormat(DATE_TIME_PATTERN_GERMAN);
      parser.setLenient(false);
      return parser.parse(text);
    } catch (ParseException e) {
      throw new IllegalArgumentException("Invalid date time format: " + text, e);
    }
  }

  private DateTimeType createFromDate(QuestionnaireResponseAnswer answer) {
    DateType date = this.dateType.toFhir(answer);
    DateTimeType dateTime = new DateTimeType();
    dateTime.setPrecision(date.getPrecision());
    dateTime.setValueAsString(date.getValueAsString());
    return dateTime;
  }
}
