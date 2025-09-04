package de.gematik.demis.notificationgateway.domain.pathogen.fhir;

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
import static de.gematik.demis.notificationgateway.domain.pathogen.creator.AddressCreator.createAddress;
import static de.gematik.demis.notificationgateway.domain.pathogen.creator.AddressCreator.createAddressWithoutAddressUse;
import static de.gematik.demis.notificationgateway.domain.pathogen.creator.BundleCreator.createBundle;

import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.laboratory.NotificationBundleLaboratoryDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.laboratory.SpecimenDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.OrganizationBuilder;
import de.gematik.demis.notificationgateway.FeatureFlags;
import de.gematik.demis.notificationgateway.common.dto.*;
import de.gematik.demis.notificationgateway.common.enums.NotificationType;
import de.gematik.demis.notificationgateway.common.mappers.BundleMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class PathogenBundleCreationService implements BundleMapper {

  private final FeatureFlags featureFlags;

  /**
   * Create a patient and additional resources if necessary
   *
   * @param bundleBuilder Required to add additional resources that might be referenced by Patient
   * @param rawData The raw data to retrieve additional resources from
   * @param submittingRole The submitting role should be created before calling this method
   */
  private Patient createPatient(
      final NotificationBundleLaboratoryDataBuilder bundleBuilder,
      PathogenTest rawData,
      final PractitionerRole submittingRole) {
    final Address whereabouts;
    final NotifiedPerson rawPatientData = rawData.getNotifiedPerson();
    final NotifiedPersonAddressInfo whereaboutsInfo = rawPatientData.getCurrentAddress();
    switch (whereaboutsInfo.getAddressType()) {
      case OTHER_FACILITY:
        final Address address = createAddressWithoutAddressUse(whereaboutsInfo);
        Organization otherFacility =
            new OrganizationBuilder()
                .setDefaults()
                .setMetaProfileUrl(
                    "https://demis.rki.de/fhir/StructureDefinition/NotifiedPersonFacility")
                .setFacilityName(whereaboutsInfo.getAdditionalInfo())
                .setAddress(address)
                .build();
        bundleBuilder.addAdditionalEntry(otherFacility);
        whereabouts = createAddress(whereaboutsInfo, otherFacility);
        break;
      case SUBMITTING_FACILITY:
        // Using submittingRole.getOrganizationTarget() won't reference the correct entity here
        final Organization resource = (Organization) submittingRole.getOrganization().getResource();
        whereabouts = createAddress(whereaboutsInfo, resource);
        break;
      default:
        whereabouts = createAddress(whereaboutsInfo);
    }

    final List<Address> addresses =
        List.of(whereabouts, createAddress(rawPatientData.getResidenceAddress()));
    final Patient patient;
    patient = createPatient(rawPatientData, addresses);

    return patient;
  }

  /**
   * Creates a complete FHIR {@link Bundle} that can be sent to the DEMIS core services.
   *
   * <p>This method uses the provided {@link NotificationType} to determine the type of notification
   * to create, which corresponds to different notification paragraphs.
   *
   * @param pathogenTest The {@link PathogenTest} object containing the data for the notification.
   * @param notificationType The {@link NotificationType} specifying the type of notification.
   * @return A {@link Bundle} object representing the complete notification.
   */
  public Bundle toBundle(PathogenTest pathogenTest, NotificationType notificationType) {
    return createBundle(
        pathogenTest,
        notificationType,
        featureFlags.isSnapshot530Active(),
        featureFlags.isFollowUpNotificationActive());
  }

  /**
   * @deprecated this only works for §7.1 notifications Creates FHIR bundleBuilder from POJO input
   * @param pathogenTest pathogen test business details
   * @return FHIR bundleBuilder
   */
  @Deprecated(forRemoval = true)
  public Bundle toBundle(PathogenTest pathogenTest) {
    final NotifiedPerson notifiedPerson = pathogenTest.getNotifiedPerson();
    // ggf. NICHT übernehmen!
    final PathogenDTO pathogenDTO = pathogenTest.getPathogenDTO();
    final NotificationLaboratoryCategory notificationLaboratoryCategory =
        pathogenTest.getNotificationCategory();
    final NotifierFacility notifierFacility = pathogenTest.getNotifierFacility();
    final PractitionerRole notifierRole = createNotifierPractitionerRole(notifierFacility);
    final PractitionerRole submittingRole;
    final SubmitterFacility submitterFacility = pathogenTest.getSubmittingFacility();
    // TODO handle null here somehow
    final NotifiedPersonAddressInfo currentAddress =
        Objects.requireNonNull(notifiedPerson.getCurrentAddress());
    boolean isNotifiedPersonFacility =
        currentAddress.getAddressType().equals(AddressType.SUBMITTING_FACILITY);
    submittingRole = createSubmitterPractitionerRole(submitterFacility, isNotifiedPersonFacility);

    final NotificationBundleLaboratoryDataBuilder bundleBuilder =
        new NotificationBundleLaboratoryDataBuilder().setDefaults();
    final Patient patient = createPatient(bundleBuilder, pathogenTest, submittingRole);

    List<Observation> observation = new ArrayList<>();
    List<Specimen> specimenList = new ArrayList<>();

    addSpecimen(
        pathogenDTO,
        patient,
        submittingRole,
        specimenList,
        observation,
        notificationLaboratoryCategory);

    final DiagnosticReport diagnosticReport =
        createDiagnosticReport(pathogenDTO, patient, observation, notificationLaboratoryCategory);

    return bundleBuilder
        .setPathogenDetection(observation)
        .setSubmitterRole(submittingRole)
        .setNotifierRole(notifierRole)
        .setNotifiedPerson(patient)
        .setSpecimen(specimenList)
        .setNotificationLaboratory(
            createComposition(
                patient,
                notifierRole,
                diagnosticReport,
                pathogenDTO,
                notificationLaboratoryCategory))
        .setLaboratoryReport(diagnosticReport)
        .build();
  }

  private void addSpecimen(
      PathogenDTO pathogenDTO,
      Patient patient,
      PractitionerRole submittingRole,
      List<Specimen> specimenList,
      List<Observation> observation,
      NotificationLaboratoryCategory notificationLaboratoryCategory) {
    final String pathogenShortCode = pathogenDTO.getCodeDisplay().getCode();
    final List<SpecimenDTO> specimenDTOList = pathogenDTO.getSpecimenList();

    for (SpecimenDTO specimenDTO : specimenDTOList) {
      SpecimenDataBuilder specimenDataBuilder =
          new SpecimenDataBuilder()
              .setDefaultData()
              .setProfileUrlHelper(pathogenShortCode)
              .setReceivedTime(createDate(specimenDTO.getReceivedDate()))
              .setTypeCode(specimenDTO.getMaterial().getCode())
              .setTypeDisplay(specimenDTO.getMaterial().getDisplay())
              .setNotifiedPerson(patient)
              .setSubmittingRole(submittingRole);

      if (specimenDTO.getExtractionDate() != null) {
        specimenDataBuilder.setCollectedDate(createDate(specimenDTO.getExtractionDate()));
      }
      Specimen specimen = specimenDataBuilder.build();
      specimenList.add(specimen);

      observation.addAll(
          createObservation(
              pathogenDTO,
              specimenDTO.getMethodPathogenList(),
              patient,
              specimen,
              notificationLaboratoryCategory));

      createObservationsForResistanceGenes(
          specimenDTO.getResistanceGeneList(),
          patient,
          specimen,
          observation,
          pathogenShortCode,
          featureFlags.isSnapshot530Active());
      createObservationsForResistances(
          specimenDTO.getResistanceList(), patient, specimen, observation, pathogenShortCode);
    }
  }
}
