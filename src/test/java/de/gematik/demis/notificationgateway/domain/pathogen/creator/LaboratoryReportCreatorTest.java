package de.gematik.demis.notificationgateway.domain.pathogen.creator;

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

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.gematik.demis.notificationgateway.common.dto.CodeDisplay;
import de.gematik.demis.notificationgateway.common.dto.NotificationLaboratoryCategory;
import de.gematik.demis.notificationgateway.common.dto.PathogenDTO;
import de.gematik.demis.notificationgateway.common.enums.NotificationType;
import java.util.List;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class LaboratoryReportCreatorTest {

  @Test
  void createDiagnosticReportShouldReturnReportWithDetectedStatusWhenPositiveObservationExists() {
    PathogenDTO pathogenDTO = new PathogenDTO();
    pathogenDTO.setCodeDisplay(new CodeDisplay("123").display("Test Pathogen"));
    Patient patient = new Patient();
    patient.setId("Patient/123");
    NotificationLaboratoryCategory notificationCategory = new NotificationLaboratoryCategory();
    notificationCategory.setReportStatus(NotificationLaboratoryCategory.ReportStatusEnum.FINAL);
    notificationCategory.setInterpretation("Positive");
    Observation positiveObservation = new Observation();
    positiveObservation.setId("Observation/456");
    Coding positiveCoding = new Coding().setCode("POS");
    CodeableConcept positiveInterpretation = new CodeableConcept().addCoding(positiveCoding);
    positiveObservation.setInterpretation(List.of(positiveInterpretation));

    DiagnosticReport report =
        LaboratoryReportCreator.createDiagnosticReport(
            pathogenDTO,
            patient,
            List.of(positiveObservation),
            notificationCategory,
            NotificationType.NOMINAL);

    assertThat(report.getConclusion()).isEqualTo("Positive");
    assertThat(report.getStatus()).isEqualTo(DiagnosticReport.DiagnosticReportStatus.FINAL);
  }

  @Test
  void
      createDiagnosticReportShouldReturnReportWithNotDetectedStatusWhenNoPositiveObservationExists() {
    PathogenDTO pathogenDTO = new PathogenDTO();
    pathogenDTO.setCodeDisplay(new CodeDisplay("123").display("Test Pathogen"));
    Patient patient = new Patient();
    patient.setId("Patient/123");
    NotificationLaboratoryCategory notificationCategory = new NotificationLaboratoryCategory();
    notificationCategory.setReportStatus(NotificationLaboratoryCategory.ReportStatusEnum.FINAL);
    notificationCategory.setInterpretation("Negative");
    Observation negativeObservation = new Observation();
    negativeObservation.setId("Observation/456");
    Coding negativeCoding = new Coding().setCode("POS");
    CodeableConcept negativeInterpretation = new CodeableConcept().addCoding(negativeCoding);
    negativeObservation.setInterpretation(List.of(negativeInterpretation));

    DiagnosticReport report =
        LaboratoryReportCreator.createDiagnosticReport(
            pathogenDTO,
            patient,
            List.of(negativeObservation),
            notificationCategory,
            NotificationType.NOMINAL);

    assertThat(report.getConclusion()).isEqualTo("Negative");
    assertThat(report.getStatus()).isEqualTo(DiagnosticReport.DiagnosticReportStatus.FINAL);
  }

  @Test
  void createDiagnosticReportShouldIncludeLaboratoryOrderIdWhenProvided() {
    PathogenDTO pathogenDTO = new PathogenDTO();
    pathogenDTO.setCodeDisplay(new CodeDisplay("123").display("Test Pathogen"));
    Patient patient = new Patient();
    patient.setId("Patient/123");
    patient.setId("Patient/123");
    NotificationLaboratoryCategory notificationCategory = new NotificationLaboratoryCategory();
    notificationCategory.setReportStatus(NotificationLaboratoryCategory.ReportStatusEnum.FINAL);
    notificationCategory.setInterpretation("Negative");
    notificationCategory.setLaboratoryOrderId("LAB123");
    Observation observation = new Observation();
    observation.setId("Observation/456");
    Coding coding = new Coding().setCode("POS");
    CodeableConcept interpretation = new CodeableConcept().addCoding(coding);
    observation.setInterpretation(List.of(interpretation));

    DiagnosticReport report =
        LaboratoryReportCreator.createDiagnosticReport(
            pathogenDTO,
            patient,
            List.of(observation),
            notificationCategory,
            NotificationType.NOMINAL);

    assertThat(report.getBasedOnFirstRep().getIdentifier().getValue()).isEqualTo("LAB123");
  }

  @Test
  void createDiagnosticReportShouldHandleNullLaboratoryOrderIdGracefully() {
    PathogenDTO pathogenDTO = new PathogenDTO();
    pathogenDTO.setCodeDisplay(new CodeDisplay("123").display("Test Pathogen"));
    Patient patient = new Patient();
    patient.setId("Patient/123");
    patient.setId("Patient/123");
    NotificationLaboratoryCategory notificationCategory = new NotificationLaboratoryCategory();
    notificationCategory.setReportStatus(NotificationLaboratoryCategory.ReportStatusEnum.FINAL);
    notificationCategory.setInterpretation("Negative");
    notificationCategory.setLaboratoryOrderId(null);
    Observation observation = new Observation();
    observation.setId("Observation/456");
    Coding coding = new Coding().setCode("POS");
    CodeableConcept interpretation = new CodeableConcept().addCoding(coding);
    observation.setInterpretation(List.of(interpretation));

    DiagnosticReport report =
        LaboratoryReportCreator.createDiagnosticReport(
            pathogenDTO,
            patient,
            List.of(observation),
            notificationCategory,
            NotificationType.NOMINAL);

    assertThat(report.getBasedOn()).isEmpty();
  }

  @Test
  void createDiagnosticReportShouldThrowExceptionForNullReportStatus() {
    PathogenDTO pathogenDTO = new PathogenDTO();
    pathogenDTO.setCodeDisplay(new CodeDisplay("123").display("Test Pathogen"));
    Patient patient = new Patient();
    patient.setId("Patient/123");
    patient.setId("Patient/123");
    NotificationLaboratoryCategory notificationCategory = new NotificationLaboratoryCategory();
    notificationCategory.setInterpretation("Negative");
    notificationCategory.setLaboratoryOrderId(null);
    Observation observation = new Observation();
    Coding coding = new Coding().setCode("POS");
    CodeableConcept interpretation = new CodeableConcept().addCoding(coding);
    observation.setInterpretation(List.of(interpretation));
    List observations = List.of(observation);
    assertThatThrownBy(
            () ->
                LaboratoryReportCreator.createDiagnosticReport(
                    pathogenDTO,
                    patient,
                    observations,
                    notificationCategory,
                    NotificationType.NOMINAL))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void createDiagnosticReportShouldHandleEmptyObservationList() {
    PathogenDTO pathogenDTO = new PathogenDTO();
    pathogenDTO.setCodeDisplay(new CodeDisplay("123").display("Test Pathogen"));
    Patient patient = new Patient();
    patient.setId("Patient/123");
    patient.setId("Patient/123");
    NotificationLaboratoryCategory notificationCategory = new NotificationLaboratoryCategory();
    notificationCategory.setReportStatus(NotificationLaboratoryCategory.ReportStatusEnum.FINAL);
    notificationCategory.setInterpretation("Negative");

    DiagnosticReport report =
        LaboratoryReportCreator.createDiagnosticReport(
            pathogenDTO, patient, emptyList(), notificationCategory, NotificationType.NOMINAL);

    assertThat(report.getConclusion()).isNotEqualTo("pathogenDetected");
  }
}
