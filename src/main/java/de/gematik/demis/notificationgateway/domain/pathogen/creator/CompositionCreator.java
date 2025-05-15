package de.gematik.demis.notificationgateway.domain.pathogen.creator;

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

import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.laboratory.AnonymousCompositionBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.laboratory.NonNominalCompositionBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.laboratory.NotificationLaboratoryDataBuilder;
import de.gematik.demis.notificationgateway.common.dto.NotificationLaboratoryCategory;
import de.gematik.demis.notificationgateway.domain.pathogen.enums.LaboratoryNotificationType;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;

/**
 * Utility class for creating FHIR {@link Composition} objects.
 *
 * <p>This class provides methods to build a {@link Composition} instance based on the provided
 * patient, practitioner role, diagnostic report, notification category, and laboratory notification
 * type.
 */
public class CompositionCreator {

  /**
   * Private constructor to prevent instantiation of this utility class.
   *
   * <p>This class is designed as a utility class and should not be instantiated.
   */
  private CompositionCreator() {}

  /**
   * Creates a FHIR {@link Composition} object using the provided parameters.
   *
   * @param patient The {@link Patient} object representing the notified person.
   * @param practitionerRole The {@link PractitionerRole} object representing the notifier.
   * @param diagnosticReport The {@link DiagnosticReport} object containing laboratory results.
   * @param notificationCategory The {@link NotificationLaboratoryCategory} object containing
   *     details about the notification category.
   * @param laboratoryNotificationType The {@link LaboratoryNotificationType} enum indicating the
   *     type of laboratory notification (e.g., NON_NOMINAL, ANONYMOUS, LAB).
   * @return A {@link Composition} object populated with the provided data.
   */
  public static Composition createComposition(
      Patient patient,
      PractitionerRole practitionerRole,
      DiagnosticReport diagnosticReport,
      NotificationLaboratoryCategory notificationCategory,
      LaboratoryNotificationType laboratoryNotificationType) {

    final Composition.CompositionStatus compositionStatus =
        Composition.CompositionStatus.fromCode(notificationCategory.getReportStatus().getValue());

    final String notificationLaboratorySectionCompomentyCode = "11502-2";
    final String notificationLaboratorySectionCompomentyDisplay = "Laboratory report";

    final NotificationLaboratoryDataBuilder builder =
        switch (laboratoryNotificationType) {
          case NON_NOMINAL -> new NonNominalCompositionBuilder();
          case ANONYMOUS -> new AnonymousCompositionBuilder();
          case LAB -> new NotificationLaboratoryDataBuilder();
        };

    builder
        .setDefault()
        .setCompositionStatus(compositionStatus)
        .setTitle("Erregernachweismeldung")
        .setSectionComponentCode(notificationLaboratorySectionCompomentyCode)
        .setSectionComponentDisplay(notificationLaboratorySectionCompomentyDisplay)
        .setNotifiedPerson(patient)
        .setNotifierRole(practitionerRole)
        .setLaboratoryReport(diagnosticReport);

    final String initialNotificationId = notificationCategory.getInitialNotificationId();

    if (initialNotificationId != null) {
      builder.setRelatesToNotificationId(initialNotificationId);
    }

    return builder.build();
  }
}
