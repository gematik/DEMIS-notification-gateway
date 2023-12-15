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

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.SYSTEM_LOINC;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.gematik.demis.notificationgateway.common.constants.FhirConstants;
import de.gematik.demis.notificationgateway.common.dto.Diagnosis;
import de.gematik.demis.notificationgateway.common.dto.QuickTest;
import de.gematik.demis.notificationgateway.utils.FileUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Composition.CompositionStatus;
import org.hl7.fhir.r4.model.Composition.SectionComponent;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.Test;

class NotificationLaboratoryCreationServiceTest {

  private final NotificationLaboratoryCreationService creationService =
      new NotificationLaboratoryCreationService();

  @Test
  void testCompositionContainsGeneralFixedData() throws JsonProcessingException {
    QuickTest quickTest = FileUtils.createQuickTest("portal/laboratory/notification_content.json");
    final Diagnosis diagnosticInfo = quickTest.getDiagnostic();

    final Composition composition =
        creationService.createNotificationLaboratory(
            diagnosticInfo, new Patient(), new PractitionerRole(), new DiagnosticReport());

    assertThat(composition)
        .matches(Resource::hasId)
        .matches(Composition::hasStatus)
        .matches(Composition::hasIdentifier)
        .matches(Composition::hasType)
        .matches(Composition::hasCategory)
        .matches(Composition::hasSubject)
        .matches(Composition::hasAuthor)
        .matches(Resource::hasMeta);
    assertThat(composition.getTitle()).isEqualTo("Erregernachweismeldung");
    assertThat(composition.getStatus()).isEqualTo(CompositionStatus.FINAL);
    assertThat(composition.getIdentifier().getSystem())
        .isEqualTo(FhirConstants.NAMING_SYSTEM_NOTIFICATION_ID);
    assertThat(composition.getMeta()).matches(Meta::hasProfile);
    assertThat(composition.getMeta().getProfile())
        .isNotEmpty()
        .element(0)
        .matches(
            canonicalType ->
                FhirConstants.PROFILE_NOTIFICATION_LABORATORY.equals(
                    canonicalType.asStringValue()));

    assertThat(composition.getType().getCoding())
        .isNotEmpty()
        .hasSize(1)
        .element(0)
        .matches(coding -> SYSTEM_LOINC.equals(coding.getSystem()))
        .matches(coding -> "34782-3".equals(coding.getCode()))
        .matches(coding -> "Infectious disease Note".equals(coding.getDisplay()));

    assertThat(composition.getCategory())
        .isNotEmpty()
        .hasSize(1)
        .element(0)
        .satisfies(
            codeableConcept ->
                assertThat(codeableConcept.getCoding())
                    .isNotEmpty()
                    .hasSize(1)
                    .element(0)
                    .matches(coding -> SYSTEM_LOINC.equals(coding.getSystem()))
                    .matches(coding -> "11502-2".equals(coding.getCode()))
                    .matches(coding -> "Laboratory report".equals(coding.getDisplay())));

    assertThat(composition.getSubject()).matches(Reference::hasReference);
    assertThat(composition.getDate().toInstant())
        .isEqualTo(diagnosticInfo.getReceivedDate().toInstant());
    assertThat(composition.getAuthor()).hasSize(1).element(0).matches(Reference::hasReference);
    assertThat(composition.getSection()).hasSize(1);

    final SectionComponent sectionComponent = composition.getSection().get(0);
    assertThat(sectionComponent)
        .isNotNull()
        .extracting(SectionComponent::getCode)
        .isNotNull()
        .extracting(CodeableConcept::getCoding)
        .satisfies(
            codings ->
                assertThat(codings)
                    .hasSize(1)
                    .element(0)
                    .matches(coding -> SYSTEM_LOINC.equals(coding.getSystem()))
                    .matches(coding -> "11502-2".equals(coding.getCode()))
                    .matches(coding -> "Laboratory report".equals(coding.getDisplay())));
    assertThat(sectionComponent.getEntry()).hasSize(1).element(0).matches(Reference::hasReference);
  }
}
