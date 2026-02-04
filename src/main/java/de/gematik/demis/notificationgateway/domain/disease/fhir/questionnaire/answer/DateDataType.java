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

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import de.gematik.demis.notificationgateway.common.dto.QuestionnaireResponseAnswer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.DateType;

final class DateDataType implements DataType<DateType> {

  private static final String MONTH_OF_YEAR_GERMAN = "MM.yyyy";
  private static final String DAY_OF_YEAR_GERMAN = "dd.MM.yyyy";
  private static final String MONTH_OF_YEAR_ISO = "yyyy-MM";
  private static final String DAY_OF_YEAR_ISO = "yyyy-MM-dd";
  private static final String UNSUPPORTED_DATE_FORMAT = "Unsupported date format: ";

  private static Date parse(String text, String pattern) {
    try {
      final SimpleDateFormat parser = new SimpleDateFormat(pattern);
      parser.setLenient(false);
      return parser.parse(text);
    } catch (ParseException e) {
      throw new IllegalArgumentException(UNSUPPORTED_DATE_FORMAT + text, e);
    }
  }

  @Override
  public DateType toFhir(QuestionnaireResponseAnswer answer) {
    return createDateType(getValue(answer));
  }

  @Override
  public boolean test(QuestionnaireResponseAnswer answer) {
    return StringUtils.isNotBlank(answer.getValueDate());
  }

  private DateType createDateType(String date) {
    switch (date.length()) {
      case 4:
        return createYearFrom4Chars(date);
      case 7:
        return createMonthOfYearFrom7Chars(date);
      case 10:
        return createDateFrom10Chars(date);
      default:
        throw new IllegalArgumentException(UNSUPPORTED_DATE_FORMAT + date);
    }
  }

  private DateType createMonthOfYearFrom7Chars(String text) {
    final String pattern = getMonthOfYearPattern(text);
    final DateType value = new DateType();
    value.setValue(parse(text, pattern), TemporalPrecisionEnum.MONTH);
    return value;
  }

  private DateType createYearFrom4Chars(String text) {
    DateType date = new DateType();
    date.setPrecision(TemporalPrecisionEnum.YEAR);
    date.setYear(Integer.parseInt(text));
    return date;
  }

  private DateType createDateFrom10Chars(String text) {
    final String pattern = getDayOfYearPattern(text);
    final DateType date = new DateType();
    date.setValue(parse(text, pattern), TemporalPrecisionEnum.DAY);
    return date;
  }

  private boolean isGerman(String text) {
    return text.indexOf('.') > -1;
  }

  private boolean isIso(String text) {
    return text.indexOf('-') > -1;
  }

  private String getDayOfYearPattern(String text) {
    if (isGerman(text)) {
      return DAY_OF_YEAR_GERMAN;
    }
    if (isIso(text)) {
      return DAY_OF_YEAR_ISO;
    }
    throw new IllegalArgumentException(UNSUPPORTED_DATE_FORMAT + text);
  }

  private String getMonthOfYearPattern(String text) {
    if (isGerman(text)) {
      return MONTH_OF_YEAR_GERMAN;
    }
    if (isIso(text)) {
      return MONTH_OF_YEAR_ISO;
    }
    throw new IllegalArgumentException(UNSUPPORTED_DATE_FORMAT + text);
  }

  private String getValue(QuestionnaireResponseAnswer answer) {
    return Objects.requireNonNull(answer.getValueDate(), "date value not set");
  }
}
