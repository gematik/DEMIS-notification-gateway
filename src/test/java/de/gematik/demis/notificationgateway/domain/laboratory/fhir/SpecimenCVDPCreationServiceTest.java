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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.gematik.demis.notificationgateway.common.constants.FhirConstants;
import de.gematik.demis.notificationgateway.common.dto.Diagnosis;
import de.gematik.demis.notificationgateway.common.dto.QuickTest;
import de.gematik.demis.notificationgateway.utils.FileUtils;
import java.util.UUID;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Specimen;
import org.hl7.fhir.r4.model.Specimen.SpecimenStatus;
import org.junit.jupiter.api.Test;

class SpecimenCVDPCreationServiceTest {

  private final SpecimenCVDPCreationService creationService = new SpecimenCVDPCreationService();

  @Test
  void testSpecimenContainsGeneralFixedData() throws JsonProcessingException {
    final QuickTest quickTest =
        FileUtils.createQuickTest("portal/laboratory/notification_content_min.json");
    final Diagnosis diagnosticInfo = quickTest.getDiagnostic();
    final Specimen specimen =
        creationService.createSpecimenCVDP(diagnosticInfo, new Patient(), new PractitionerRole());

    assertTrue(specimen.hasId());

    assertTrue(specimen.hasMeta());
    final Meta meta = specimen.getMeta();
    assertTrue(meta.hasProfile());
    assertEquals(FhirConstants.PROFILE_SPECIMEN_CVDP, meta.getProfile().get(0).asStringValue());

    assertTrue(specimen.hasStatus());
    assertEquals(SpecimenStatus.AVAILABLE, specimen.getStatus());

    assertTrue(specimen.hasType());
    final CodeableConcept type = specimen.getType();
    assertEquals(1, type.getCoding().size());
    final Coding typeCoding = type.getCoding().get(0);
    assertEquals(FhirConstants.SYSTEM_SNOMED, typeCoding.getSystem());
    assertEquals("309164002", typeCoding.getCode());
    assertEquals("Upper respiratory swab sample (specimen)", typeCoding.getDisplay());

    assertTrue(specimen.hasSubject());
    final Reference subject = specimen.getSubject();
    assertTrue(subject.hasReference());

    assertTrue(specimen.hasReceivedTime());
    assertEquals(
        diagnosticInfo.getReceivedDate().toInstant(), specimen.getReceivedTime().toInstant());

    assertTrue(specimen.hasCollection());
    final Reference collector = specimen.getCollection().getCollector();
    assertTrue(collector.hasReference());
  }

  @Test
  void testSpecimenContainsTimeFromDiagnosticInfo() throws JsonProcessingException {
    final QuickTest quickTest =
        FileUtils.createQuickTest("portal/laboratory/notification_content_min.json");
    final Diagnosis diagnosticInfo = quickTest.getDiagnostic();
    final Specimen specimen =
        creationService.createSpecimenCVDP(diagnosticInfo, new Patient(), new PractitionerRole());

    assertTrue(specimen.hasReceivedTime());
    assertEquals(
        diagnosticInfo.getReceivedDate().toInstant(), specimen.getReceivedTime().toInstant());
  }

  @Test
  void testSpecimenContainsIdsInReference() throws JsonProcessingException {
    final QuickTest quickTest =
        FileUtils.createQuickTest("portal/laboratory/notification_content_min.json");

    final Patient notifiedPerson = new Patient();
    final String notifiedPersonId = UUID.randomUUID().toString();
    notifiedPerson.setId(notifiedPersonId);

    final PractitionerRole submittingRole = new PractitionerRole();
    final String submittingRoleId = UUID.randomUUID().toString();
    submittingRole.setId(submittingRoleId);

    final Specimen specimen =
        creationService.createSpecimenCVDP(
            quickTest.getDiagnostic(), notifiedPerson, submittingRole);

    final Reference subject = specimen.getSubject();
    assertTrue(subject.hasReference());
    assertThat(subject.getReference()).contains(notifiedPersonId);

    final Reference collector = specimen.getCollection().getCollector();
    assertTrue(collector.hasReference());
    assertThat(collector.getReference()).contains(submittingRoleId);
  }
}
