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

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.CODE_SYSTEM_OBSERVATION_INTERPRETATION;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.PROFILE_PATHOGEN_DETECTION_CVDP;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.SYSTEM_LOINC;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.SYSTEM_SNOMED;

import de.gematik.demis.notificationgateway.common.dto.Diagnosis;
import de.gematik.demis.notificationgateway.common.dto.Diagnosis.TestTypeEnum;
import de.gematik.demis.notificationgateway.common.dto.Diagnosis.VocEnum;
import de.gematik.demis.notificationgateway.common.utils.ConfiguredCodeSystems;
import de.gematik.demis.notificationgateway.common.utils.ReferenceUtils;
import java.util.UUID;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Specimen;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.codesystems.V3ObservationInterpretation;
import org.springframework.stereotype.Service;

@Service
public class PathogenDetectionCVDPCreationService {
  public Observation createPathogenDetectionCVDP(
      Diagnosis diagnosticContent, Patient notifiedPerson, Specimen specimen) {
    Observation observation = new Observation();
    observation.setId(UUID.randomUUID().toString());
    observation.setMeta(new Meta().addProfile(PROFILE_PATHOGEN_DETECTION_CVDP));

    observation.setStatus(ObservationStatus.FINAL);
    addCategory(observation);
    addCode(observation, diagnosticContent);
    observation.setSubject(ReferenceUtils.createReference(notifiedPerson));
    addValue(observation, diagnosticContent);
    addInterpretation(observation);
    addMethod(observation, diagnosticContent);
    addNote(observation, diagnosticContent);
    observation.setSpecimen(ReferenceUtils.createReference(specimen));

    return observation;
  }

  private void addValue(Observation observation, Diagnosis diagnosticContent) {
    final TestTypeEnum testType = diagnosticContent.getTestType();
    switch (testType) {
      case ANTIGEN_RAPID_TEST:
      case NUCLEIC_ACID_CERTIFICATE:
        observation.setValue(new StringType("Positiv"));
        break;
      case PCR_RAPID_TEST:
        observation.setValue(new StringType("PCR-Schnelltest positiv"));
        break;
      case SEQUENCING:
      case VARIANT_SPECIFIC_PCR:
        setVocValue(observation, diagnosticContent);
        break;
      default:
        throw new IllegalStateException("unkown test type: " + testType);
    }
  }

  private void setVocValue(Observation observation, Diagnosis diagnosticContent) {
    final VocEnum voc = diagnosticContent.getVoc();
    if (voc != null) {
      switch (voc) {
        case ALPHA:
          observation.setValue(new StringType("#B.1.1.7"));
          break;
        case BETA:
          observation.setValue(new StringType("#B.1.351"));
          break;
        case DELTA:
          observation.setValue(new StringType("#B.1.617.2"));
          break;
        case GAMMA:
          observation.setValue(new StringType("#B.1.1.28.1"));
          break;
        case OMIKRON:
          observation.setValue(new StringType("#B.1.1.529"));
          break;
        default:
          throw new IllegalStateException("unkown voc: " + voc);
      }
    } else {
      observation.setValue(new StringType("Variante nicht bestimmbar"));
    }
  }

  private void addInterpretation(Observation observation) {
    final Coding code =
        new Coding()
            .setSystem(CODE_SYSTEM_OBSERVATION_INTERPRETATION)
            .setCode(V3ObservationInterpretation.POS.toCode());
    observation.addInterpretation(new CodeableConcept(code));
  }

  private void addCategory(Observation observation) {
    observation
        .addCategory()
        .addCoding(ConfiguredCodeSystems.getInstance().getObservationCategoryCoding("laboratory"));
  }

  private void addCode(Observation observation, Diagnosis diagnosticContent) {
    final Coding coding = new Coding();
    coding.setSystem(SYSTEM_LOINC);

    final TestTypeEnum testType = diagnosticContent.getTestType();
    switch (testType) {
      case ANTIGEN_RAPID_TEST:
        coding
            .setCode("94558-4")
            .setDisplay(
                "SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay");
        break;
      case NUCLEIC_ACID_CERTIFICATE:
        coding
            .setCode("95406-5")
            .setDisplay("SARS-CoV-2 (COVID-19) RNA [Presence] in Nose by NAA with probe detection");
        break;
      case PCR_RAPID_TEST:
        coding
            .setCode("94746-5")
            .setDisplay(
                "SARS-CoV-2 (COVID-19) RNA [Cycle Threshold #] in Specimen by NAA with probe detection");
        break;
      case SEQUENCING:
        coding
            .setCode("96741-4")
            .setDisplay("SARS-CoV-2 (COVID-19) variant [Type] in Specimen by Sequencing");
        break;
      case VARIANT_SPECIFIC_PCR:
        coding
            .setCode("96751-3")
            .setDisplay(
                "SARS-CoV-2 (COVID-19) S gene mutation detected [Identifier] in Specimen by Molecular genetics method");
        break;
      default:
        throw new IllegalStateException("unknown test type: " + testType);
    }

    final CodeableConcept code = new CodeableConcept(coding);
    observation.setCode(code);
  }

  private void addMethod(Observation observation, Diagnosis diagnosticContent) {
    final Coding coding = new Coding();
    coding.setSystem(SYSTEM_SNOMED);

    final TestTypeEnum testType = diagnosticContent.getTestType();
    switch (testType) {
      case ANTIGEN_RAPID_TEST:
        coding.setCode("121276004").setDisplay("Antigen assay (procedure)");
        break;
      case NUCLEIC_ACID_CERTIFICATE:
        coding.setCode("398545005").setDisplay("Nucleic acid assay (procedure)");
        break;
      case PCR_RAPID_TEST:
        // corresponding snomed code not yet included in value set
        return;
      case SEQUENCING:
        coding.setCode("117040002").setDisplay("Nucleic acid sequencing (procedure)");
        break;
      case VARIANT_SPECIFIC_PCR:
        coding.setCode("69363007").setDisplay("Nucleic acid amplification (procedure)");
        break;
      default:
        throw new IllegalStateException("unknown test type: " + testType);
    }

    observation.setMethod(new CodeableConcept().addCoding(coding));
  }

  private void addNote(Observation observation, Diagnosis diagnosticContent) {
    final String comment = diagnosticContent.getComment();
    if (comment != null) {
      observation.addNote().setText(comment);
    }
  }
}
