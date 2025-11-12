package de.gematik.demis.notificationgateway.domain.pathogen.creator;

/*-
 * #%L
 * DEMIS Notification-Gateway
 * %%
 * Copyright (C) 2025 gematik GmbH
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
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */

import static de.gematik.demis.notificationgateway.domain.pathogen.mapper.RkiCodeUtil.getInterpretationValueCodeForResistance;
import static de.gematik.demis.notificationgateway.domain.pathogen.mapper.RkiCodeUtil.getInterpretationValueCodeForResistanceGene;

import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.laboratory.PathogenDetectionDataBuilder;
import de.gematik.demis.notificationgateway.common.dto.CodeDisplay;
import de.gematik.demis.notificationgateway.common.dto.MethodPathogenDTO;
import de.gematik.demis.notificationgateway.common.dto.NotificationLaboratoryCategory;
import de.gematik.demis.notificationgateway.common.dto.PathogenDTO;
import de.gematik.demis.notificationgateway.common.dto.ResistanceDTO;
import de.gematik.demis.notificationgateway.common.dto.ResistanceGeneDTO;
import de.gematik.demis.notificationgateway.domain.pathogen.mapper.RkiCodeUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Specimen;

/**
 * Utility class for creating FHIR {@link DiagnosticReport} objects.
 *
 * <p>This class provides methods to build a {@link DiagnosticReport} instance based on the provided
 * {@link PathogenDTO}, {@link Patient}, and other related data.
 */
public class ObservationCreator {

  /** SNOMED CT URL used for coding in FHIR {@link Observation} objects. */
  public static final String SNOMED_CT_URL = "http://snomed.info/sct";

  public static final String SNOMED_CT_VERSION =
      "http://snomed.info/sct/11000274103/version/20241115";

  /**
   * Private constructor to prevent instantiation of this utility class.
   *
   * <p>This class is designed as a utility class and should not be instantiated.
   */
  private ObservationCreator() {}

  /**
   * Creates a list of FHIR {@link Observation} objects for pathogen detection.
   *
   * @param pathogenDTO The {@link PathogenDTO} object containing pathogen details.
   * @param methodPathogenDTOList A list of {@link MethodPathogenDTO} objects representing methods
   *     used for detection.
   * @param patient The {@link Patient} object representing the notified person.
   * @param specimen The {@link Specimen} object representing the specimen used for testing.
   * @param notificationCategory The {@link NotificationLaboratoryCategory} object containing
   *     notification details.
   * @return A list of {@link Observation} objects populated with the provided data.
   */
  public static List<Observation> createObservation(
      PathogenDTO pathogenDTO,
      List<MethodPathogenDTO> methodPathogenDTOList,
      Patient patient,
      Specimen specimen,
      NotificationLaboratoryCategory notificationCategory) {
    final List<Observation> collect = new ArrayList<>();
    final String pathogenCode = notificationCategory.getPathogen().getCode();
    final String pathogenDisplay = notificationCategory.getPathogen().getDisplay();

    // Observation 1
    final String pathogenShortCode = pathogenDTO.getCodeDisplay().getCode();
    final MethodPathogenDTO firstMethodPathogenDTO = methodPathogenDTOList.getFirst();
    collect.add(
        createSingleObservation(
            firstMethodPathogenDTO,
            pathogenCode,
            pathogenDisplay,
            patient,
            specimen,
            pathogenShortCode));

    // Observation 2 - only if analyt is not null
    if (firstMethodPathogenDTO.getAnalyt() != null) {
      collect.add(
          createSingleObservation(
              firstMethodPathogenDTO,
              firstMethodPathogenDTO.getAnalyt().getCode(),
              firstMethodPathogenDTO.getAnalyt().getDisplay(),
              patient,
              specimen,
              pathogenShortCode));
    }

    // Observations for all other diagnostik input
    for (int i = 1; i < methodPathogenDTOList.size(); i++) {
      final MethodPathogenDTO pdto = methodPathogenDTOList.get(i);
      String code;
      String display;
      if (pdto.getAnalyt() != null) {
        code = pdto.getAnalyt().getCode();
        display = pdto.getAnalyt().getDisplay();
      } else {
        code = pathogenCode;
        display = pathogenDisplay;
      }
      collect.add(
          createSingleObservation(pdto, code, display, patient, specimen, pathogenShortCode));
    }

    return collect;
  }

