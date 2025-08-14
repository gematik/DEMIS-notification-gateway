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

import static de.gematik.demis.notificationgateway.common.creator.HumanNameCreator.createHumanName;

import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.NotifiedPersonAnonymousDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.NotifiedPersonNominalDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.laboratory.NotificationBundleLaboratoryDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.OrganizationBuilder;
import de.gematik.demis.notificationgateway.common.creator.ContactPointCreator;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPerson;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPersonAddressInfo;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPersonAnonymous;
import de.gematik.demis.notificationgateway.common.dto.PathogenTest;
import de.gematik.demis.notificationgateway.common.utils.DateUtils;
import java.util.List;
import java.util.Objects;
import org.hl7.fhir.r4.model.*;

/**
 * Utility class for creating FHIR {@link Patient} objects.
 *
 * <p>This class provides methods to build a {@link Patient} instance based on the provided {@link
 * PathogenTest} and related data.
 */
public class PatientCreator {

  /**
   * Private constructor to prevent instantiation of this utility class.
   *
   * <p>This class is designed as a utility class and should not be instantiated.
   */
  private PatientCreator() {}

  /**
   * Creates a FHIR {@link Patient} object using the provided parameters.
   *
   * @param bundleBuilder The {@link NotificationBundleLaboratoryDataBuilder} used to build the
   *     notification bundleBuilder.
   * @param rawData The {@link PathogenTest} object containing data about the notified person.
   * @param submittingRole The {@link PractitionerRole} object representing the submitting facility.
   * @return A {@link Patient} object populated with the provided data.
   * @throws IllegalArgumentException if the current address of the patient is null.
   */
  public static Patient createPatient(
      NotificationBundleLaboratoryDataBuilder bundleBuilder,
      PathogenTest rawData,
      PractitionerRole submittingRole,
      boolean featureFlagFollowUpActive) {

    if (featureFlagFollowUpActive) {

      return createPatientWithFollowUpOptions(bundleBuilder, rawData, submittingRole);
    } else {
      return legacyPatientCreation(bundleBuilder, rawData, submittingRole);
    }
  }

  private static Patient legacyPatientCreation(
      NotificationBundleLaboratoryDataBuilder bundleBuilder,
      PathogenTest rawData,
      PractitionerRole submittingRole) {
    NotifiedPerson rawPatientData = rawData.getNotifiedPerson();
    NotifiedPersonAddressInfo whereaboutsInfo = rawPatientData.getCurrentAddress();

    if (whereaboutsInfo == null) {
      throw new IllegalArgumentException("Current address of patient cannot be null");
    }

    Address whereabouts = createWhereaboutsAddress(bundleBuilder, whereaboutsInfo, submittingRole);

    List<Address> addresses =
        List.of(whereabouts, AddressCreator.createAddress(rawPatientData.getResidenceAddress()));

    NotifiedPersonNominalDataBuilder patientBuilder =
        new NotifiedPersonNominalDataBuilder()
            .setDefault()
            .setBirthdate(
                new DateType(DateUtils.createDate(rawPatientData.getInfo().getBirthDate())))
            .setHumanName(createHumanName(rawPatientData.getInfo()))
            .setGender(
                Enumerations.AdministrativeGender.valueOf(
                    rawPatientData.getInfo().getGender().getValue()));

    addresses.stream().filter(Objects::nonNull).forEach(patientBuilder::addAddress);
    rawPatientData.getContacts().stream()
        .map(ContactPointCreator::createContactPoint)
        .forEach(patientBuilder::addTelecom);

    return patientBuilder.build();
  }

  private static Patient createPatientWithFollowUpOptions(
      NotificationBundleLaboratoryDataBuilder bundleBuilder,
      PathogenTest rawData,
      PractitionerRole submittingRole) {
    if (rawData.getNotifiedPerson() != null) {
      return createNotifiedPersonNominal(bundleBuilder, rawData, submittingRole);
    } else {
      return createNotifiedPersonAnonymous(rawData);
    }
  }

