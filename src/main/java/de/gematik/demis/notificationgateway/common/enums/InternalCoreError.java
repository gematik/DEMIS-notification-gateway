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
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */

import static de.gematik.demis.notificationgateway.common.constants.MessageConstants.FAILED_TO_PROCEED_REQUEST;
import static de.gematik.demis.notificationgateway.common.constants.MessageConstants.VALIDATION_ERROR_OCCURRED;

public enum InternalCoreError {
  NG_100_TOKEN(FAILED_TO_PROCEED_REQUEST),
  NG_200_VALIDATION(VALIDATION_ERROR_OCCURRED),
  NG_300_REQUEST(FAILED_TO_PROCEED_REQUEST);

  private final String message;

  InternalCoreError(String message) {
    this.message = message;
  }

  public String reason() {
    return this.message + " (" + this.name() + ")";
  }
}
