/*
 * Copyright [2023], gematik GmbH
 *
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
 */

package de.gematik.demis.notificationgateway.domain.laboratory.fhir;

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.PROFILE_NOTIFICATION_LABORATORY;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.SYSTEM_LOINC;

import de.gematik.demis.notificationgateway.common.dto.Diagnosis;
import de.gematik.demis.notificationgateway.common.services.fhir.CompositionCreationService;
import de.gematik.demis.notificationgateway.common.utils.DateUtils;
import de.gematik.demis.notificationgateway.common.utils.ReferenceUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Composition.SectionComponent;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.springframework.stereotype.Service;

@Service
public class NotificationLaboratoryCreationService extends CompositionCreationService {

  private static final Coding LOINC_11502_2_Coding =
      new Coding().setSystem(SYSTEM_LOINC).setCode("11502-2").setDisplay("Laboratory report");

  public Composition createNotificationLaboratory(
      Diagnosis diagnosticContent,
      Patient notifiedPerson,
      PractitionerRole notifierRole,
      DiagnosticReport diagnosticReport) {
    Composition notificationLaboratory =
        createComposition(
            PROFILE_NOTIFICATION_LABORATORY,
            "Erregernachweismeldung",
            LOINC_11502_2_Coding,
            notifiedPerson,
            notifierRole);

    notificationLaboratory.setDate(DateUtils.createDate(diagnosticContent.getReceivedDate()));
    addSection(notificationLaboratory, diagnosticReport);

    return notificationLaboratory;
  }

  private void addSection(Composition notificationLaboratory, DiagnosticReport diagnosticReport) {
    final SectionComponent sectionComponent = notificationLaboratory.addSection();

    sectionComponent.setCode(new CodeableConcept(LOINC_11502_2_Coding));
    sectionComponent.addEntry(ReferenceUtils.createReference(diagnosticReport));
  }
}
