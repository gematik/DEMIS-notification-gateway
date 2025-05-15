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

import static de.gematik.demis.notificationgateway.common.utils.DateUtils.createDate;
import static de.gematik.demis.notificationgateway.domain.pathogen.creator.ObservationCreator.createObservation;
import static de.gematik.demis.notificationgateway.domain.pathogen.creator.ObservationCreator.createObservationsForResistanceGenes;
import static de.gematik.demis.notificationgateway.domain.pathogen.creator.ObservationCreator.createObservationsForResistances;

import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.laboratory.SpecimenDataBuilder;
import de.gematik.demis.notificationgateway.common.dto.NotificationLaboratoryCategory;
import de.gematik.demis.notificationgateway.common.dto.PathogenDTO;
import de.gematik.demis.notificationgateway.common.dto.SpecimenDTO;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Specimen;
import org.springframework.stereotype.Component;

/**
 * Component class for creating FHIR {@link Specimen} objects.
 *
 * <p>This class provides methods to build a {@link Specimen} instance based on the provided {@link
 * PathogenDTO}, {@link Patient}, and other related data.
 */
@Component
public class SpecimenCreator {

  /** Private constructor to prevent instantiation of this utility class. */
  private SpecimenCreator() {}

  /**
   * Creates a FHIR {@link Specimen} object using the provided parameters.
   *
   * <p>This method also generates related {@link Observation} objects for pathogen detection,
   * resistance genes, and resistances, and adds them to the provided observation list.
   *
   * @param pathogenDTO The {@link PathogenDTO} object containing information about the pathogen.
   * @param patient The {@link Patient} object representing the notified person.
   * @param submittingRole The {@link PractitionerRole} object representing the submitting facility.
   * @param observation A list of {@link Observation} objects to which new observations will be
   *     added.
   * @param notificationLaboratoryCategory The {@link NotificationLaboratoryCategory} object
   *     containing details about the notification category.
   * @param featureFlagSnapshot5_3_0Active
   * @return A {@link Specimen} object populated with the provided data, or null if no specimen data
   *     is available.
   */
  public static List<Specimen> createSpecimen(
      PathogenDTO pathogenDTO,
      Patient patient,
      PractitionerRole submittingRole,
      List<Observation> observation,
      NotificationLaboratoryCategory notificationLaboratoryCategory,
      boolean featureFlagSnapshot5_3_0Active) {
    List<Specimen> returnList = new ArrayList<>();

    // Retrieve the pathogen short code and the list of specimen data transfer objects (DTOs).
    final String pathogenShortCode = pathogenDTO.getCodeDisplay().getCode();
    final List<SpecimenDTO> specimenDTOList = pathogenDTO.getSpecimenList();
    if (specimenDTOList == null || specimenDTOList.isEmpty()) {
      return returnList;
    }

    // Iterate through the specimen DTOs and build the Specimen object.
    for (SpecimenDTO specimenDTO : specimenDTOList) {
      Specimen specimen = null;
      SpecimenDataBuilder specimenDataBuilder =
          new SpecimenDataBuilder()
              .setDefaultData()
              .setProfileUrlHelper(pathogenShortCode)
              .setReceivedTime(createDate(specimenDTO.getReceivedDate()))
              .setTypeCode(specimenDTO.getMaterial().getCode())
              .setTypeDisplay(specimenDTO.getMaterial().getDisplay())
              .setNotifiedPerson(patient)
              .setSubmittingRole(submittingRole);

      // Set the collected date if available.
      if (specimenDTO.getExtractionDate() != null) {
        specimenDataBuilder.setCollectedDate(createDate(specimenDTO.getExtractionDate()));
      }
      specimen = specimenDataBuilder.build();

      // Add observations for pathogen detection.
      observation.addAll(
          createObservation(
              pathogenDTO,
              specimenDTO.getMethodPathogenList(),
              patient,
              specimen,
              notificationLaboratoryCategory));

      // Add observations for resistance genes.
      observation.addAll(
          createObservationsForResistanceGenes(
              specimenDTO.getResistanceGeneList(),
              patient,
              specimen,
              pathogenShortCode,
              featureFlagSnapshot5_3_0Active));

      // Add observations for resistances.
      observation.addAll(
          createObservationsForResistances(
              specimenDTO.getResistanceList(), patient, specimen, pathogenShortCode));
      returnList.add(specimen);
    }
    return returnList;
  }
}
