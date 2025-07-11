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

import static de.gematik.demis.notificationgateway.domain.pathogen.creator.CompositionCreator.createComposition;
import static de.gematik.demis.notificationgateway.domain.pathogen.creator.LaboratoryReportCreator.createDiagnosticReport;
import static de.gematik.demis.notificationgateway.domain.pathogen.creator.PatientCreator.createPatient;
import static de.gematik.demis.notificationgateway.domain.pathogen.creator.PractitionerOrganizationCreator.createNotifierPractitionerRole;
import static de.gematik.demis.notificationgateway.domain.pathogen.creator.PractitionerOrganizationCreator.createSubmitterPractitionerRole;
import static de.gematik.demis.notificationgateway.domain.pathogen.creator.SpecimenCreator.createSpecimen;

import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.laboratory.AnonymousBundleBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.laboratory.NonNominalBundleBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.laboratory.NotificationBundleLaboratoryDataBuilder;
import de.gematik.demis.notificationgateway.common.dto.AddressType;
import de.gematik.demis.notificationgateway.common.dto.NotificationLaboratoryCategory;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPerson;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPersonAddressInfo;
import de.gematik.demis.notificationgateway.common.dto.NotifierFacility;
import de.gematik.demis.notificationgateway.common.dto.PathogenDTO;
import de.gematik.demis.notificationgateway.common.dto.PathogenTest;
import de.gematik.demis.notificationgateway.common.enums.NotificationType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Specimen;

/**
 * Utility class for creating FHIR {@link Bundle} objects.
 *
 * <p>This class provides methods to build a FHIR {@link Bundle} based on the provided {@link
 * PathogenTest} and {@link NotificationType}.
 */
public class BundleCreator {

  /**
   * Private constructor to prevent instantiation of this utility class.
   *
   * <p>This class is designed as a utility class and should not be instantiated.
   */
  private BundleCreator() {}

  /**
   * Retrieves the {@link Coding} object representing the address use based on the provided {@link
   * AddressType}.
   *
   * @param addressType The type of the address.
   * @param featureFlagSnapshot5_3_0Active A boolean flag indicating if the feature is active.
   * @return A {@link Coding} object representing the address use.
   * @throws IllegalArgumentException if the address type is null.
   */
  public static Bundle createBundle(
      PathogenTest pathogenTest,
      NotificationType notificationType,
      boolean featureFlagSnapshot5_3_0Active) {

    // DTOs
    final NotifiedPerson notifiedPerson =
        Objects.requireNonNull(pathogenTest.getNotifiedPerson(), "NotifiedPerson must not be null");
    final PathogenDTO pathogenDTO = pathogenTest.getPathogenDTO();
    final NotificationLaboratoryCategory notificationLaboratoryCategory =
        pathogenTest.getNotificationCategory();
    final NotifierFacility notifierFacility = pathogenTest.getNotifierFacility();
    final NotifiedPersonAddressInfo currentAddress =
        Objects.requireNonNull(
            notifiedPerson.getCurrentAddress(), "CurrentAddress must not be null");
    boolean isNotifiedPersonFacility =
        AddressType.SUBMITTING_FACILITY.equals(currentAddress.getAddressType());

    // hl7 pojos
    if (pathogenTest.getSubmittingFacility() == null) {
      throw new IllegalArgumentException("Submitting facility must not be null");
    }
    final PractitionerRole notifierRole = createNotifierPractitionerRole(notifierFacility);
    final PractitionerRole submittingRole =
        createSubmitterPractitionerRole(
            pathogenTest.getSubmittingFacility(), isNotifiedPersonFacility);

    // check for notification type and use the appropriate bundleBuilder builder
    final NotificationBundleLaboratoryDataBuilder bundleBuilder =
        switch (notificationType) {
          case NON_NOMINAL -> new NonNominalBundleBuilder().setDefaults();
          case ANONYMOUS -> new AnonymousBundleBuilder().setDefaults();
          case NOMINAL -> new NotificationBundleLaboratoryDataBuilder().setDefaults();
        };
    final Patient patient = createPatient(bundleBuilder, pathogenTest, submittingRole);

    List<Observation> observation = new ArrayList<>();
    List<Specimen> specimenList = new ArrayList<>();

    specimenList.addAll(
        createSpecimen(
            pathogenDTO,
            patient,
            submittingRole,
            observation,
            notificationLaboratoryCategory,
            featureFlagSnapshot5_3_0Active));

    final DiagnosticReport diagnosticReport =
        createDiagnosticReport(
            pathogenDTO, patient, observation, notificationLaboratoryCategory, notificationType);

    return bundleBuilder
        .setPathogenDetection(observation)
        .setSubmitterRole(submittingRole)
        .setNotifierRole(notifierRole)
        .setNotifiedPerson(patient)
        .setSpecimen(specimenList)
        .setNotificationLaboratory(
            createComposition(
                patient,
                notifierRole,
                diagnosticReport,
                notificationLaboratoryCategory,
                notificationType))
        .setLaboratoryReport(diagnosticReport)
        .build();
  }
}
