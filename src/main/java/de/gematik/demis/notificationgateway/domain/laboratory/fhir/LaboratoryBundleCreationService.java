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

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.PROFILE_NOTIFICATION_BUNDLE_LABORATORY;

import de.gematik.demis.notificationgateway.common.dto.QuickTest;
import de.gematik.demis.notificationgateway.common.services.fhir.BundleCreationService;
import de.gematik.demis.notificationgateway.common.services.fhir.NotifiedPersonCreationService;
import de.gematik.demis.notificationgateway.common.services.fhir.OrganizationCreationService;
import de.gematik.demis.notificationgateway.common.services.fhir.PractitionerRoleCreationService;
import javax.validation.Valid;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Specimen;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LaboratoryBundleCreationService extends BundleCreationService {
  @Autowired private NotifiedPersonCreationService notifiedPersonCreationService;
  @Autowired private OrganizationCreationService organizationCreationService;
  @Autowired private SubmittingFacilityCreationService submittingFacilityCreationService;
  @Autowired private PathogenDetectionCVDPCreationService pathogenDetectionCVDPCreationService;
  @Autowired private SpecimenCVDPCreationService specimenCVDPCreationService;
  @Autowired private LaboratoryReportCVDPCreationService laboratoryReportCVDPCreationService;
  @Autowired private NotificationLaboratoryCreationService notificationLaboratoryCreationService;
  @Autowired private PractitionerRoleCreationService practitionerRoleCreationService;

  public Bundle createLaboratoryBundle(@Valid QuickTest content) {
    final Bundle bundle = createBundle(PROFILE_NOTIFICATION_BUNDLE_LABORATORY);

    final Organization notifierFacility =
        organizationCreationService.createLabNotifierFacility(content.getNotifierFacility());
    final PractitionerRole notifierRole =
        practitionerRoleCreationService.createNotifierRole(notifierFacility);

    final Organization submittingFacility =
        submittingFacilityCreationService.createSubmittingFacility(content.getNotifierFacility());
    final PractitionerRole submittingRole =
        practitionerRoleCreationService.createSubmittingRole(submittingFacility);

    final Patient notifiedPerson =
        notifiedPersonCreationService.createNotifiedPerson(content.getNotifiedPerson());

    final Specimen specimen =
        specimenCVDPCreationService.createSpecimenCVDP(
            content.getDiagnostic(), notifiedPerson, submittingRole);
    final Observation pathogenDetection =
        pathogenDetectionCVDPCreationService.createPathogenDetectionCVDP(
            content.getDiagnostic(), notifiedPerson, specimen);

    final DiagnosticReport diagnosticReport =
        laboratoryReportCVDPCreationService.createLaboratoryReportCVDP(
            content.getDiagnostic(), notifiedPerson, pathogenDetection);
    final Composition notificationLaboratory =
        notificationLaboratoryCreationService.createNotificationLaboratory(
            content.getDiagnostic(), notifiedPerson, notifierRole, diagnosticReport);

    addEntry(bundle, notificationLaboratory);
    addEntry(bundle, notifiedPerson);
    addEntry(bundle, notifierFacility);
    addEntry(bundle, notifierRole);
    addEntry(bundle, submittingFacility);
    addEntry(bundle, submittingRole);
    addEntry(bundle, diagnosticReport);
    addEntry(bundle, pathogenDetection);
    addEntry(bundle, specimen);

    return bundle;
  }
}
