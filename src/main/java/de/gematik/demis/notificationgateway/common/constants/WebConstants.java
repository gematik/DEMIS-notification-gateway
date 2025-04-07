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
public class WebConstants {

  public static final String API_NG_NOTIFICATION = "/api/ng/notification";
  public static final String API_NG_REPORTS = "/api/ng/reports";
  public static final String PATHOGEN_PATH = API_NG_NOTIFICATION + "/pathogen";
  public static final String BED_OCCUPANCY_PATH = API_NG_REPORTS + "/bedOccupancy";

  public static final String HEADER_X_REAL_IP = "x-real-ip";
  public static final String NOT_AVAILABLE = "N/A";
}
