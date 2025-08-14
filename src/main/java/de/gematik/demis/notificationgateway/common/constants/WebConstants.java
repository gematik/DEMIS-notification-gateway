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
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConstants {

  @Value("${api.ng.notification.context-path}")
  public void setApiNgNotificationContextPath(String contextPath) {
    PATHOGEN_PATH = contextPath + "/pathogen";
  }

  @Value("${api.ng.bedoccupancy.context-path}")
  public void setApiNgReportsContextPath(String contextPath) {
    BED_OCCUPANCY_PATH = contextPath + "/bedOccupancy";
  }

  public static String PATHOGEN_PATH;
  public static String BED_OCCUPANCY_PATH;

  public static final String HEADER_X_REAL_IP = "x-real-ip";
  public static final String NOT_AVAILABLE = "N/A";
}
