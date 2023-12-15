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

package de.gematik.demis.notificationgateway.domain.hospitalization.fhir;

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.NAMING_SYSTEM_NOTIFICATION_ID;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.PROFILE_NOTIFICATION_DISEASE;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.SYSTEM_LOINC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.demis.notificationgateway.common.utils.ConfiguredCodeSystems;
import java.util.List;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Composition.CompositionStatus;
import org.hl7.fhir.r4.model.Composition.SectionComponent;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Test;

class NotificationDiseaseCreationServiceTest {

  NotificationDiseaseCreationService creationService = new NotificationDiseaseCreationService();

  @Test
  void testNotificationDiseaseContainsGeneralFixedData() {
    final Composition notificationDisease =
        creationService.createNotificationDisease(
            new Patient(),
            new PractitionerRole(),
            new Condition(),
            new QuestionnaireResponse(),
            new QuestionnaireResponse());

    assertEquals(
        PROFILE_NOTIFICATION_DISEASE,
        notificationDisease.getMeta().getProfile().get(0).asStringValue());
    assertEquals(NAMING_SYSTEM_NOTIFICATION_ID, notificationDisease.getIdentifier().getSystem());
    assertEquals(CompositionStatus.FINAL, notificationDisease.getStatus());

    final Coding typeCoding = notificationDisease.getType().getCoding().get(0);
    assertEquals(SYSTEM_LOINC, typeCoding.getSystem());
    assertEquals("34782-3", typeCoding.getCode());
    assertEquals("Infectious disease Note", typeCoding.getDisplay());

    final ConfiguredCodeSystems codeSystems = ConfiguredCodeSystems.getInstance();
    final Coding categoryCoding = notificationDisease.getCategory().get(0).getCoding().get(0);
    assertEquals(codeSystems.getNotificationTypeCoding("6.1_2"), categoryCoding);

    assertTrue(notificationDisease.hasSubject());
    assertTrue(notificationDisease.getSubject().hasReference());

    assertTrue(notificationDisease.hasDate());

    final Reference author = notificationDisease.getAuthor().get(0);
    assertTrue(author.hasReference());

    assertEquals("Meldung gemäß §6 Absatz 1, 2 IfSG", notificationDisease.getTitle());

    final List<SectionComponent> sections = notificationDisease.getSection();
    assertEquals(3, sections.size());

    final SectionComponent diagnosisSection = sections.get(0);
    assertEquals("Diagnose", diagnosisSection.getTitle());
    final Coding diagnosisCodeCoding = diagnosisSection.getCode().getCoding().get(0);
    assertEquals(codeSystems.getSectionCodeCoding("diagnosis"), diagnosisCodeCoding);
    final List<Reference> diagnosisEntries = diagnosisSection.getEntry();
    assertEquals(1, diagnosisEntries.size());

    final SectionComponent diseaseCommonSection = sections.get(1);
    assertEquals(
        "Meldetatbestandsübergreifende klinische und epidemiologische Angaben",
        diseaseCommonSection.getTitle());
    final Coding diseaseCommonCodeCoding = diseaseCommonSection.getCode().getCoding().get(0);
    assertEquals(
        codeSystems.getSectionCodeCoding("generalClinAndEpiInformation"), diseaseCommonCodeCoding);
    final List<Reference> diseaseCommonEntries = diseaseCommonSection.getEntry();
    assertEquals(1, diseaseCommonEntries.size());

    final SectionComponent diseaseCVDDSection = sections.get(2);
    assertEquals(
        "Meldetatbestandsspezifische klinische und epidemiologische Angaben",
        diseaseCVDDSection.getTitle());
    final Coding diseaseCVDDCodeCoding = diseaseCVDDSection.getCode().getCoding().get(0);
    assertEquals(
        codeSystems.getSectionCodeCoding("specificClinAndEpiInformation"), diseaseCVDDCodeCoding);
    final List<Reference> diseaseCVDDEntries = diseaseCVDDSection.getEntry();
    assertEquals(1, diseaseCVDDEntries.size());
  }
}
