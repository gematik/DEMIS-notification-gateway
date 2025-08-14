package de.gematik.demis.notificationgateway.domain.disease.fhir;

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

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.PROFILE_NOTIFIED_PERSON;
import static de.gematik.demis.notificationgateway.common.creator.HumanNameCreator.createHumanName;
import static java.util.Arrays.asList;

import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.NotifiedPersonAnonymousDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.NotifiedPersonNominalDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.AddressDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.igs.InvalidInputDataException;
import de.gematik.demis.notification.builder.demis.fhir.notification.utils.Utils;
import de.gematik.demis.notificationgateway.common.dto.AddressType;
import de.gematik.demis.notificationgateway.common.dto.ContactPointInfo;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPerson;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPersonAddressInfo;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPersonAnonymous;
import de.gematik.demis.notificationgateway.common.services.fhir.FhirObjectCreationService;
import de.gematik.demis.notificationgateway.common.utils.DateUtils;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.HumanName.NameUse;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.stereotype.Service;

@Service
class NotifiedPersonCreationService {

  private final FhirObjectCreationService fhirObjectCreationService;
  private final OrganizationCreationService organizationCreationService;

  NotifiedPersonCreationService(
      FhirObjectCreationService fhirObjectCreationService,
      OrganizationCreationService organizationCreationService) {
    this.fhirObjectCreationService = fhirObjectCreationService;
    this.organizationCreationService = organizationCreationService;
  }

  private static void setGender(Patient notifiedPerson, NotifiedPerson notifiedPersonContent) {
    notifiedPerson.setGender(
        AdministrativeGender.valueOf(notifiedPersonContent.getInfo().getGender().getValue()));
  }

  /**
   * Creates a FHIR Patient resource based on the provided notified person content.
   *
   * <p>This method supports two types of notified person content: - `NotifiedPerson`: Creates a
   * nominal patient with detailed information such as name, gender, birthdate, addresses, and
   * contact points. - `NotifiedPersonAnonymous`: Creates an anonymous patient with limited
   * information such as gender, birthdate, and residence address.
   *
   * @return A `Patient` resource representing the notified person.
   * @throws InvalidInputDataException If the type of `notifiedPersonContent` is not supported.
   */
  public Patient createPatient(Object notifiedPersonContent, PractitionerRole practitionerRole) {
    return switch (notifiedPersonContent) {
      case NotifiedPerson notifiedPerson -> {
        // nominal
        List<Address> addresses =
            asList(
                createCurrentAddress(notifiedPerson, practitionerRole),
                createResidenceAddress(notifiedPerson));
        yield new NotifiedPersonNominalDataBuilder()
            .setDefault()
            .setBirthdate(
                new DateType(DateUtils.createDate(notifiedPerson.getInfo().getBirthDate())))
            .setGender(
                Enumerations.AdministrativeGender.valueOf(
                    notifiedPerson.getInfo().getGender().getValue()))
            .setHumanName(createHumanName(notifiedPerson.getInfo()))
            .setTelecom(createContacts(notifiedPerson))
            .setAddress(addresses)
            .build();
      }
      case NotifiedPersonAnonymous notifiedPersonAnonymous -> {
        // anonymous
        var residenceAddress = notifiedPersonAnonymous.getResidenceAddress();
        var addressBuilder = new AddressDataBuilder();
        if (residenceAddress != null) {
          addressBuilder
              .setCountry(residenceAddress.getCountry())
              .setPostalCode(residenceAddress.getZip());
          AddressType addressType = residenceAddress.getAddressType();
          if (addressType != null) {
            addressBuilder.withAddressUseExtension(addressType.getValue());
          }
        }
        yield new NotifiedPersonAnonymousDataBuilder()
            .setDefault()
            .setGender(
                Enumerations.AdministrativeGender.valueOf(
                    notifiedPersonAnonymous.getGender().getValue()))
            .setBirthdate(new DateType(notifiedPersonAnonymous.getBirthDate()))
            .addAddress(addressBuilder.build())
            .build();
      }
      default ->
          throw new InvalidInputDataException(
              "NotifiedPersonContent type not supported: " + notifiedPersonContent.getClass());
    };
  }

  /**
   * Creates a FHIR Patient resource based on the provided notified person content.
   *
   * <p>This method is marked as deprecated and will be removed in future versions. It creates a
   * `Patient` resource using detailed information from a `NotifiedPerson` object. The resource
   * includes attributes such as ID, metadata, gender, name, birthdate, contact points, current
   * address, and residence address.
   *
   * @param notifiedPersonContent The `NotifiedPerson` object containing the details of the notified
   *     person.
   * @param practitionerRole The `PractitionerRole` associated with the notified person, used for
   *     creating specific address types.
   * @return A `Patient` resource representing the notified person.
   * @deprecated This method is deprecated and scheduled for removal. Use the updated method for
   *     creating `Patient` resources.
   */
  @Deprecated(forRemoval = true)
  public Patient createPatientLegacy(
      NotifiedPerson notifiedPersonContent, PractitionerRole practitionerRole) {
    final Patient notifiedPerson = new Patient();
    notifiedPerson.setId(Utils.generateUuidString());
    notifiedPerson.setMeta(new Meta().addProfile(PROFILE_NOTIFIED_PERSON));
    setGender(notifiedPerson, notifiedPersonContent);
    addName(notifiedPerson, notifiedPersonContent);
    addBirthDate(notifiedPerson, notifiedPersonContent);
    addContacts(notifiedPerson, notifiedPersonContent);
    addCurrentAddress(notifiedPerson, notifiedPersonContent, practitionerRole);
    addResidenceAddress(notifiedPerson, notifiedPersonContent);
    return notifiedPerson;
  }