  /**
   * Creates FHIR {@link Observation} objects for resistance genes.
   *
   * @param resistanceGenes A list of {@link ResistanceGeneDTO} objects representing resistance
   *     genes.
   * @param patient The {@link Patient} object representing the notified person.
   * @param specimen The {@link Specimen} object representing the specimen used for testing.
   * @param pathogenCode The code of the pathogen being tested.
   * @return A list of {@link Observation} objects populated with resistance gene data.
   */
  public static List<Observation> createObservationsForResistanceGenes(
      List<ResistanceGeneDTO> resistanceGenes,
      Patient patient,
      Specimen specimen,
      String pathogenCode) {
    if (resistanceGenes == null || resistanceGenes.isEmpty()) {
      return Collections.emptyList();
    }
    List<Observation> observations = new ArrayList<>();
    for (ResistanceGeneDTO resistanceGene : resistanceGenes) {
      CodeDisplay gene = resistanceGene.getResistanceGene();
      final String code = gene != null ? gene.getCode() : null;
      final String display = gene != null ? gene.getDisplay() : null;
      String obsercationSystem = null;
      String observationVersion = null;
      // TODO remove when with feature_flag_notifications_7_3 as it should be given every time
      if (gene != null) {
        String systemWithVersion = gene.getSystem();
        if (systemWithVersion != null) {
          String[] splitSystem = systemWithVersion.split("\\|");
          obsercationSystem = splitSystem[0];
          if (splitSystem.length > 1) {
            observationVersion = splitSystem[1];
          }
        }
      }

      RkiCodeUtil.InterpretationValueCode interpretationValueCodeForResistanceGene =
          getInterpretationValueCodeForResistanceGene(resistanceGene.getResistanceGeneResult());
      final String interpretation = interpretationValueCodeForResistanceGene.interpretation();
      final String valueCode = interpretationValueCodeForResistanceGene.valueCode();
      final String valueDisplay = "";

      String methodCode = "708068002";
      String methodDisplay = "Molecular genetics technique (qualifier value)";
      Observation observation =
          new PathogenDetectionDataBuilder()
              .setDefaultData()
              .setMethodCode(methodCode)
              .setMethodDisplay(methodDisplay)
              .setMethodSystem(SNOMED_CT_URL)
              .setMethodCodingVersion(SNOMED_CT_VERSION)
              .setInterpretationCode(interpretation)
              .setValue(
                  new CodeableConcept(
                      new Coding(SNOMED_CT_URL, valueCode, valueDisplay)
                          .setVersion(SNOMED_CT_VERSION)))
              .setNotifiedPerson(patient)
              .setSpecimen(specimen)
              .setProfileUrlHelper(pathogenCode)
              .setObservationCodeCode(code)
              .setObservationCodeDisplay(display)
              .setObservationCodeSystem(obsercationSystem)
              .setObservationCodeVersion(observationVersion)
              .setStatus(Observation.ObservationStatus.FINAL)
              .build();
      observations.add(observation);
    }
    return observations;
  }

