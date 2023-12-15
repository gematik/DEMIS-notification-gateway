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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.gematik.demis.notificationgateway.common.constants.FhirConstants;
import de.gematik.demis.notificationgateway.common.dto.Diagnosis;
import de.gematik.demis.notificationgateway.common.dto.QuickTest;
import de.gematik.demis.notificationgateway.utils.FileUtils;
import java.util.List;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.DiagnosticReport.DiagnosticReportStatus;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.Test;

class LaboratoryReportCVDPCreationServiceTest {

  private final LaboratoryReportCVDPCreationService creationService =
      new LaboratoryReportCVDPCreationService();

  @Test
  void testDiagnosticReportContainsGeneralFixedData() throws JsonProcessingException {
    QuickTest quickTest =
        FileUtils.createQuickTest("portal/laboratory/notification_content_min.json");
    final Diagnosis diagnosticInfo = quickTest.getDiagnostic();

    final DiagnosticReport diagnosticReport =
        creationService.createLaboratoryReportCVDP(
            diagnosticInfo, new Patient(), new Observation());

    assertThat(diagnosticReport)
        .matches(Resource::hasId)
        .matches(DiagnosticReport::hasStatus)
        .matches(DiagnosticReport::hasCode)
        .matches(DiagnosticReport::hasSubject)
        .matches(DiagnosticReport::hasIssued)
        .matches(DiagnosticReport::hasResult)
        .matches(DiagnosticReport::hasConclusionCode)
        .matches(Resource::hasMeta);
    assertThat(diagnosticReport.getStatus()).isEqualTo(DiagnosticReportStatus.FINAL);
    assertThat(diagnosticReport.getMeta()).matches(Meta::hasProfile);
    assertThat(diagnosticReport.getMeta().getProfile())
        .isNotEmpty()
        .element(0)
        .matches(
            canonicalType ->
                FhirConstants.PROFILE_LABORATORY_REPORT_CVDP.equals(canonicalType.asStringValue()));
    assertThat(diagnosticReport.getCode().getCoding())
        .isNotEmpty()
        .hasSize(1)
        .element(0)
        .matches(
            coding -> FhirConstants.CODE_SYSTEM_NOTIFICATION_CATEGORY.equals(coding.getSystem()))
        .matches(coding -> "cvdp".equals(coding.getCode()))
        .matches(
            coding ->
                "Severe-Acute-Respiratory-Syndrome-Coronavirus-2 (SARS-CoV-2)"
                    .equals(coding.getDisplay()));
    assertThat(diagnosticReport.getSubject()).matches(Reference::hasReference);
    assertThat(diagnosticReport.getIssued().toInstant())
        .isEqualTo(diagnosticInfo.getReceivedDate().toInstant());
    assertThat(diagnosticReport.getResult()).hasSize(1).element(0).matches(Reference::hasReference);
    assertThat(diagnosticReport.getConclusionCode()).hasSize(1);
    final List<Coding> conclusionCodeCodings =
        diagnosticReport.getConclusionCode().get(0).getCoding();
    assertThat(conclusionCodeCodings)
        .hasSize(1)
        .element(0)
        .matches(coding -> FhirConstants.CODE_SYSTEM_CONCLUSION_CODE.equals(coding.getSystem()))
        .matches(coding -> "pathogenDetected".equals(coding.getCode()))
        .matches(coding -> "Meldepflichtiger Erreger nachgewiesen".equals(coding.getDisplay()));
  }
}
