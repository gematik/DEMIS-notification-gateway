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
 * #L%
 */

import static org.junit.jupiter.api.Assertions.assertThrows;

import de.gematik.demis.notificationgateway.common.dto.Condition;
import de.gematik.demis.notificationgateway.common.dto.DiseaseNotification;
import de.gematik.demis.notificationgateway.common.dto.DiseaseStatus;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPerson;
import de.gematik.demis.notificationgateway.common.dto.NotifierFacility;
import de.gematik.demis.notificationgateway.common.dto.QuestionnaireResponse;
import de.gematik.demis.notificationgateway.common.exceptions.HoneypotException;
import de.gematik.demis.notificationgateway.common.properties.NESProperties;
import de.gematik.demis.notificationgateway.common.proxies.BundlePublisher;
import de.gematik.demis.notificationgateway.common.services.OkResponseService;
import de.gematik.demis.notificationgateway.domain.HeaderProperties;
import de.gematik.demis.notificationgateway.domain.disease.fhir.DiseaseNotificationBundleCreationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiseaseNotificationServiceTest {

  @Mock private DiseaseNotificationBundleCreationService bundleCreationService;
  @Mock private BundlePublisher bundlePublisher;
  @Mock private OkResponseService okResponseService;
  @Mock private NESProperties nesProperties;
  @Mock private HeaderProperties headerProperties;

  @InjectMocks private DiseaseNotificationService diseaseNotificationService;

  private static DiseaseNotification createDiseaseNotification() {
    DiseaseNotification notification = new DiseaseNotification();
    notification.setNotifierFacility(new NotifierFacility());
    notification.setNotifiedPerson(new NotifiedPerson());
    notification.setStatus(new DiseaseStatus());
    notification.setCondition(new Condition());
    notification.setCommon(new QuestionnaireResponse());
    notification.setDisease(new QuestionnaireResponse());
    return notification;
  }

  @Test
  void givenHoneyPotNotifierFacilityWhenSendNotificationThenError() {
    DiseaseNotification diseaseNotification = createDiseaseNotification();
    diseaseNotification.getNotifierFacility().setOneTimeCode("test");
    verifyHoneyPotDetection(diseaseNotification);
  }

  private void verifyHoneyPotDetection(DiseaseNotification diseaseNotification) {
    assertThrows(
        HoneypotException.class,
        () -> diseaseNotificationService.sendNotification(diseaseNotification, null));
  }
}