  /**
   * Creates FHIR {@link Observation} objects for resistances.
   *
   * @param resistances A list of {@link ResistanceDTO} objects representing resistances.
   * @param patient The {@link Patient} object representing the notified person.
   * @param specimen The {@link Specimen} object representing the specimen used for testing.
   * @param pathogenCode The code of the pathogen being tested.
   * @return A list of {@link Observation} objects populated with resistance data.
   */
  public static List<Observation> createObservationsForResistances(
      List<ResistanceDTO> resistances, Patient patient, Specimen specimen, String pathogenCode) {
    if (resistances == null || resistances.isEmpty()) {
      return Collections.emptyList();
    }
    List<Observation> observations = new ArrayList<>();
    for (ResistanceDTO resistance : resistances) {
      CodeDisplay resistanceCodeDisplay = resistance.getResistance();
      final String code = resistanceCodeDisplay != null ? resistanceCodeDisplay.getCode() : null;
      final String display =
          resistanceCodeDisplay != null ? resistanceCodeDisplay.getDisplay() : null;

      RkiCodeUtil.InterpretationValueCode interpretationValueCodeForResistance =
          getInterpretationValueCodeForResistance(resistance.getResistanceResult());
      final String interpretation = interpretationValueCodeForResistance.interpretation();
      final String valueCode = interpretationValueCodeForResistance.valueCode();
      final String valueDisplay = "";

      String obsercationSystem = null;
      String observationVersion = null;
      // TODO remove when with feature_flag_notifications_7_3 as it should be given every time
      if (resistanceCodeDisplay != null) {
        String systemWithVersion = resistanceCodeDisplay.getSystem();
        if (systemWithVersion != null) {
          String[] splitSystem = systemWithVersion.split("\\|");
          obsercationSystem = splitSystem[0];
          if (splitSystem.length > 1) {
            observationVersion = splitSystem[1];
          }
        }
      }

      Observation observation =
          new PathogenDetectionDataBuilder()
              .setDefaultData()
              .setMethodCode("14788002")
              .setMethodDisplay("Antimicrobial susceptibility test (procedure)")
              .setMethodSystem(SNOMED_CT_URL)
              .setMethodCodingVersion(SNOMED_CT_VERSION)
              .setInterpretationCode(interpretation)
              .setValue(
                  new CodeableConcept(
                      new Coding(SNOMED_CT_URL, valueCode, valueDisplay)
                          .setVersion(SNOMED_CT_VERSION)))
              .setNotifiedPerson(patient)
              .setSpecimen(specimen)
              .setProfileUrlHelper(pathogenCode)
              .setObservationCodeCode(code)
              .setObservationCodeDisplay(display)
              .setObservationCodeSystem(obsercationSystem)
              .setObservationCodeVersion(observationVersion)
              .setStatus(Observation.ObservationStatus.FINAL)
              .build();
      observations.add(observation);
    }
    return observations;
  }

  /**
   * Creates a single FHIR {@link Observation} object with fixed values for observation code and
   * display. These observations are not intended for resistance or resistance gene data.
   *
   * @param methodPathogenDTO The {@link MethodPathogenDTO} object containing method details.
   * @param valueCode The code representing the observation value.
   * @param valueDisplay The display text for the observation value.
   * @param patient The {@link Patient} object representing the notified person.
   * @param specimen The {@link Specimen} object representing the specimen used for testing.
   * @param pathogenCode The code of the pathogen being tested.
   * @return A {@link Observation} object populated with the provided data.
   */
  private static Observation createSingleObservation(
      MethodPathogenDTO methodPathogenDTO,
      String valueCode,
      String valueDisplay,
      Patient patient,
      Specimen specimen,
      String pathogenCode) {

    String methodSystem = null;
    String methodVersion = null;
    CodeDisplay method = methodPathogenDTO.getMethod();
    // TODO remove when with feature_flag_notifications_7_3 as it should be given every time
    if (method.getSystem() != null) {
      String[] splitSystem = method.getSystem().split("\\|");
      methodSystem = splitSystem[0];
      if (splitSystem.length > 1) {
        methodVersion = splitSystem[1];
      }
    }

    return new PathogenDetectionDataBuilder()
        .setDefaultData()
        .setInterpretationCode(methodPathogenDTO.getResult().getValue())
        .setMethodCode(methodPathogenDTO.getMethod().getCode())
        .setMethodDisplay(methodPathogenDTO.getMethod().getDisplay())
        .setMethodCodingVersion(methodVersion)
        .setMethodSystem(methodSystem)
        .setValue(
            new CodeableConcept(
                new Coding(SNOMED_CT_URL, valueCode, valueDisplay).setVersion(SNOMED_CT_VERSION)))
        .setObservationCodeCode("41852-5")
        .setObservationCodeDisplay("Microorganism or agent identified in Specimen")
        .setObservationCodeSystem("http://loinc.org")
        .setObservationCodeVersion("2.79")
        .setStatus(Observation.ObservationStatus.FINAL)
        .setNotifiedPerson(patient)
        .setSpecimen(specimen)
        .setProfileUrlHelper(pathogenCode)
        .build();
  }
}
