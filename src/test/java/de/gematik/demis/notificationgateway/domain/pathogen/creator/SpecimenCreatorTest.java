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

import static de.gematik.demis.notificationgateway.domain.pathogen.creator.SpecimenCreator.createSpecimen;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import de.gematik.demis.notificationgateway.common.dto.*;
import de.gematik.demis.notificationgateway.common.utils.DateUtils;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Specimen;
import org.junit.jupiter.api.Test;

class SpecimenCreatorTest {

  @Test
  void shouldCreateSpecimenAndObservations() {
    PathogenDTO pathogenDTO = new PathogenDTO();
    pathogenDTO.setCodeDisplay(new CodeDisplay("PathogenCode"));
    SpecimenDTO specimenDTO = new SpecimenDTO();
    specimenDTO.setMaterial(new CodeDisplay("MaterialCode").display("MaterialDisplay"));
    pathogenDTO.setSpecimenList(List.of(specimenDTO));
    Patient patient = new Patient();
    patient.setId("Patient/123");
    PractitionerRole submittingRole = new PractitionerRole();
    submittingRole.setId("PractitionerRole/123");
    NotificationLaboratoryCategory notificationCategory =
        mock(NotificationLaboratoryCategory.class);
    when(notificationCategory.getPathogen()).thenReturn(new CodeDisplay("PathogenCode"));

    List<Observation> observationList = new ArrayList<>();

    MethodPathogenDTO methodPathogenDTO = new MethodPathogenDTO();
    methodPathogenDTO.setResult(MethodPathogenDTO.ResultEnum.POS);
    methodPathogenDTO.setMethod(new CodeDisplay("MethodCode"));
    specimenDTO.setMethodPathogenList(List.of(methodPathogenDTO));
    ResistanceGeneDTO resistanceGeneDTO = new ResistanceGeneDTO();
    resistanceGeneDTO.setResistanceGene(new CodeDisplay("ResistanceGeneCode"));
    resistanceGeneDTO.setResistanceGeneResult(ResistanceGeneDTO.ResistanceGeneResultEnum.DETECTED);
    specimenDTO.setResistanceGeneList(List.of(resistanceGeneDTO));
    ResistanceDTO resistanceDTO = new ResistanceDTO();
    resistanceDTO.setResistance(new CodeDisplay("ResistanceCode"));
    resistanceDTO.setResistanceResult(ResistanceDTO.ResistanceResultEnum.RESISTANT);
    specimenDTO.setResistanceList(List.of(resistanceDTO));

    List<Specimen> specimen =
        createSpecimen(
            pathogenDTO, patient, submittingRole, observationList, notificationCategory, null);

    assertThat(specimen).hasSize(1);
    assertThat(specimen.get(0).getType().getCodingFirstRep().getCode()).isEqualTo("MaterialCode");
    assertThat(specimen.get(0).getType().getCodingFirstRep().getDisplay())
        .isEqualTo("MaterialDisplay");

    assertThat(observationList)
        .hasSize(3)
        .allSatisfy(
            observation -> {
              assertThat(observation.getSubject().getReference()).isEqualTo("Patient/123");
              assertThat(observation.getSpecimen().getResource().getIdElement())
                  .isEqualTo(specimen.get(0).getIdElement());
            });
  }

