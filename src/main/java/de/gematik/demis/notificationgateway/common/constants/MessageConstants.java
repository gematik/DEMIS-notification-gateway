package de.gematik.demis.notificationgateway.common.constants;

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

import lombok.experimental.UtilityClass;

@UtilityClass
public class MessageConstants {
  public static final String CLIENT_ADDRESS_EMPTY =
      "Client IP address has not been forwarded in HTTP headers!";
  public static final String VALIDATION_ERROR_OCCURRED = "Validation error occurred";
  public static final String INSTANTIATION_ERROR_OCCURRED = "Instantiation error occurred";
  public static final String FAILED_TO_PROCEED_REQUEST = "failed to proceed request";
  public static final String CONTENT_NOT_ACCEPTED = "content not accepted";
}
