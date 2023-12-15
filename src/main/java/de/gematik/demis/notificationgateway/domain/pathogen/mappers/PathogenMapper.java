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

package de.gematik.demis.notificationgateway.domain.pathogen.mappers;

import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.laboratory.NotificationBundleLaboratoryDataBuilder;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPerson;
import de.gematik.demis.notificationgateway.common.dto.NotifierFacility;
import de.gematik.demis.notificationgateway.common.dto.PathogenDTO;
import de.gematik.demis.notificationgateway.common.dto.PathogenTest;
import de.gematik.demis.notificationgateway.common.mappers.BundleMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Specimen;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PathogenMapper implements BundleMapper {
  public Bundle toBundle(PathogenTest pathogenTest) {
    final NotifiedPerson notifiedPerson = pathogenTest.getNotifiedPerson();
    final PathogenDTO pathogenDTO = pathogenTest.getPathogenDTO();
    /* create patient */
    final Patient patient = createPatient(notifiedPerson);

    final NotifierFacility notifierFacility = pathogenTest.getNotifierFacility();
    /* create notifier role */
    final PractitionerRole notifierRole = createNotifierPractitionerRole(notifierFacility);
    /* create submitting organization */
    final PractitionerRole submittingRole = createSubmitterPractitionerRole(notifierFacility);
    /* create specimen */
    final Specimen specimen = createSpecimen(pathogenDTO, patient, submittingRole);
    /* create observation */
    final List<Observation> observation = createObservation(pathogenDTO, patient, specimen);
    /* create diagnostic report */
    final DiagnosticReport diagnosticReport =
        createDiagnosticReport(pathogenDTO, patient, observation);

    return new NotificationBundleLaboratoryDataBuilder()
        .setDefaults()
        .setPathogenDetection(observation)
        .setSubmitterRole(submittingRole)
        .setNotifierRole(notifierRole)
        .setNotifiedPerson(patient)
        .setSpecimen(specimen)
        .setNotificationLaboratory(
            createComposition(patient, notifierRole, diagnosticReport, pathogenDTO))
        .setLaboratoryReport(diagnosticReport)
        .build();
  }
}
