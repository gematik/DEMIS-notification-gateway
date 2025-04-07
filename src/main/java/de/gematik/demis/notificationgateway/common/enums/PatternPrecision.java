package de.gematik.demis.notificationgateway.common.enums;

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

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import de.gematik.demis.notificationgateway.common.exceptions.BadRequestException;
import java.util.Arrays;
import lombok.Getter;

@Getter
public enum PatternPrecision {
  YYYY_MM_DD("yyyy-MM-dd", 10, TemporalPrecisionEnum.DAY),
  YYYY_MM("yyyy-MM", 7, TemporalPrecisionEnum.MONTH),
  YYYY("yyyy", 4, TemporalPrecisionEnum.YEAR),
  YYYY_M("yyyy-M", 6, TemporalPrecisionEnum.MONTH),
  YYYY_M_D("yyyy-M-d", 8, TemporalPrecisionEnum.DAY);

  private final String pattern;
  private final int length;
  private final TemporalPrecisionEnum precision;

  PatternPrecision(String pattern, int length, TemporalPrecisionEnum precision) {
    this.pattern = pattern;
    this.length = length;
    this.precision = precision;
  }

  public static PatternPrecision byLength(int length) throws BadRequestException {
    return Arrays.stream(values())
        .filter(patternPrecision -> patternPrecision.length == length)
        .findFirst()
        .orElseThrow(() -> new BadRequestException("invalid date length"));
  }
}
