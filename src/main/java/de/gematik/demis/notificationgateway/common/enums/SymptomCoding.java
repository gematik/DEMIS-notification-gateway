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

package de.gematik.demis.notificationgateway.common.enums;

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.SYSTEM_SNOMED;
import static de.gematik.demis.notificationgateway.common.dto.Symptom.ACUTE_RESPIRATORY_DISTRESS_SYNDROME;
import static de.gematik.demis.notificationgateway.common.dto.Symptom.CHILL;
import static de.gematik.demis.notificationgateway.common.dto.Symptom.COUGH;
import static de.gematik.demis.notificationgateway.common.dto.Symptom.DIARRHEA;
import static de.gematik.demis.notificationgateway.common.dto.Symptom.DYSPNEA;
import static de.gematik.demis.notificationgateway.common.dto.Symptom.FEVER;
import static de.gematik.demis.notificationgateway.common.dto.Symptom.GENERALLY_UNWELL;
import static de.gematik.demis.notificationgateway.common.dto.Symptom.HEADACHE;
import static de.gematik.demis.notificationgateway.common.dto.Symptom.LOSS_OF_SENSE_OF_SMELL;
import static de.gematik.demis.notificationgateway.common.dto.Symptom.LOSS_OF_TASTE;
import static de.gematik.demis.notificationgateway.common.dto.Symptom.MUSCLE_PAIN;
import static de.gematik.demis.notificationgateway.common.dto.Symptom.OTHER_COVID_19_SYMPTOMS;
import static de.gematik.demis.notificationgateway.common.dto.Symptom.PATIENT_VENTILATED;
import static de.gematik.demis.notificationgateway.common.dto.Symptom.PNEUMONIA;
import static de.gematik.demis.notificationgateway.common.dto.Symptom.SNIFFLES;
import static de.gematik.demis.notificationgateway.common.dto.Symptom.SORE_THROAT_SYMPTOM;
import static de.gematik.demis.notificationgateway.common.dto.Symptom.TACHYCARDIA;
import static de.gematik.demis.notificationgateway.common.dto.Symptom.TACHYPNEA;

import de.gematik.demis.notificationgateway.common.dto.Symptom;
import de.gematik.demis.notificationgateway.common.exceptions.BadRequestException;
import java.util.Arrays;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;

@Getter
public enum SymptomCoding {
  ACUTE_RESPIRATORY_DISTRESS_SYNDROME_CODING(
      ACUTE_RESPIRATORY_DISTRESS_SYNDROME,
      new Coding()
          .setSystem(SYSTEM_SNOMED)
          .setCode("67782005")
          .setDisplay("Acute respiratory distress syndrome (disorder)")),
  CHILL_CODING(
      CHILL,
      new Coding().setSystem(SYSTEM_SNOMED).setCode("43724002").setDisplay("Chill (finding)")),
  COUGH_CODING(
      COUGH,
      new Coding().setSystem(SYSTEM_SNOMED).setCode("49727002").setDisplay("Cough (finding)")),
  DIARRHEA_CODING(
      DIARRHEA,
      new Coding().setSystem(SYSTEM_SNOMED).setCode("62315008").setDisplay("Diarrhea (finding)")),
  DYSPNEA_CODING(
      DYSPNEA,
      new Coding().setSystem(SYSTEM_SNOMED).setCode("267036007").setDisplay("Dyspnea (finding)")),
  FEVER_CODING(
      FEVER,
      new Coding().setSystem(SYSTEM_SNOMED).setCode("386661006").setDisplay("Fever (finding)")),
  GENERALLY_UNWELL_CODING(
      GENERALLY_UNWELL,
      new Coding()
          .setSystem(SYSTEM_SNOMED)
          .setCode("213257006")
          .setDisplay("Generally unwell (finding)")),
  HEADACHE_CODING(
      HEADACHE,
      new Coding().setSystem(SYSTEM_SNOMED).setCode("25064002").setDisplay("Headache (finding)")),
  LOSS_OF_SENSE_OF_SMELL_CODING(
      LOSS_OF_SENSE_OF_SMELL,
      new Coding()
          .setSystem(SYSTEM_SNOMED)
          .setCode("44169009")
          .setDisplay("Loss of sense of smell (finding)")),
  LOSS_OF_TASTE_CODING(
      LOSS_OF_TASTE,
      new Coding()
          .setSystem(SYSTEM_SNOMED)
          .setCode("36955009")
          .setDisplay("Loss of taste (finding)")),
  MUSCLE_PAIN_CODING(
      MUSCLE_PAIN,
      new Coding()
          .setSystem(SYSTEM_SNOMED)
          .setCode("68962001")
          .setDisplay("Muscle pain (finding)")),
  PATIENT_VENTILATED_CODING(
      PATIENT_VENTILATED,
      new Coding()
          .setSystem(SYSTEM_SNOMED)
          .setCode("371820004")
          .setDisplay("Patient ventilated (finding)")),
  PNEUMONIA_CODING(
      PNEUMONIA,
      new Coding()
          .setSystem(SYSTEM_SNOMED)
          .setCode("233604007")
          .setDisplay("Pneumonia (disorder)")),
  SNIFFLES_CODING(
      SNIFFLES,
      new Coding().setSystem(SYSTEM_SNOMED).setCode("275280004").setDisplay("Sniffles (finding)")),
  SORE_THROAT_SYMPTOM_CODING(
      SORE_THROAT_SYMPTOM,
      new Coding()
          .setSystem(SYSTEM_SNOMED)
          .setCode("267102003")
          .setDisplay("Sore throat symptom (finding)")),
  TACHYPNEA_CODING(
      TACHYPNEA,
      new Coding().setSystem(SYSTEM_SNOMED).setCode("271823003").setDisplay("Tachypnea (finding)")),
  TACHYCARDIA_CODING(
      TACHYCARDIA,
      new Coding().setSystem(SYSTEM_SNOMED).setCode("3424008").setDisplay("Tachycardia (finding)")),
  OTHER_COVID_19_SYMPTOMS_CODING(
      OTHER_COVID_19_SYMPTOMS,
      new Coding()
          .setSystem(SYSTEM_SNOMED)
          .setCode("840539006:47429007=404684003")
          .setDisplay("andere COVID-19-Symptome"));

  private final Symptom symptom;

  private final Coding coding;

  SymptomCoding(Symptom symptom, Coding coding) {
    this.symptom = symptom;
    this.coding = coding;
  }

  public static SymptomCoding bySymptom(Symptom symptom) throws BadRequestException {
    return Arrays.stream(values())
        .filter(symptomCoding -> symptomCoding.symptom == symptom)
        .findFirst()
        .orElseThrow(() -> new BadRequestException("unknown symptom enum: " + symptom));
  }
}