  private static Patient createNotifiedPersonAnonymous(PathogenTest rawData) {
    NotifiedPersonAnonymous notifiedPersonAnonymous = rawData.getNotifiedPersonAnonymous();
    if (notifiedPersonAnonymous == null) {
      throw new IllegalArgumentException("NotifiedPersonAnonymous cannot be null");
    }
    String zip =
        notifiedPersonAnonymous.getResidenceAddress() != null
            ? notifiedPersonAnonymous.getResidenceAddress().getZip()
            : null;
    String country =
        notifiedPersonAnonymous.getResidenceAddress() != null
            ? notifiedPersonAnonymous.getResidenceAddress().getCountry()
            : null;
    return new NotifiedPersonAnonymousDataBuilder()
        .setDefault()
        .addAddress(new Address().setPostalCode(zip).setCountry(country))
        .setGender(
            Enumerations.AdministrativeGender.valueOf(
                notifiedPersonAnonymous.getGender().getValue()))
        .setBirthdate(new DateType(notifiedPersonAnonymous.getBirthDate()))
        .build();
  }

  private static Patient createNotifiedPersonNominal(
      NotificationBundleLaboratoryDataBuilder bundleBuilder,
      PathogenTest rawData,
      PractitionerRole submittingRole) {
    NotifiedPerson rawPatientData = rawData.getNotifiedPerson();
    NotifiedPersonAddressInfo whereaboutsInfo = rawPatientData.getCurrentAddress();

    if (whereaboutsInfo == null) {
      throw new IllegalArgumentException("Current address of patient cannot be null");
    }

    Address whereabouts = createWhereaboutsAddress(bundleBuilder, whereaboutsInfo, submittingRole);

    List<Address> addresses =
        List.of(whereabouts, AddressCreator.createAddress(rawPatientData.getResidenceAddress()));

    NotifiedPersonNominalDataBuilder patientBuilder =
        new NotifiedPersonNominalDataBuilder()
            .setHumanName(createHumanName(rawPatientData.getInfo()))
            .setDefault()
            .setBirthdate(
                new DateType(DateUtils.createDate(rawPatientData.getInfo().getBirthDate())))
            .setGender(
                Enumerations.AdministrativeGender.valueOf(
                    rawPatientData.getInfo().getGender().getValue()));
    addresses.stream().filter(Objects::nonNull).forEach(patientBuilder::addAddress);
    rawPatientData.getContacts().stream()
        .map(ContactPointCreator::createContactPoint)
        .forEach(patientBuilder::addTelecom);

    return patientBuilder.build();
  }

  /**
   * Creates a FHIR {@link Address} object for the whereabouts of the notified person.
   *
   * @param bundleBuilder The {@link NotificationBundleLaboratoryDataBuilder} used to build the
   *     notification bundleBuilder.
   * @param whereaboutsInfo The {@link NotifiedPersonAddressInfo} object containing address details.
   * @param submittingRole The {@link PractitionerRole} object representing the submitting facility.
   * @return A {@link Address} object populated with the provided data.
   */
  private static Address createWhereaboutsAddress(
      NotificationBundleLaboratoryDataBuilder bundleBuilder,
      NotifiedPersonAddressInfo whereaboutsInfo,
      PractitionerRole submittingRole) {

    switch (whereaboutsInfo.getAddressType()) {
      case OTHER_FACILITY:
        Address address = AddressCreator.createAddressWithoutAddressUse(whereaboutsInfo);
        Organization otherFacility =
            new OrganizationBuilder()
                .setDefaults()
                .setMetaProfileUrl(
                    "https://demis.rki.de/fhir/StructureDefinition/NotifiedPersonFacility")
                .setFacilityName(whereaboutsInfo.getAdditionalInfo())
                .setAddress(address)
                .build();
        bundleBuilder.addAdditionalEntry(otherFacility);
        return AddressCreator.createAddress(whereaboutsInfo, otherFacility);

      case SUBMITTING_FACILITY:
        Organization resource = (Organization) submittingRole.getOrganization().getResource();
        return AddressCreator.createAddress(whereaboutsInfo, resource);

      default:
        return AddressCreator.createAddress(whereaboutsInfo);
    }
  }
}