  @Test
  void shouldCreateSpecimenAndObservationsExtractionDateCreation() {
    PathogenDTO pathogenDTO = new PathogenDTO();
    pathogenDTO.setCodeDisplay(new CodeDisplay("PathogenCode"));
    SpecimenDTO specimenDTO = new SpecimenDTO();
    specimenDTO.setMaterial(new CodeDisplay("MaterialCode").display("MaterialDisplay"));
    pathogenDTO.setSpecimenList(List.of(specimenDTO));
    LocalDate date = LocalDate.now();
    specimenDTO.setExtractionDate(date);
    Patient patient = new Patient();
    patient.setId("Patient/123");
    PractitionerRole submittingRole = new PractitionerRole();
    submittingRole.setId("PractitionerRole/123");
    NotificationLaboratoryCategory notificationCategory = new NotificationLaboratoryCategory();
    notificationCategory.setPathogen(new CodeDisplay("PathogenCode"));

    List<Observation> observationList = new ArrayList<>();

    MethodPathogenDTO methodPathogenDTO = new MethodPathogenDTO();
    methodPathogenDTO.setResult(MethodPathogenDTO.ResultEnum.POS);
    methodPathogenDTO.setMethod(new CodeDisplay("MethodCode"));
    specimenDTO.setMethodPathogenList(List.of(methodPathogenDTO));
    ResistanceGeneDTO resistanceGeneDTO = new ResistanceGeneDTO();
    resistanceGeneDTO.setResistanceGene(new CodeDisplay("ResistanceGeneCode"));
    resistanceGeneDTO.setResistanceGeneResult(ResistanceGeneDTO.ResistanceGeneResultEnum.DETECTED);
    specimenDTO.setResistanceGeneList(List.of(resistanceGeneDTO));
    ResistanceDTO resistanceDTO = new ResistanceDTO();
    resistanceDTO.setResistance(new CodeDisplay("ResistanceCode"));
    resistanceDTO.setResistanceResult(ResistanceDTO.ResistanceResultEnum.RESISTANT);
    specimenDTO.setResistanceList(List.of(resistanceDTO));

    List<Specimen> specimen =
        createSpecimen(
            pathogenDTO, patient, submittingRole, observationList, notificationCategory, null);

    assertThat(specimen).hasSize(1);
    assertThat(specimen.get(0).getType().getCodingFirstRep().getCode()).isEqualTo("MaterialCode");
    assertThat(specimen.get(0).getType().getCodingFirstRep().getDisplay())
        .isEqualTo("MaterialDisplay");

    // Extract the Date from the DateTimeType and compare
    DateTimeType collectedDateTime = (DateTimeType) specimen.get(0).getCollection().getCollected();
    assertThat(collectedDateTime.getValue()).isEqualTo(DateUtils.createDate(date));

    assertThat(observationList)
        .hasSize(3)
        .allSatisfy(
            observation -> {
              assertThat(observation.getSubject().getReference()).isEqualTo("Patient/123");
              assertThat(observation.getSpecimen().getResource().getIdElement())
                  .isEqualTo(specimen.get(0).getIdElement());
            });
  }

  @Test
  void shouldHandleEmptySpecimenList() {
    PathogenDTO pathogenDTO = mock(PathogenDTO.class);
    Patient patient = new Patient();
    PractitionerRole submittingRole = new PractitionerRole();
    NotificationLaboratoryCategory notificationCategory =
        mock(NotificationLaboratoryCategory.class);

    List<Observation> observationList = new ArrayList<>();

    when(pathogenDTO.getSpecimenList()).thenReturn(List.of());
    when(pathogenDTO.getCodeDisplay()).thenReturn(new CodeDisplay("PathogenCode"));

    List<Specimen> specimen =
        createSpecimen(
            pathogenDTO, patient, submittingRole, observationList, notificationCategory, null);

    assertThat(specimen).isEmpty();
    assertThat(observationList).isEmpty();
  }

  @Test
  void shouldHandleNullSpecimenList() {
    PathogenDTO pathogenDTO = mock(PathogenDTO.class);
    Patient patient = new Patient();
    PractitionerRole submittingRole = new PractitionerRole();
    NotificationLaboratoryCategory notificationCategory =
        mock(NotificationLaboratoryCategory.class);

    List<Observation> observationList = new ArrayList<>();

    when(pathogenDTO.getSpecimenList()).thenReturn(null);
    when(pathogenDTO.getCodeDisplay()).thenReturn(new CodeDisplay("PathogenCode"));

    List<Specimen> specimen =
        createSpecimen(
            pathogenDTO, patient, submittingRole, observationList, notificationCategory, null);

    assertThat(specimen).isEmpty();
    assertThat(observationList).isEmpty();
  }

  @Test
  void shouldReturnEmptyListWhenSpecimenListIsEmpty() {
    PathogenDTO pathogenDTO = mock(PathogenDTO.class);
    Patient patient = new Patient();
    PractitionerRole submittingRole = new PractitionerRole();
    NotificationLaboratoryCategory notificationCategory =
        mock(NotificationLaboratoryCategory.class);

    List<Observation> observationList = new ArrayList<>();

    when(pathogenDTO.getSpecimenList()).thenReturn(List.of());
    when(pathogenDTO.getCodeDisplay()).thenReturn(new CodeDisplay("PathogenCode"));

    List<Specimen> specimens =
        SpecimenCreator.createSpecimen(
            pathogenDTO, patient, submittingRole, observationList, notificationCategory, null);

    assertThat(specimens).isEmpty();
    assertThat(observationList).isEmpty();
  }

