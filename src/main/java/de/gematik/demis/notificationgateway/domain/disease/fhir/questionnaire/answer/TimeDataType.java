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

import de.gematik.demis.notificationgateway.common.dto.QuestionnaireResponseAnswer;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.TimeType;

final class TimeDataType implements DataType<TimeType> {

  @Override
  public TimeType toFhir(QuestionnaireResponseAnswer answer) {
    return new TimeType(getValue(answer));
  }

  @Override
  public boolean test(QuestionnaireResponseAnswer answer) {
    return StringUtils.isNotBlank(answer.getValueTime());
  }

  private String getValue(QuestionnaireResponseAnswer answer) {
    final String input = Objects.requireNonNull(answer.getValueTime(), "time value not set");
    // Supported formats: HH:mm, HH:mm:ss, HH:mm:ss.SSS
    if (!input.matches("^\\d{2}:\\d{2}(:\\d{2}(\\.\\d{1,3})?)?$")) {
      throw new IllegalArgumentException("Invalid time format: " + input);
    }
    final String[] parts = input.split("[:\\.]");
    final int hour = Integer.parseInt(parts[0]);
    if (hour < 0 || hour > 23) {
      throw new IllegalArgumentException("Hour out of valid range: " + hour);
    }
    final int minute = Integer.parseInt(parts[1]);
    if (minute < 0 || minute > 59) {
      throw new IllegalArgumentException("Minute out of valid range: " + minute);
    }
    if (parts.length > 2) {
      final int second = Integer.parseInt(parts[2]);
      if (second < 0 || second > 59) {
        throw new IllegalArgumentException("Second out of valid range: " + second);
      }
    }
    return input;
  }
}
