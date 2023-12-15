/*
 * Copyright [2023], gematik GmbH
 *
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
 */

package de.gematik.demis.notificationgateway.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class ApplicationProperties {
  @Value("${spring.application.name:DEMIS Notification-Gateway}")
  private String applicationName;

  @Value("${application.version}")
  private String applicationVersion;

  @Value("${application.legacy.mode:true}")
  private boolean legacyModeEnabled;

  @Value("${http.connection.timeout.ms:60000}")
  private int httpConnectionTimeoutMilliseconds;

  @Value("${http.connection.pool.timeout.ms:10000}")
  private int httpConnectionPoolTimeoutMilliseconds;

  @Value("${http.socket.timeout.ms:30000}")
  private int httpSocketTimeoutMilliseconds;

  public String identifier() {
    return applicationName + "" + applicationVersion;
  }
}
