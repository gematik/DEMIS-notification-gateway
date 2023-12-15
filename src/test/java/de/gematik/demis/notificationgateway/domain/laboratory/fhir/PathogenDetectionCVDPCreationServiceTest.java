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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.gematik.demis.notificationgateway.common.constants.FhirConstants;
import de.gematik.demis.notificationgateway.common.dto.QuickTest;
import de.gematik.demis.notificationgateway.utils.FileUtils;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Specimen;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PathogenDetectionCVDPCreationServiceTest {

  private final PathogenDetectionCVDPCreationService creationService =
      new PathogenDetectionCVDPCreationService();

  @Test
  void testPathogenDetectionContainsGeneralFixedData() throws JsonProcessingException {
    final QuickTest quickTest =
        FileUtils.createQuickTest("portal/laboratory/notification_content_min.json");
    final Observation pathogenDetection =
        creationService.createPathogenDetectionCVDP(
            quickTest.getDiagnostic(), new Patient(), new Specimen());

    assertTrue(pathogenDetection.hasId());

    final Meta meta = pathogenDetection.getMeta();
    assertTrue(meta.hasProfile());
    assertEquals(
        FhirConstants.PROFILE_PATHOGEN_DETECTION_CVDP, meta.getProfile().get(0).asStringValue());

    assertEquals(ObservationStatus.FINAL, pathogenDetection.getStatus());

    assertTrue(pathogenDetection.hasCategory());
    assertEquals(1, pathogenDetection.getCategory().size());
    final List<Coding> categoryCodings = pathogenDetection.getCategory().get(0).getCoding();
    assertEquals(1, categoryCodings.size());
    final Coding categoryCoding = categoryCodings.get(0);
    assertEquals(FhirConstants.CODE_SYSTEM_OBSERVATION_CATEGORY, categoryCoding.getSystem());
    assertEquals("laboratory", categoryCoding.getCode());
    assertEquals("Laboratory", categoryCoding.getDisplay());

    assertTrue(pathogenDetection.hasCode());
    assertTrue(pathogenDetection.hasMethod());

    assertTrue(pathogenDetection.hasSubject());
    final Reference subject = pathogenDetection.getSubject();
    assertTrue(subject.hasReference());

    assertTrue(pathogenDetection.hasValue());

    assertTrue(pathogenDetection.hasInterpretation());
    assertEquals(1, pathogenDetection.getInterpretation().size());
    final List<Coding> interpretationCodings =
        pathogenDetection.getInterpretation().get(0).getCoding();
    assertEquals(1, interpretationCodings.size());
    final Coding interpretationCoding = interpretationCodings.get(0);
    assertEquals(
        FhirConstants.CODE_SYSTEM_OBSERVATION_INTERPRETATION, interpretationCoding.getSystem());
    assertEquals("POS", interpretationCoding.getCode());

    assertTrue(pathogenDetection.hasSpecimen());
    final Reference specimen = pathogenDetection.getSpecimen();
    assertTrue(specimen.hasReference());
  }

  @Test
  void testPathogenDetectionContainsAntigenTestValues() throws JsonProcessingException {
    final QuickTest quickTest =
        FileUtils.createQuickTest("portal/laboratory/diagnostic/notification_content_antigen.json");
    final Observation pathogenDetection =
        creationService.createPathogenDetectionCVDP(
            quickTest.getDiagnostic(), new Patient(), new Specimen());

    final CodeableConcept code = pathogenDetection.getCode();
    assertEquals(1, code.getCoding().size());
    final Coding codeCoding = code.getCoding().get(0);
    assertEquals(SYSTEM_LOINC, codeCoding.getSystem());
    assertEquals("94558-4", codeCoding.getCode());
    assertEquals(
        "SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay",
        codeCoding.getDisplay());
    assertFalse(code.hasText());

    assertEquals("Positiv", pathogenDetection.getValueStringType().asStringValue());

    assertTrue(pathogenDetection.hasMethod());
    final List<Coding> methodCodings = pathogenDetection.getMethod().getCoding();
    assertEquals(1, methodCodings.size());
    final Coding methodCoding = methodCodings.get(0);
    assertEquals("121276004", methodCoding.getCode());
    assertEquals("Antigen assay (procedure)", methodCoding.getDisplay());
  }

  @Test
  void testPathogenDetectionContainsNucleicAcidTestValues() throws JsonProcessingException {
    final QuickTest quickTest =
        FileUtils.createQuickTest(
            "portal/laboratory/diagnostic/notification_content_nucleic_acid.json");
    final Observation pathogenDetection =
        creationService.createPathogenDetectionCVDP(
            quickTest.getDiagnostic(), new Patient(), new Specimen());

    final CodeableConcept code = pathogenDetection.getCode();
    assertEquals(1, code.getCoding().size());
    final Coding codeCoding = code.getCoding().get(0);
    assertEquals(SYSTEM_LOINC, codeCoding.getSystem());
    assertEquals("95406-5", codeCoding.getCode());
    assertEquals(
        "SARS-CoV-2 (COVID-19) RNA [Presence] in Nose by NAA with probe detection",
        codeCoding.getDisplay());
    assertFalse(code.hasText());

    assertEquals("Positiv", pathogenDetection.getValueStringType().asStringValue());

    assertTrue(pathogenDetection.hasMethod());
    final List<Coding> methodCodings = pathogenDetection.getMethod().getCoding();
    assertEquals(1, methodCodings.size());
    final Coding methodCoding = methodCodings.get(0);
    assertEquals("398545005", methodCoding.getCode());
    assertEquals("Nucleic acid assay (procedure)", methodCoding.getDisplay());
  }

  @Test
  void testPathogenDetectionContainsPCRRapidTestValues() throws JsonProcessingException {
    final QuickTest quickTest =
        FileUtils.createQuickTest(
            "portal/laboratory/diagnostic/notification_content_pcr_rapid.json");
    final Observation pathogenDetection =
        creationService.createPathogenDetectionCVDP(
            quickTest.getDiagnostic(), new Patient(), new Specimen());

    final CodeableConcept code = pathogenDetection.getCode();
    assertEquals(1, code.getCoding().size());
    final Coding codeCoding = code.getCoding().get(0);
    assertEquals(SYSTEM_LOINC, codeCoding.getSystem());
    assertEquals("94746-5", codeCoding.getCode());
    assertEquals(
        "SARS-CoV-2 (COVID-19) RNA [Cycle Threshold #] in Specimen by NAA with probe detection",
        codeCoding.getDisplay());
    assertFalse(code.hasText());

    assertEquals("PCR-Schnelltest positiv", pathogenDetection.getValueStringType().asStringValue());

    assertFalse(pathogenDetection.hasMethod());
  }

  @Test
  void testPathogenDetectionContainsVariantSpecificPCRTestValues() throws JsonProcessingException {
    final QuickTest quickTest =
        FileUtils.createQuickTest(
            "portal/laboratory/diagnostic/notification_content_variantspecific_pcr.json");
    final Observation pathogenDetection =
        creationService.createPathogenDetectionCVDP(
            quickTest.getDiagnostic(), new Patient(), new Specimen());

    final CodeableConcept code = pathogenDetection.getCode();
    assertEquals(1, code.getCoding().size());
    final Coding codeCoding = code.getCoding().get(0);
    assertEquals(SYSTEM_LOINC, codeCoding.getSystem());
    assertEquals("96751-3", codeCoding.getCode());
    assertEquals(
        "SARS-CoV-2 (COVID-19) S gene mutation detected [Identifier] in Specimen by Molecular genetics method",
        codeCoding.getDisplay());
    assertFalse(code.hasText());

    assertEquals(
        "Variante nicht bestimmbar", pathogenDetection.getValueStringType().asStringValue());

    assertTrue(pathogenDetection.hasMethod());
    final List<Coding> methodCodings = pathogenDetection.getMethod().getCoding();
    assertEquals(1, methodCodings.size());
    final Coding methodCoding = methodCodings.get(0);
    assertEquals("69363007", methodCoding.getCode());
    assertEquals("Nucleic acid amplification (procedure)", methodCoding.getDisplay());
  }

  @Test
  void testPathogenDetectionContainsSequencingTestValues() throws JsonProcessingException {
    final QuickTest quickTest =
        FileUtils.createQuickTest(
            "portal/laboratory/diagnostic/notification_content_sequencing.json");
    final Observation pathogenDetection =
        creationService.createPathogenDetectionCVDP(
            quickTest.getDiagnostic(), new Patient(), new Specimen());

    final CodeableConcept code = pathogenDetection.getCode();
    assertEquals(1, code.getCoding().size());
    final Coding codeCoding = code.getCoding().get(0);
    assertEquals(SYSTEM_LOINC, codeCoding.getSystem());
    assertEquals("96741-4", codeCoding.getCode());
    assertEquals(
        "SARS-CoV-2 (COVID-19) variant [Type] in Specimen by Sequencing", codeCoding.getDisplay());
    assertFalse(code.hasText());

    assertEquals(
        "Variante nicht bestimmbar", pathogenDetection.getValueStringType().asStringValue());

    assertTrue(pathogenDetection.hasMethod());
    final List<Coding> methodCodings = pathogenDetection.getMethod().getCoding();
    assertEquals(1, methodCodings.size());
    final Coding methodCoding = methodCodings.get(0);
    assertEquals("117040002", methodCoding.getCode());
    assertEquals("Nucleic acid sequencing (procedure)", methodCoding.getDisplay());
  }

  @Test
  void testPathogenDetectionContainsIdsInReferences() throws JsonProcessingException {
    final QuickTest quickTest =
        FileUtils.createQuickTest("portal/laboratory/notification_content_min.json");

    final Patient notifiedPerson = new Patient();
    final String notifiedPersonId = UUID.randomUUID().toString();
    notifiedPerson.setId(notifiedPersonId);

    final Specimen specimen = new Specimen();
    final String specimenId = UUID.randomUUID().toString();
    specimen.setId(specimenId);

    final Observation pathogenDetection =
        creationService.createPathogenDetectionCVDP(
            quickTest.getDiagnostic(), notifiedPerson, specimen);

    assertTrue(pathogenDetection.hasSubject());
    final Reference subject = pathogenDetection.getSubject();
    assertTrue(subject.hasReference());
    assertThat(subject.getReference()).contains(notifiedPersonId);

    assertTrue(pathogenDetection.hasSpecimen());
    final Reference specimenReference = pathogenDetection.getSpecimen();
    assertTrue(specimenReference.hasReference());
    assertThat(specimenReference.getReference()).contains(specimenId);
  }

  @SuppressWarnings("unused")
  private static Stream<Arguments> provideVocValues() {
    return Stream.of(
        Arguments.of(
            "portal/laboratory/diagnostic/notification_content_voc_alpha.json", "#B.1.1.7"),
        Arguments.of("portal/laboratory/diagnostic/notification_content_voc_beta.json", "#B.1.351"),
        Arguments.of(
            "portal/laboratory/diagnostic/notification_content_voc_gamma.json", "#B.1.1.28.1"),
        Arguments.of(
            "portal/laboratory/diagnostic/notification_content_voc_delta.json", "#B.1.617.2"),
        Arguments.of(
            "portal/laboratory/diagnostic/notification_content_voc_omikron.json", "#B.1.1.529"));
  }

  @ParameterizedTest
  @MethodSource("provideVocValues")
  void testPathogenDetectionContainsCorrectVoc(String inputFile, String expectedVocValue)
      throws JsonProcessingException {
    final QuickTest quickTest = FileUtils.createQuickTest(inputFile);
    final Observation pathogenDetection =
        creationService.createPathogenDetectionCVDP(
            quickTest.getDiagnostic(), new Patient(), new Specimen());

    assertEquals(expectedVocValue, pathogenDetection.getValue().toString());
  }
}
