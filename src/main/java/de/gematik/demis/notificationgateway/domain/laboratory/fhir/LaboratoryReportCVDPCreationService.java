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

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.PROFILE_LABORATORY_REPORT_CVDP;

import de.gematik.demis.notificationgateway.common.dto.Diagnosis;
import de.gematik.demis.notificationgateway.common.utils.ConfiguredCodeSystems;
import de.gematik.demis.notificationgateway.common.utils.DateUtils;
import de.gematik.demis.notificationgateway.common.utils.ReferenceUtils;
import java.util.UUID;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.DiagnosticReport.DiagnosticReportStatus;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Service;

@Service
public class LaboratoryReportCVDPCreationService {

  public DiagnosticReport createLaboratoryReportCVDP(
      Diagnosis diagnosticContent, Patient notifiedPerson, Observation pathogenDetection) {
    DiagnosticReport diagnosticReport = new DiagnosticReport();
    diagnosticReport.setId(UUID.randomUUID().toString());
    diagnosticReport.setMeta(new Meta().addProfile(PROFILE_LABORATORY_REPORT_CVDP));

    diagnosticReport.setStatus(DiagnosticReportStatus.FINAL);
    diagnosticReport.setCode(
        new CodeableConcept(
            ConfiguredCodeSystems.getInstance().getNotificationCategoryCoding("cvdp")));
    diagnosticReport.setSubject(ReferenceUtils.createReference(notifiedPerson));
    diagnosticReport.setIssued(DateUtils.createDate(diagnosticContent.getReceivedDate()));
    diagnosticReport.addResult(ReferenceUtils.createReference(pathogenDetection));
    diagnosticReport.addConclusionCode(
        new CodeableConcept(
            ConfiguredCodeSystems.getInstance().getConclusionCodeCoding("pathogenDetected")));

    return diagnosticReport;
  }
}
