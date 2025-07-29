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

import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.laboratory.LaboratoryReportDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.laboratory.LaboratoryReportNonNominalDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.utils.Utils;
import de.gematik.demis.notificationgateway.common.dto.NotificationLaboratoryCategory;
import de.gematik.demis.notificationgateway.common.dto.PathogenDTO;
import de.gematik.demis.notificationgateway.common.enums.NotificationType;
import de.gematik.demis.notificationgateway.common.utils.PropertyUtil;
import java.util.List;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;

/**
 * Utility class for creating FHIR {@link DiagnosticReport} objects.
 *
 * <p>This class provides methods to build a {@link DiagnosticReport} instance based on the provided
 * {@link PathogenDTO}, {@link Patient}, and other related data.
 */
public class LaboratoryReportCreator {
  /**
   * Private constructor to prevent instantiation of this utility class.
   *
   * <p>This class is designed as a utility class and should not be instantiated.
   */
  private LaboratoryReportCreator() {}

  /**
   * Creates a FHIR {@link DiagnosticReport} object using the provided parameters.
   *
   * @param pathogenDTO The {@link PathogenDTO} object containing information about the pathogen.
   * @param patient The {@link Patient} object representing the notified person.
   * @param observationList A list of {@link Observation} objects related to the diagnostic report.
   * @param notificationCategory The {@link NotificationLaboratoryCategory} object containing
   *     details about the notification category.
   * @param notificationType The {@link NotificationType} enum indicating the type of laboratory
   *     notification (e.g., ANONYMOUS, NON_NOMINAL, LAB).
   * @return A {@link DiagnosticReport} object populated with the provided data.
   */
  public static DiagnosticReport createDiagnosticReport(
      PathogenDTO pathogenDTO,
      Patient patient,
      List<Observation> observationList,
      NotificationLaboratoryCategory notificationCategory,
      NotificationType notificationType) {

    final String value = notificationCategory.getReportStatus().getValue();
    final String conclusionCode = notificationCategory.getInterpretation();
    final String laboratoryOrderId = notificationCategory.getLaboratoryOrderId();

    final LaboratoryReportDataBuilder laboratoryReportDataBuilder =
        switch (notificationType) {
          case ANONYMOUS -> new LaboratoryReportNonNominalDataBuilder();
          case NON_NOMINAL -> new LaboratoryReportNonNominalDataBuilder();
          case NOMINAL -> new LaboratoryReportDataBuilder();
        };

    String codeVersion = PropertyUtil.getProperty("lab.notification.category.version");
    codeVersion = codeVersion == null ? "" : codeVersion; // fallback

    laboratoryReportDataBuilder
        .setDefaultData()
        .setStatus(DiagnosticReport.DiagnosticReportStatus.fromCode(value))
        .setCodeCode(pathogenDTO.getCodeDisplay().getCode())
        .setCodeDisplay(pathogenDTO.getCodeDisplay().getDisplay())
        .setCodeVersion(codeVersion)
        .setIssued(Utils.getCurrentDate())
        .setProfileUrlHelper(pathogenDTO.getCodeDisplay().getCode())
        .setNotifiedPerson(patient)
        .setConclusionCodeStatusToNotDetected()
        .setConclusion(conclusionCode)
        .setPathogenDetections(observationList);

    if (laboratoryOrderId != null && !laboratoryOrderId.isEmpty()) {
      laboratoryReportDataBuilder.addBasedOn(laboratoryOrderId);
    }

    for (Observation obs : observationList) {
      for (CodeableConcept interpretation : obs.getInterpretation()) {
        for (Coding code : interpretation.getCoding()) {
          if (("POS").equals(code.getCode())) {
            laboratoryReportDataBuilder.setConclusionCodeStatusToDetected();
            return laboratoryReportDataBuilder.build();
          }
        }
      }
    }

    return laboratoryReportDataBuilder.build();
  }
}