  private void addName(Patient notifiedPerson, NotifiedPerson notifiedPersonContent) {
    final String firstnameInfo = notifiedPersonContent.getInfo().getFirstname();
    List<StringType> firstNameList = new ArrayList<>();
    for (String firstName : firstnameInfo.split("\\s+")) {
      firstNameList.add(new StringType(firstName));
    }
    notifiedPerson
        .addName()
        .setUse(NameUse.OFFICIAL)
        .setFamily(notifiedPersonContent.getInfo().getLastname())
        .setGiven(firstNameList);
  }

  private void addBirthDate(Patient notifiedPerson, NotifiedPerson notifiedPersonContent) {
    final LocalDate dateOfBirth = notifiedPersonContent.getInfo().getBirthDate();
    if (dateOfBirth != null) {
      notifiedPerson.setBirthDate(DateUtils.createDate(dateOfBirth));
    }
  }

  private void addContacts(Patient notifiedPerson, NotifiedPerson notifiedPersonContent) {
    for (ContactPointInfo contact : notifiedPersonContent.getContacts()) {
      ContactPoint contactPoint = fhirObjectCreationService.createContactPoint(contact);
      notifiedPerson.addTelecom(contactPoint);
    }
  }

  private List<ContactPoint> createContacts(NotifiedPerson notifiedPersonContent) {
    return notifiedPersonContent.getContacts().stream()
        .map(fhirObjectCreationService::createContactPoint)
        .toList();
  }

  private void addResidenceAddress(Patient notifiedPerson, NotifiedPerson notifiedPersonContent) {
    final NotifiedPersonAddressInfo info = notifiedPersonContent.getResidenceAddress();
    if (info != null) {
      notifiedPerson.addAddress(fhirObjectCreationService.createAddress(info, true));
    }
  }

  private Address createResidenceAddress(NotifiedPerson notifiedPersonContent) {
    final NotifiedPersonAddressInfo info = notifiedPersonContent.getResidenceAddress();
    return fhirObjectCreationService.createAddress(info, true);
  }

  private void addCurrentAddress(
      Patient notifiedPerson,
      NotifiedPerson notifiedPersonContent,
      PractitionerRole practitionerRole) {
    final NotifiedPersonAddressInfo currentAddress = notifiedPersonContent.getCurrentAddress();
    if (currentAddress != null) {
      final Address address;
      final AddressType addressType = currentAddress.getAddressType();
      if (addressType == AddressType.OTHER_FACILITY) {
        address = createOtherFacilityAddress(currentAddress);
      } else if (addressType == AddressType.SUBMITTING_FACILITY) {
        address = createNotifierFacilityAddress(practitionerRole, currentAddress);
      } else {
        address = fhirObjectCreationService.createAddress(currentAddress);
      }
      notifiedPerson.addAddress(address);
    }
  }

  private Address createCurrentAddress(
      NotifiedPerson notifiedPersonContent, PractitionerRole practitionerRole) {
    final NotifiedPersonAddressInfo currentAddress = notifiedPersonContent.getCurrentAddress();
    final Address address;
    if (currentAddress != null) {
      final AddressType addressType = currentAddress.getAddressType();
      if (addressType == AddressType.OTHER_FACILITY) {
        address = createOtherFacilityAddress(currentAddress);
      } else if (addressType == AddressType.SUBMITTING_FACILITY) {
        address = createNotifierFacilityAddress(practitionerRole, currentAddress);
      } else {
        address = fhirObjectCreationService.createAddress(currentAddress);
      }
      return address;
    } else {
      throw new InvalidInputDataException(
          "No current address found while creating notifiedPerson Resource");
    }
  }

  private Address createOtherFacilityAddress(NotifiedPersonAddressInfo currentAddress) {
    return fhirObjectCreationService.createAddressWithReferenceToOrganization(
        currentAddress, organizationCreationService.createNotifiedPersonFacility(currentAddress));
  }

  private Address createNotifierFacilityAddress(
      PractitionerRole practitionerRole, NotifiedPersonAddressInfo currentAddress) {
    return fhirObjectCreationService.createAddressWithReferenceToOrganization(
        currentAddress, organizationCreationService.createNotifiedPersonFacility(practitionerRole));
  }
}