  @Test
  void shouldReturnEmptyListWhenSpecimenListIsNull() {
    PathogenDTO pathogenDTO = mock(PathogenDTO.class);
    Patient patient = new Patient();
    PractitionerRole submittingRole = new PractitionerRole();
    NotificationLaboratoryCategory notificationCategory =
        mock(NotificationLaboratoryCategory.class);

    List<Observation> observationList = new ArrayList<>();

    when(pathogenDTO.getSpecimenList()).thenReturn(null);
    when(pathogenDTO.getCodeDisplay()).thenReturn(new CodeDisplay("PathogenCode"));

    List<Specimen> specimens =
        SpecimenCreator.createSpecimen(
            pathogenDTO, patient, submittingRole, observationList, notificationCategory, null);

    assertThat(specimens).isEmpty();
    assertThat(observationList).isEmpty();
  }

  @Test
  void shouldCreateMultipleSpecimensForMultipleSpecimenDTOs() {
    PathogenDTO pathogenDTO = new PathogenDTO();
    pathogenDTO.setCodeDisplay(new CodeDisplay("PathogenCode"));

    SpecimenDTO specimenDTO1 = new SpecimenDTO();
    specimenDTO1.setMaterial(new CodeDisplay("MaterialCode1").display("MaterialDisplay1"));

    SpecimenDTO specimenDTO2 = new SpecimenDTO();
    specimenDTO2.setMaterial(new CodeDisplay("MaterialCode2").display("MaterialDisplay2"));

    pathogenDTO.setSpecimenList(List.of(specimenDTO1, specimenDTO2));

    Patient patient = new Patient();
    patient.setId("Patient/123");
    PractitionerRole submittingRole = new PractitionerRole();
    submittingRole.setId("PractitionerRole/123");
    NotificationLaboratoryCategory notificationCategory = new NotificationLaboratoryCategory();
    notificationCategory.setPathogen(new CodeDisplay("PathogenCode"));

    MethodPathogenDTO methodPathogenDTO = new MethodPathogenDTO();
    methodPathogenDTO.setResult(MethodPathogenDTO.ResultEnum.POS);
    methodPathogenDTO.setMethod(new CodeDisplay("MethodCode"));
    specimenDTO1.setMethodPathogenList(List.of(methodPathogenDTO));
    ResistanceGeneDTO resistanceGeneDTO = new ResistanceGeneDTO();
    resistanceGeneDTO.setResistanceGene(new CodeDisplay("ResistanceGeneCode"));
    resistanceGeneDTO.setResistanceGeneResult(ResistanceGeneDTO.ResistanceGeneResultEnum.DETECTED);
    specimenDTO1.setResistanceGeneList(List.of(resistanceGeneDTO));
    ResistanceDTO resistanceDTO = new ResistanceDTO();
    resistanceDTO.setResistance(new CodeDisplay("ResistanceCode"));
    resistanceDTO.setResistanceResult(ResistanceDTO.ResistanceResultEnum.RESISTANT);
    specimenDTO1.setResistanceList(List.of(resistanceDTO));

    MethodPathogenDTO methodPathogenDTO2 = new MethodPathogenDTO();
    methodPathogenDTO2.setResult(MethodPathogenDTO.ResultEnum.POS);
    methodPathogenDTO2.setMethod(new CodeDisplay("MethodCode"));
    specimenDTO2.setMethodPathogenList(List.of(methodPathogenDTO2));
    ResistanceGeneDTO resistanceGeneDTO2 = new ResistanceGeneDTO();
    resistanceGeneDTO2.setResistanceGene(new CodeDisplay("ResistanceGeneCode"));
    resistanceGeneDTO2.setResistanceGeneResult(ResistanceGeneDTO.ResistanceGeneResultEnum.DETECTED);
    specimenDTO1.setResistanceGeneList(List.of(resistanceGeneDTO2));
    ResistanceDTO resistanceDTO2 = new ResistanceDTO();
    resistanceDTO2.setResistance(new CodeDisplay("ResistanceCode"));
    resistanceDTO2.setResistanceResult(ResistanceDTO.ResistanceResultEnum.RESISTANT);
    specimenDTO2.setResistanceList(List.of(resistanceDTO2));

    List<Observation> observationList = new ArrayList<>();

    List<Specimen> specimens =
        SpecimenCreator.createSpecimen(
            pathogenDTO, patient, submittingRole, observationList, notificationCategory, null);

    assertThat(specimens).hasSize(2);
    assertThat(specimens.get(0).getType().getCodingFirstRep().getCode()).isEqualTo("MaterialCode1");
    assertThat(specimens.get(0).getType().getCodingFirstRep().getDisplay())
        .isEqualTo("MaterialDisplay1");
    assertThat(specimens.get(1).getType().getCodingFirstRep().getCode()).isEqualTo("MaterialCode2");
    assertThat(specimens.get(1).getType().getCodingFirstRep().getDisplay())
        .isEqualTo("MaterialDisplay2");
  }
}
