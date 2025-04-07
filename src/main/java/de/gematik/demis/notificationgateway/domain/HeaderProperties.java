package de.gematik.demis.notificationgateway.domain;

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

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@NoArgsConstructor
public class HeaderProperties {

  @Value("${header.bed.occupancy.version}")
  private String bedOccupancyVersion;

  @Value("${header.bed.occupancy.profile}")
  private String bedOccupancyProfile;

  @Value("${header.laboratory.notification.version}")
  private String laboratoryNotificationVersion;

  @Value("${header.laboratory.notification.profile}")
  private String laboratoryNotificationProfile;

  @Value("${header.disease.notification.version}")
  private String diseaseNotificationVersion;

  @Value("${header.disease.notification.profile}")
  private String diseaseNotificationProfile;
}
