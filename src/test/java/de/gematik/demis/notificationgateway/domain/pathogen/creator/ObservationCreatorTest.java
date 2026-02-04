package de.gematik.demis.notificationgateway.domain.pathogen.creator;

/*-
 * #%L
 * DEMIS Notification-Gateway
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
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
 * For additional notes and disclaimer from gematik and in case of changes by gematik,
 * find details in the "Readme" file.
 * #L%
 */

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import de.gematik.demis.notificationgateway.common.dto.*;
import java.util.HashMap;
import java.util.List;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Specimen;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ObservationCreatorTest {

  @Test
  void shouldCreateObservationsForPathogen() {

    PathogenDTO pathogenDTO = mock(PathogenDTO.class);
    MethodPathogenDTO methodPathogenDTO = mock(MethodPathogenDTO.class);
    Patient patient = new Patient();
    patient.setId("Patient/123");
    Specimen specimen = new Specimen();
    specimen.setId("Specimen/456");
    NotificationLaboratoryCategory notificationCategory =
        mock(NotificationLaboratoryCategory.class);

    when(notificationCategory.getPathogen()).thenReturn(new CodeDisplay("12345"));
    when(pathogenDTO.getCodeDisplay()).thenReturn(new CodeDisplay("67890"));
    when(methodPathogenDTO.getResult()).thenReturn(MethodPathogenDTO.ResultEnum.POS);
    when(methodPathogenDTO.getMethod()).thenReturn(new CodeDisplay("MethodCode").display("Method"));

    List<Observation> observations =
        ObservationCreator.createObservation(
            pathogenDTO, List.of(methodPathogenDTO), patient, specimen, notificationCategory, null);

    assertThat(observations).hasSize(1);
    Observation observation = observations.getFirst();
    assertThat(observation.getMethod().getCodingFirstRep().getCode()).isEqualTo("MethodCode");
    assertThat(observation.getMethod().getCodingFirstRep().getDisplay()).isEqualTo("Method");
    assertThat(observation.getValueCodeableConcept().getCodingFirstRep().getCode())
        .isEqualTo("12345");
    assertThat(observation.getMeta().getProfile().getFirst().getValue())
        .isEqualTo("https://demis.rki.de/fhir/StructureDefinition/PathogenDetection67890");

    // fixed values
    assertThat(observation.getCode().getCodingFirstRep().getCode()).isEqualTo("41852-5");
    assertThat(observation.getCode().getCodingFirstRep().getDisplay())
        .isEqualTo("Microorganism or agent identified in Specimen");
    assertThat(observation.getStatus()).isEqualTo(Observation.ObservationStatus.FINAL);
  }

  @Test
  void shouldCreateObservationsForPathogenWithAnalyt() {

    PathogenDTO pathogenDTO = mock(PathogenDTO.class);
    MethodPathogenDTO methodPathogenDTO = mock(MethodPathogenDTO.class);
    Patient patient = new Patient();
    patient.setId("Patient/123");
    Specimen specimen = new Specimen();
    specimen.setId("Specimen/456");
    NotificationLaboratoryCategory notificationCategory =
        mock(NotificationLaboratoryCategory.class);

    when(notificationCategory.getPathogen())
        .thenReturn(new CodeDisplay("12345").display("12345Display"));
    when(pathogenDTO.getCodeDisplay()).thenReturn(new CodeDisplay("67890"));
    when(methodPathogenDTO.getResult()).thenReturn(MethodPathogenDTO.ResultEnum.POS);
    when(methodPathogenDTO.getMethod()).thenReturn(new CodeDisplay("MethodCode").display("Method"));
    when(methodPathogenDTO.getAnalyt()).thenReturn(new CodeDisplay("AnalytCode").display("Analyt"));

    List<Observation> observations =
        ObservationCreator.createObservation(
            pathogenDTO, List.of(methodPathogenDTO), patient, specimen, notificationCategory, null);

    assertThat(observations).hasSize(2);
    Observation observation = observations.getFirst();
    assertThat(observation.getValueCodeableConcept().getCodingFirstRep().getCode())
        .isEqualTo("12345");
    assertThat(observation.getValueCodeableConcept().getCodingFirstRep().getDisplay())
        .isEqualTo("12345Display");
    assertThat(observation.getMethod().getCodingFirstRep().getCode()).isEqualTo("MethodCode");
    assertThat(observation.getMethod().getCodingFirstRep().getDisplay()).isEqualTo("Method");
    // there is an extra observation when an analyt is given
    Observation observation1 = observations.get(1);
    assertThat(observation1.getCode().getCodingFirstRep().getCode()).isEqualTo("41852-5");
    assertThat(observation1.getCode().getCodingFirstRep().getDisplay())
        .isEqualTo("Microorganism or agent identified in Specimen");
    assertThat(observation1.getValueCodeableConcept().getCodingFirstRep().getCode())
        .isEqualTo("AnalytCode");
    assertThat(observation1.getValueCodeableConcept().getCodingFirstRep().getDisplay())
        .isEqualTo("Analyt");
  }

  @Test
  void shouldCreateAdditionalObservationsWithoutAnalyt() {

    PathogenDTO pathogenDTO = mock(PathogenDTO.class);
    MethodPathogenDTO methodPathogenDTO = mock(MethodPathogenDTO.class);
    MethodPathogenDTO methodPathogenDTO2 = mock(MethodPathogenDTO.class);
    MethodPathogenDTO methodPathogenDTO3 = mock(MethodPathogenDTO.class);
    Patient patient = new Patient();
    patient.setId("Patient/123");
    Specimen specimen = new Specimen();
    specimen.setId("Specimen/456");
    NotificationLaboratoryCategory notificationCategory =
        mock(NotificationLaboratoryCategory.class);

    when(notificationCategory.getPathogen()).thenReturn(new CodeDisplay("12345"));
    when(pathogenDTO.getCodeDisplay()).thenReturn(new CodeDisplay("67890"));
    when(methodPathogenDTO.getResult()).thenReturn(MethodPathogenDTO.ResultEnum.POS);
    when(methodPathogenDTO.getMethod()).thenReturn(new CodeDisplay("MethodCode"));
    when(methodPathogenDTO2.getResult()).thenReturn(MethodPathogenDTO.ResultEnum.NEG);
    when(methodPathogenDTO2.getMethod()).thenReturn(new CodeDisplay("MethodCode2"));
    when(methodPathogenDTO3.getResult()).thenReturn(MethodPathogenDTO.ResultEnum.POS);
    when(methodPathogenDTO3.getMethod()).thenReturn(new CodeDisplay("MethodCode3"));

    List<Observation> observations =
        ObservationCreator.createObservation(
            pathogenDTO,
            List.of(methodPathogenDTO, methodPathogenDTO2, methodPathogenDTO3),
            patient,
            specimen,
            notificationCategory,
            null);

    assertThat(observations).hasSize(3);
    Observation observation = observations.getFirst();
    assertThat(observation.getCode().getCodingFirstRep().getCode()).isEqualTo("41852-5");
    assertThat(observation.getCode().getCodingFirstRep().getDisplay())
        .isEqualTo("Microorganism or agent identified in Specimen");
    Observation observation1 = observations.get(1);
    assertThat(observation1.getCode().getCodingFirstRep().getCode()).isEqualTo("41852-5");
    assertThat(observation1.getCode().getCodingFirstRep().getDisplay())
        .isEqualTo("Microorganism or agent identified in Specimen");
    Observation observation2 = observations.get(2);
    assertThat(observation2.getCode().getCodingFirstRep().getCode()).isEqualTo("41852-5");
    assertThat(observation2.getCode().getCodingFirstRep().getDisplay())
        .isEqualTo("Microorganism or agent identified in Specimen");
  }

  @Test
  void shouldCreateAdditionalObservationsWithMultipleAnalyt() {

    PathogenDTO pathogenDTO = mock(PathogenDTO.class);
    MethodPathogenDTO methodPathogenDTO = mock(MethodPathogenDTO.class);
    MethodPathogenDTO methodPathogenDTO2 = mock(MethodPathogenDTO.class);
    MethodPathogenDTO methodPathogenDTO3 = mock(MethodPathogenDTO.class);
    Patient patient = new Patient();
    patient.setId("Patient/123");
    Specimen specimen = new Specimen();
    specimen.setId("Specimen/456");
    NotificationLaboratoryCategory notificationCategory =
        mock(NotificationLaboratoryCategory.class);

    when(notificationCategory.getPathogen()).thenReturn(new CodeDisplay("12345"));
    when(pathogenDTO.getCodeDisplay()).thenReturn(new CodeDisplay("67890"));
    when(methodPathogenDTO.getResult()).thenReturn(MethodPathogenDTO.ResultEnum.POS);
    when(methodPathogenDTO.getMethod())
        .thenReturn(new CodeDisplay("MethodCode").display("MethodSystem"));
    when(methodPathogenDTO.getAnalyt()).thenReturn(new CodeDisplay("AnalytCode").display("Analyt"));

    when(methodPathogenDTO2.getResult()).thenReturn(MethodPathogenDTO.ResultEnum.NEG);
    when(methodPathogenDTO2.getMethod()).thenReturn(new CodeDisplay("MethodCode2"));
    when(methodPathogenDTO2.getAnalyt())
        .thenReturn(new CodeDisplay("AnalytCode2").display("Analyt2"));

    when(methodPathogenDTO3.getResult()).thenReturn(MethodPathogenDTO.ResultEnum.POS);
    when(methodPathogenDTO3.getMethod()).thenReturn(new CodeDisplay("MethodCode3"));
    when(methodPathogenDTO3.getAnalyt())
        .thenReturn(new CodeDisplay("AnalytCode3").display("Analyt3"));

    List<Observation> observations =
        ObservationCreator.createObservation(
            pathogenDTO,
            List.of(methodPathogenDTO, methodPathogenDTO2, methodPathogenDTO3),
            patient,
            specimen,
            notificationCategory,
            null);

    assertThat(observations).hasSize(4);
    Observation observation = observations.getFirst();
    assertThat(observation.getCode().getCodingFirstRep().getCode()).isEqualTo("41852-5");
    assertThat(observation.getCode().getCodingFirstRep().getDisplay())
        .isEqualTo("Microorganism or agent identified in Specimen");
    Observation observation1 = observations.get(1);
    assertThat(observation1.getCode().getCodingFirstRep().getCode()).isEqualTo("41852-5");
    assertThat(observation1.getCode().getCodingFirstRep().getDisplay())
        .isEqualTo("Microorganism or agent identified in Specimen");
    Observation observation2 = observations.get(2);
    assertThat(observation2.getCode().getCodingFirstRep().getCode()).isEqualTo("41852-5");
    assertThat(observation2.getCode().getCodingFirstRep().getDisplay())
        .isEqualTo("Microorganism or agent identified in Specimen");
    Observation observation3 = observations.get(3);
    assertThat(observation3.getCode().getCodingFirstRep().getCode()).isEqualTo("41852-5");
    assertThat(observation3.getCode().getCodingFirstRep().getDisplay())
        .isEqualTo("Microorganism or agent identified in Specimen");
  }

  @Nested
  class ResistanceTest {

    @Test
    void shouldReturnEmptyCollectionForResistanceNull() {

      assertThat(ObservationCreator.createObservationsForResistances(null, null, null, null, null))
          .isEmpty();
      assertThat(
              ObservationCreator.createObservationsForResistances(
                  List.of(), null, null, null, null))
          .isEmpty();
    }

    @Test
    void shouldCreateObservationsForResistancesResistant() {

      ResistanceDTO resistanceDTO = mock(ResistanceDTO.class);
      Patient patient = new Patient();
      patient.setId("Patient/123");
      Specimen specimen = new Specimen();
      specimen.setId("Specimen/456");

      CodeDisplay system = new CodeDisplay("R123");
      system.setDisplay("Resistance Display");
      when(resistanceDTO.getResistance()).thenReturn(system);
      when(resistanceDTO.getResistanceResult())
          .thenReturn(ResistanceDTO.ResistanceResultEnum.RESISTANT);

      List<Observation> observations =
          ObservationCreator.createObservationsForResistances(
              List.of(resistanceDTO), patient, specimen, "PathogenCode", null);

      assertThat(observations).hasSize(1);
      Observation observation = observations.getFirst();
      assertThat(observation.getCode().getCodingFirstRep().getCode()).isEqualTo("R123");
      assertThat(observation.getCode().getCodingFirstRep().getDisplay())
          .isEqualTo("Resistance Display");
      assertThat(observation.getInterpretationFirstRep().getCodingFirstRep().getCode())
          .isEqualTo("R");
      assertThat(observation.getValueCodeableConcept().getCodingFirstRep().getCode())
          .isEqualTo("30714006");
    }

    @Test
    void shouldCreateObservationsForResistancesResistantWithVersionMap() {

      ResistanceDTO resistanceDTO = mock(ResistanceDTO.class);
      Patient patient = new Patient();
      patient.setId("Patient/123");
      Specimen specimen = new Specimen();
      specimen.setId("Specimen/456");

      CodeDisplay system = new CodeDisplay("R123");
      system.setDisplay("Resistance Display");
      when(resistanceDTO.getResistance()).thenReturn(system);
      when(resistanceDTO.getResistanceResult())
          .thenReturn(ResistanceDTO.ResistanceResultEnum.RESISTANT);

      var versionMap = new HashMap<String, String>();
      versionMap.put("http://snomed.info/sct", "snomedVersion");

      List<Observation> observations =
          ObservationCreator.createObservationsForResistances(
              List.of(resistanceDTO), patient, specimen, "PathogenCode", versionMap);

      assertThat(observations).hasSize(1);
      Observation observation = observations.getFirst();
      assertThat(observation.getCode().getCodingFirstRep().getCode()).isEqualTo("R123");
      assertThat(observation.getCode().getCodingFirstRep().getDisplay())
          .isEqualTo("Resistance Display");
      assertThat(observation.getInterpretationFirstRep().getCodingFirstRep().getCode())
          .isEqualTo("R");
      assertThat(observation.getValueCodeableConcept().getCodingFirstRep().getCode())
          .isEqualTo("30714006");

      CodeableConcept codeableConcept = (CodeableConcept) observation.getValue();
      assertThat(codeableConcept.getCodingFirstRep().getVersion()).isEqualTo("snomedVersion");
    }

    @Test
    void shouldCreateObservationsForResistancesIntermediate() {

      ResistanceDTO resistanceDTO = mock(ResistanceDTO.class);
      Patient patient = new Patient();
      patient.setId("Patient/123");
      Specimen specimen = new Specimen();
      specimen.setId("Specimen/456");

      CodeDisplay system = new CodeDisplay("R123");
      system.setDisplay("Resistance Display");
      when(resistanceDTO.getResistance()).thenReturn(system);
      when(resistanceDTO.getResistanceResult())
          .thenReturn(ResistanceDTO.ResistanceResultEnum.INTERMEDIATE);

      List<Observation> observations =
          ObservationCreator.createObservationsForResistances(
              List.of(resistanceDTO), patient, specimen, "PathogenCode", null);

      assertThat(observations).hasSize(1);
      Observation observation = observations.getFirst();
      assertThat(observation.getCode().getCodingFirstRep().getCode()).isEqualTo("R123");
      assertThat(observation.getCode().getCodingFirstRep().getDisplay())
          .isEqualTo("Resistance Display");
      assertThat(observation.getInterpretationFirstRep().getCodingFirstRep().getCode())
          .isEqualTo("I");
      assertThat(observation.getValueCodeableConcept().getCodingFirstRep().getCode())
          .isEqualTo("264841006");
    }

    @Test
    void shouldCreateObservationsForResistancesSusceptibleWithIncreaseExposure() {

      ResistanceDTO resistanceDTO = mock(ResistanceDTO.class);
      Patient patient = new Patient();
      patient.setId("Patient/123");
      Specimen specimen = new Specimen();
      specimen.setId("Specimen/456");

      CodeDisplay system = new CodeDisplay("R123");
      system.setDisplay("Resistance Display");
      when(resistanceDTO.getResistance()).thenReturn(system);
      when(resistanceDTO.getResistanceResult())
          .thenReturn(ResistanceDTO.ResistanceResultEnum.SUSCEPTIBLE_WITH_INCREASED_EXPOSURE);

      List<Observation> observations =
          ObservationCreator.createObservationsForResistances(
              List.of(resistanceDTO), patient, specimen, "PathogenCode", null);

      assertThat(observations).hasSize(1);
      Observation observation = observations.getFirst();
      assertThat(observation.getCode().getCodingFirstRep().getCode()).isEqualTo("R123");
      assertThat(observation.getCode().getCodingFirstRep().getDisplay())
          .isEqualTo("Resistance Display");
      assertThat(observation.getInterpretationFirstRep().getCodingFirstRep().getCode())
          .isEqualTo("I");
      assertThat(observation.getValueCodeableConcept().getCodingFirstRep().getCode())
          .isEqualTo("1255965005");
    }

    @Test
    void shouldCreateObservationsForResistancesSusceptible() {

      ResistanceDTO resistanceDTO = mock(ResistanceDTO.class);
      Patient patient = new Patient();
      patient.setId("Patient/123");
      Specimen specimen = new Specimen();
      specimen.setId("Specimen/456");

      CodeDisplay system = new CodeDisplay("R123");
      system.setDisplay("Resistance Display");
      when(resistanceDTO.getResistance()).thenReturn(system);
      when(resistanceDTO.getResistanceResult())
          .thenReturn(ResistanceDTO.ResistanceResultEnum.SUSCEPTIBLE);

      List<Observation> observations =
          ObservationCreator.createObservationsForResistances(
              List.of(resistanceDTO), patient, specimen, "PathogenCode", null);

      assertThat(observations).hasSize(1);
      Observation observation = observations.getFirst();
      assertThat(observation.getCode().getCodingFirstRep().getCode()).isEqualTo("R123");
      assertThat(observation.getCode().getCodingFirstRep().getDisplay())
          .isEqualTo("Resistance Display");
      assertThat(observation.getInterpretationFirstRep().getCodingFirstRep().getCode())
          .isEqualTo("S");
      assertThat(observation.getValueCodeableConcept().getCodingFirstRep().getCode())
          .isEqualTo("131196009");
    }

    @Test
    void shouldCreateObservationsForResistancesIndeterminate() {

      ResistanceDTO resistanceDTO = mock(ResistanceDTO.class);
      Patient patient = new Patient();
      patient.setId("Patient/123");
      Specimen specimen = new Specimen();
      specimen.setId("Specimen/456");

      CodeDisplay system = new CodeDisplay("R123");
      system.setDisplay("Resistance Display");
      when(resistanceDTO.getResistance()).thenReturn(system);
      when(resistanceDTO.getResistanceResult())
          .thenReturn(ResistanceDTO.ResistanceResultEnum.INDETERMINATE);

      List<Observation> observations =
          ObservationCreator.createObservationsForResistances(
              List.of(resistanceDTO), patient, specimen, "PathogenCode", null);

      assertThat(observations).hasSize(1);
      Observation observation = observations.getFirst();
      assertThat(observation.getCode().getCodingFirstRep().getCode()).isEqualTo("R123");
      assertThat(observation.getCode().getCodingFirstRep().getDisplay())
          .isEqualTo("Resistance Display");
      assertThat(observation.getInterpretationFirstRep().getCodingFirstRep().getCode())
          .isEqualTo("IND");
      assertThat(observation.getValueCodeableConcept().getCodingFirstRep().getCode())
          .isEqualTo("82334004");
    }
  }

  @Nested
  class ResistanceGeneTests {

    @Test
    void shouldReturnEmptyCollectionForResistanceNull() {

      assertThat(
              ObservationCreator.createObservationsForResistanceGenes(null, null, null, null, null))
          .isEmpty();
      assertThat(
              ObservationCreator.createObservationsForResistanceGenes(
                  List.of(), null, null, null, null))
          .isEmpty();
    }

    @Test
    void shouldCreateObservationsForResistanceGenesIntermediate() {

      ResistanceGeneDTO resistanceGeneDTO = mock(ResistanceGeneDTO.class);
      Patient patient = new Patient();
      patient.setId("Patient/123");
      Specimen specimen = new Specimen();
      specimen.setId("Specimen/456");

      CodeDisplay rg123 = new CodeDisplay("RG123");
      rg123.setDisplay("Resistance Gene Display");
      when(resistanceGeneDTO.getResistanceGene()).thenReturn(rg123);
      when(resistanceGeneDTO.getResistanceGeneResult())
          .thenReturn(ResistanceGeneDTO.ResistanceGeneResultEnum.INDETERMINATE);

      List<Observation> observations =
          ObservationCreator.createObservationsForResistanceGenes(
              List.of(resistanceGeneDTO), patient, specimen, "PathogenCode", null);

      assertThat(observations).hasSize(1);
      Observation observation = observations.getFirst();
      assertThat(observation.getCode().getCodingFirstRep().getCode()).isEqualTo("RG123");
      assertThat(observation.getCode().getCodingFirstRep().getDisplay())
          .isEqualTo("Resistance Gene Display");
      assertThat(observation.getInterpretationFirstRep().getCodingFirstRep().getCode())
          .isEqualTo("IND");
      assertThat(observation.getValueCodeableConcept().getCodingFirstRep().getCode())
          .isEqualTo("82334004");
      assertThat(observation.getMethod().getCodingFirstRep().getCode()).isEqualTo("708068002");
    }

    @Test
    void shouldCreateObservationsForResistanceGenesIntermediateWithVersionMap() {

      ResistanceGeneDTO resistanceGeneDTO = mock(ResistanceGeneDTO.class);
      Patient patient = new Patient();
      patient.setId("Patient/123");
      Specimen specimen = new Specimen();
      specimen.setId("Specimen/456");

      CodeDisplay rg123 = new CodeDisplay("RG123");
      rg123.setDisplay("Resistance Gene Display");
      when(resistanceGeneDTO.getResistanceGene()).thenReturn(rg123);
      when(resistanceGeneDTO.getResistanceGeneResult())
          .thenReturn(ResistanceGeneDTO.ResistanceGeneResultEnum.INDETERMINATE);

      var versionMap = new HashMap<String, String>();
      versionMap.put("http://snomed.info/sct", "snomedVersion");

      List<Observation> observations =
          ObservationCreator.createObservationsForResistanceGenes(
              List.of(resistanceGeneDTO), patient, specimen, "PathogenCode", versionMap);

      assertThat(observations).hasSize(1);
      Observation observation = observations.getFirst();
      assertThat(observation.getCode().getCodingFirstRep().getCode()).isEqualTo("RG123");
      assertThat(observation.getCode().getCodingFirstRep().getDisplay())
          .isEqualTo("Resistance Gene Display");
      assertThat(observation.getInterpretationFirstRep().getCodingFirstRep().getCode())
          .isEqualTo("IND");
      assertThat(observation.getValueCodeableConcept().getCodingFirstRep().getCode())
          .isEqualTo("82334004");
      assertThat(observation.getMethod().getCodingFirstRep().getCode()).isEqualTo("708068002");
    }

    @Test
    void shouldCreateObservationsForResistanceGenesDetected() {

      ResistanceGeneDTO resistanceGeneDTO = mock(ResistanceGeneDTO.class);
      Patient patient = new Patient();
      patient.setId("Patient/123");
      Specimen specimen = new Specimen();
      specimen.setId("Specimen/456");

      CodeDisplay rg123 = new CodeDisplay("RG123");
      rg123.setDisplay("Resistance Gene Display");
      when(resistanceGeneDTO.getResistanceGene()).thenReturn(rg123);
      when(resistanceGeneDTO.getResistanceGeneResult())
          .thenReturn(ResistanceGeneDTO.ResistanceGeneResultEnum.DETECTED);

      List<Observation> observations =
          ObservationCreator.createObservationsForResistanceGenes(
              List.of(resistanceGeneDTO), patient, specimen, "PathogenCode", null);

      assertThat(observations).hasSize(1);
      Observation observation = observations.getFirst();
      assertThat(observation.getCode().getCodingFirstRep().getCode()).isEqualTo("RG123");
      assertThat(observation.getCode().getCodingFirstRep().getDisplay())
          .isEqualTo("Resistance Gene Display");
      assertThat(observation.getInterpretationFirstRep().getCodingFirstRep().getCode())
          .isEqualTo("R");
      assertThat(observation.getValueCodeableConcept().getCodingFirstRep().getCode())
          .isEqualTo("260373001");
    }

    @Test
    void shouldCreateObservationsForResistanceGenesNotDetected() {

      ResistanceGeneDTO resistanceGeneDTO = mock(ResistanceGeneDTO.class);
      Patient patient = new Patient();
      patient.setId("Patient/123");
      Specimen specimen = new Specimen();
      specimen.setId("Specimen/456");

      CodeDisplay rg123 = new CodeDisplay("RG123");
      rg123.setDisplay("Resistance Gene Display");
      when(resistanceGeneDTO.getResistanceGene()).thenReturn(rg123);
      when(resistanceGeneDTO.getResistanceGeneResult())
          .thenReturn(ResistanceGeneDTO.ResistanceGeneResultEnum.NOT_DETECTED);

      List<Observation> observations =
          ObservationCreator.createObservationsForResistanceGenes(
              List.of(resistanceGeneDTO), patient, specimen, "PathogenCode", null);

      assertThat(observations).hasSize(1);
      Observation observation = observations.getFirst();
      assertThat(observation.getCode().getCodingFirstRep().getCode()).isEqualTo("RG123");
      assertThat(observation.getCode().getCodingFirstRep().getDisplay())
          .isEqualTo("Resistance Gene Display");
      assertThat(observation.getInterpretationFirstRep().getCodingFirstRep().getCode())
          .isEqualTo("S");
      assertThat(observation.getValueCodeableConcept().getCodingFirstRep().getCode())
          .isEqualTo("260415000");
    }
  }

  @Nested
  class VersionsThroughPortalTest {
    @Test
    void shouldCreateObservationsForPathogen() {

      PathogenDTO pathogenDTO = mock(PathogenDTO.class);
      MethodPathogenDTO methodPathogenDTO = mock(MethodPathogenDTO.class);
      Patient patient = new Patient();
      patient.setId("Patient/123");
      Specimen specimen = new Specimen();
      specimen.setId("Specimen/456");
      NotificationLaboratoryCategory notificationCategory =
          mock(NotificationLaboratoryCategory.class);

      when(notificationCategory.getPathogen()).thenReturn(new CodeDisplay("12345"));
      when(pathogenDTO.getCodeDisplay()).thenReturn(new CodeDisplay("67890"));
      when(methodPathogenDTO.getResult()).thenReturn(MethodPathogenDTO.ResultEnum.POS);
      when(methodPathogenDTO.getMethod())
          .thenReturn(new CodeDisplay("MethodCode").display("Method"));

      var versionMap = new HashMap<String, String>();
      versionMap.put("http://snomed.info/sct", "snomedVersion");

      List<Observation> observations =
          ObservationCreator.createObservation(
              pathogenDTO,
              List.of(methodPathogenDTO),
              patient,
              specimen,
              notificationCategory,
              versionMap);

      assertThat(observations).hasSize(1);
      Observation observation = observations.getFirst();
      assertThat(observation.getValue()).isInstanceOf(CodeableConcept.class);
      CodeableConcept codeableConcept = (CodeableConcept) observation.getValue();
      assertThat(codeableConcept.getCodingFirstRep().getVersion()).isEqualTo("snomedVersion");
    }
  }
}
