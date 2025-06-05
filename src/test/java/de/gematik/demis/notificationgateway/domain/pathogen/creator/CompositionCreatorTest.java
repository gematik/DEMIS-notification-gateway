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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import de.gematik.demis.notificationgateway.common.dto.NotificationLaboratoryCategory;
import de.gematik.demis.notificationgateway.domain.pathogen.enums.LaboratoryNotificationType;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Test;

class CompositionCreatorTest {

  @Test
  void createCompositionShouldReturnValidCompositionWithLab7_1URL() {
    Patient patient = new Patient();
    patient.setId("Patient/123");
    PractitionerRole practitionerRole = new PractitionerRole();
    practitionerRole.setId("PractitionerRole/123");
    DiagnosticReport diagnosticReport = new DiagnosticReport();
    diagnosticReport.setId("DiagnosticReport/456");
    NotificationLaboratoryCategory notificationCategory = new NotificationLaboratoryCategory();
    notificationCategory.setReportStatus(NotificationLaboratoryCategory.ReportStatusEnum.FINAL);

    Composition composition =
        CompositionCreator.createComposition(
            patient,
            practitionerRole,
            diagnosticReport,
            notificationCategory,
            LaboratoryNotificationType.LAB);

    assertThat(composition.getStatus()).isEqualTo(Composition.CompositionStatus.FINAL);
    assertThat(composition.getTitle()).isEqualTo("Erregernachweismeldung");
    assertThat(composition.getSectionFirstRep().getCode().getCodingFirstRep().getCode())
        .isEqualTo("11502-2");
    assertThat(composition.getMeta().getProfile().getFirst().getValue())
        .isEqualTo("https://demis.rki.de/fhir/StructureDefinition/NotificationLaboratory");
  }

  @Test
  void createCompositionShouldReturnValidCompositionWithAnonymousURL() {
    Patient patient = new Patient();
    patient.setId("Patient/123");
    PractitionerRole practitionerRole = new PractitionerRole();
    practitionerRole.setId("PractitionerRole/123");
    DiagnosticReport diagnosticReport = new DiagnosticReport();
    diagnosticReport.setId("DiagnosticReport/456");
    NotificationLaboratoryCategory notificationCategory = new NotificationLaboratoryCategory();
    notificationCategory.setReportStatus(NotificationLaboratoryCategory.ReportStatusEnum.FINAL);

    Composition composition =
        CompositionCreator.createComposition(
            patient,
            practitionerRole,
            diagnosticReport,
            notificationCategory,
            LaboratoryNotificationType.ANONYMOUS);

    assertThat(composition.getStatus()).isEqualTo(Composition.CompositionStatus.FINAL);
    assertThat(composition.getTitle()).isEqualTo("Erregernachweismeldung");
    assertThat(composition.getSectionFirstRep().getCode().getCodingFirstRep().getCode())
        .isEqualTo("11502-2");
    assertThat(composition.getMeta().getProfile().getFirst().getValue())
        .isEqualTo("https://demis.rki.de/fhir/StructureDefinition/NotificationLaboratoryAnonymous");
  }

  @Test
  void createCompositionShouldReturnValidCompositionWithNonNominalUrl() {
    Patient patient = new Patient();
    patient.setId("Patient/123");
    PractitionerRole practitionerRole = new PractitionerRole();
    practitionerRole.setId("PractitionerRole/123");
    DiagnosticReport diagnosticReport = new DiagnosticReport();
    diagnosticReport.setId("DiagnosticReport/456");
    NotificationLaboratoryCategory notificationCategory = new NotificationLaboratoryCategory();
    notificationCategory.setReportStatus(NotificationLaboratoryCategory.ReportStatusEnum.FINAL);

    Composition composition =
        CompositionCreator.createComposition(
            patient,
            practitionerRole,
            diagnosticReport,
            notificationCategory,
            LaboratoryNotificationType.NON_NOMINAL);

    assertThat(composition.getStatus()).isEqualTo(Composition.CompositionStatus.FINAL);
    assertThat(composition.getTitle()).isEqualTo("Erregernachweismeldung");
    assertThat(composition.getSectionFirstRep().getCode().getCodingFirstRep().getCode())
        .isEqualTo("11502-2");
    assertThat(composition.getMeta().getProfile().getFirst().getValue())
        .isEqualTo(
            "https://demis.rki.de/fhir/StructureDefinition/NotificationLaboratoryNonNominal");
  }

  @Test
  void createCompositionShouldSetRelatesToNotificationIdWhenPresent() {
    Patient patient = new Patient();
    patient.setId("Patient/123");
    PractitionerRole practitionerRole = new PractitionerRole();
    practitionerRole.setId("PractitionerRole/123");
    DiagnosticReport diagnosticReport = new DiagnosticReport();
    diagnosticReport.setId("DiagnosticReport/456");
    NotificationLaboratoryCategory notificationCategory = new NotificationLaboratoryCategory();
    notificationCategory.setReportStatus(NotificationLaboratoryCategory.ReportStatusEnum.FINAL);
    notificationCategory.setInitialNotificationId("12345");

    Composition composition =
        CompositionCreator.createComposition(
            patient,
            practitionerRole,
            diagnosticReport,
            notificationCategory,
            LaboratoryNotificationType.LAB);

    assertThat(
            ((Reference) composition.getRelatesToFirstRep().getTarget()).getIdentifier().getValue())
        .isEqualTo("12345");
  }

  @Test
  void createCompositionShouldHandleNullInitialNotificationIdGracefully() {
    Patient patient = new Patient();
    patient.setId("Patient/123");
    PractitionerRole practitionerRole = new PractitionerRole();
    practitionerRole.setId("PractitionerRole/123");
    DiagnosticReport diagnosticReport = new DiagnosticReport();
    diagnosticReport.setId("DiagnosticReport/456");
    NotificationLaboratoryCategory notificationCategory = new NotificationLaboratoryCategory();
    notificationCategory.setReportStatus(NotificationLaboratoryCategory.ReportStatusEnum.FINAL);
    notificationCategory.setInitialNotificationId(null);

    Composition composition =
        CompositionCreator.createComposition(
            patient,
            practitionerRole,
            diagnosticReport,
            notificationCategory,
            LaboratoryNotificationType.LAB);

    assertThat(composition.getRelatesTo()).isEmpty();
  }

  @Test
  void createCompositionShouldThrowExceptionForInvalidReportStatus() {
    Patient patient = new Patient();
    patient.setId("Patient/123");
    PractitionerRole practitionerRole = new PractitionerRole();
    practitionerRole.setId("PractitionerRole/123");
    DiagnosticReport diagnosticReport = new DiagnosticReport();
    NotificationLaboratoryCategory notificationCategory =
        mock(NotificationLaboratoryCategory.class);

    when(notificationCategory.getReportStatus()).thenReturn(null);

    assertThrows(
        NullPointerException.class,
        () ->
            CompositionCreator.createComposition(
                patient,
                practitionerRole,
                diagnosticReport,
                notificationCategory,
                LaboratoryNotificationType.LAB));
  }
}
