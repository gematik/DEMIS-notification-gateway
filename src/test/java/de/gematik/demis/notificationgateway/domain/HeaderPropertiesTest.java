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
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HeaderPropertiesTest {

  @Test
  void testGettersAndSetters() {
    HeaderProperties headerProperties = new HeaderProperties();

    String bedVersion = "Bed Version 1";
    String bedProfile = "Bed Profile 1";
    String labVersion = "Lab Version 1";
    String labProfile = "Lab Profile 1";
    String diseaseVersion = "Disease Version 1";
    String diseaseProfile = "Disease Profile 1";

    headerProperties.setBedOccupancyVersion(bedVersion);
    headerProperties.setBedOccupancyProfile(bedProfile);
    headerProperties.setLaboratoryNotificationVersion(labVersion);
    headerProperties.setLaboratoryNotificationProfile(labProfile);
    headerProperties.setDiseaseNotificationVersion(diseaseVersion);
    headerProperties.setDiseaseNotificationProfile(diseaseProfile);

    assertThat(headerProperties.getBedOccupancyVersion()).isEqualTo(bedVersion);
    assertThat(headerProperties.getBedOccupancyProfile()).isEqualTo(bedProfile);
    assertThat(headerProperties.getLaboratoryNotificationVersion()).isEqualTo(labVersion);
    assertThat(headerProperties.getLaboratoryNotificationProfile()).isEqualTo(labProfile);
    assertThat(headerProperties.getDiseaseNotificationVersion()).isEqualTo(diseaseVersion);
    assertThat(headerProperties.getDiseaseNotificationProfile()).isEqualTo(diseaseProfile);
  }
}
