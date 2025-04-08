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

import de.gematik.demis.notificationgateway.common.dto.Condition;
import de.gematik.demis.notificationgateway.common.dto.DiseaseNotification;
import de.gematik.demis.notificationgateway.common.dto.DiseaseStatus;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPerson;
import de.gematik.demis.notificationgateway.common.dto.NotifierFacility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

record ValidationDiseaseNotification(
    @NotNull @Valid NotifierFacility notifierFacility,
    @NotNull @Valid NotifiedPerson notifiedPerson,
    @NotNull @Valid DiseaseStatus status,
    @NotNull @Valid Condition condition) {

  static ValidationDiseaseNotification of(DiseaseNotification diseaseNotification) {
    return new ValidationDiseaseNotification(
        diseaseNotification.getNotifierFacility(),
        diseaseNotification.getNotifiedPerson(),
        diseaseNotification.getStatus(),
        diseaseNotification.getCondition());
  }
}
