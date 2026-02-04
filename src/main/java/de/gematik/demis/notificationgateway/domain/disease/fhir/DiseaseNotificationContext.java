package de.gematik.demis.notificationgateway.domain.disease.fhir;

/*-
 * #%L
 * DEMIS Notification-Gateway
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
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
 * For additional notes and disclaimer from gematik and in case of changes by gematik,
 * find details in the "Readme" file.
 * #L%
 */

import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.disease.NotificationBundleDiseaseDataBuilder;
import de.gematik.demis.notificationgateway.common.dto.DiseaseNotification;
import org.hl7.fhir.r4.model.Patient;

/**
 * Context for building a FHIR bundleBuilder
 *
 * @param notification disease notification
 * @param bundleBuilder bundleBuilder
 * @param notifiedPerson notified person
 */
public record DiseaseNotificationContext(
    DiseaseNotification notification,
    NotificationBundleDiseaseDataBuilder bundleBuilder,
    Patient notifiedPerson) {
  /**
   * Get category, aka. disease code
   *
   * @return category
   */
  public String category() {
    return notification().getStatus().getCategory().toLowerCase();
  }
}
