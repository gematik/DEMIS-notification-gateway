package de.gematik.demis.notificationgateway.domain.disease;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.demis.notificationgateway.FeatureFlags;
import de.gematik.demis.notificationgateway.common.dto.DiseaseNotification;
import de.gematik.demis.notificationgateway.common.dto.OkResponse;
import de.gematik.demis.notificationgateway.utils.FileUtils;
import jakarta.validation.Validator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

@ExtendWith(MockitoExtension.class)
class DiseaseRestControllerTest {

  @Mock private DiseaseNotificationService notificationService;
  @Mock private Validator validator;
  @Mock private HttpHeaders headers;
  private DiseaseRestController controller;

  @Test
  void addDiseaseNotification_shouldSucceedRegression() throws Exception {
    FeatureFlags featureFlagsMock = mock(FeatureFlags.class);
    when(featureFlagsMock.isNotifications73()).thenReturn(false);

    controller = new DiseaseRestController(validator, notificationService, featureFlagsMock);

    DiseaseNotification diseaseNotification =
        FileUtils.createDiseaseNotification("portal/disease/notification-formly-input.json");
    when(this.notificationService.sendNotification(eq(diseaseNotification), any()))
        .thenReturn(new OkResponse());
    when(headers.get("Authorization")).thenReturn(List.of("Bearer " + "token"));

    this.controller.addDiseaseNotification(diseaseNotification, headers);
    verify(this.notificationService).sendNotification(eq(diseaseNotification), any());
    verify(this.notificationService, never())
        .sendNotification((eq(diseaseNotification)), any(), any());
  }

  @Test
  void addDiseaseNotification_shouldSucceed() throws Exception {
    FeatureFlags featureFlagsMock = mock(FeatureFlags.class);
    when(featureFlagsMock.isNotifications73()).thenReturn(true);

    controller = new DiseaseRestController(validator, notificationService, featureFlagsMock);

    DiseaseNotification diseaseNotification =
        FileUtils.createDiseaseNotification("portal/disease/notification-formly-input.json");
    when(this.notificationService.sendNotification(eq(diseaseNotification), any(), any()))
        .thenReturn(new OkResponse());
    when(headers.get("Authorization")).thenReturn(List.of("Bearer " + "token"));

    this.controller.addDiseaseNotification(diseaseNotification, headers);
    verify(this.notificationService).sendNotification(eq(diseaseNotification), any(), any());
    verify(this.notificationService, never()).sendNotification(eq(diseaseNotification), any());
  }
}
