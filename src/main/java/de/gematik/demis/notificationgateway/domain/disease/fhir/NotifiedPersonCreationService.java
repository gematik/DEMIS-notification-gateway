package de.gematik.demis.notificationgateway.domain.disease.fhir;

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

import static de.gematik.demis.notificationgateway.common.creator.HumanNameCreator.createHumanName;
import static de.gematik.demis.notificationgateway.common.mappers.GenderMapper.createGenderExtension;
import static de.gematik.demis.notificationgateway.common.mappers.GenderMapper.mapGender;

import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.NotifiedPersonAnonymousDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.NotifiedPersonNominalDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.AddressDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.igs.InvalidInputDataException;
import de.gematik.demis.notificationgateway.common.dto.AddressType;
import de.gematik.demis.notificationgateway.common.dto.Gender;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPerson;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPersonAddressInfo;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPersonAnonymous;
import de.gematik.demis.notificationgateway.common.services.fhir.FhirObjectCreationService;
import de.gematik.demis.notificationgateway.common.utils.DateUtils;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;
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
    if (notifiedPersonContent instanceof NotifiedPerson notifiedPerson) {
      List<Address> addresses = new ArrayList<>();
      if (notifiedPerson.getCurrentAddress() != null) {
        addresses.add(createCurrentAddress(notifiedPerson, practitionerRole));
      }
      if (notifiedPerson.getResidenceAddress() != null) {
        addresses.add(createResidenceAddress(notifiedPerson));
      }
      if (addresses.isEmpty()) {
        throw new InvalidInputDataException("Residence address of patient cannot be null");
      }
      Gender gender = notifiedPerson.getInfo().getGender();
      Patient patient =
          new NotifiedPersonNominalDataBuilder()
              .setDefault()
              .setBirthdate(
                  new DateType(DateUtils.createDate(notifiedPerson.getInfo().getBirthDate())))
              .setGender(mapGender(gender))
              .setHumanName(createHumanName(notifiedPerson.getInfo()))
              .setTelecom(createContacts(notifiedPerson))
              .setAddress(addresses)
              .build();
      createGenderExtension(gender)
          .ifPresent(extension -> patient.getGenderElement().addExtension(extension));
      return patient;
    }
    if (notifiedPersonContent instanceof NotifiedPersonAnonymous notifiedPersonAnonymous) {
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
      Gender gender = notifiedPersonAnonymous.getGender();

      Patient patient =
          new NotifiedPersonAnonymousDataBuilder()
              .setDefault()
              .setGender(mapGender(gender))
              .setBirthdate(new DateType(notifiedPersonAnonymous.getBirthDate()))
              .addAddress(addressBuilder.build())
              .build();
      createGenderExtension(gender)
          .ifPresent(extension -> patient.getGenderElement().addExtension(extension));
      return patient;
    }
    throw new InvalidInputDataException(
        "NotifiedPersonContent type not supported: " + notifiedPersonContent.getClass());
  }

  private List<ContactPoint> createContacts(NotifiedPerson notifiedPersonContent) {
    return notifiedPersonContent.getContacts().stream()
        .map(fhirObjectCreationService::createContactPoint)
        .toList();
  }

  private Address createResidenceAddress(NotifiedPerson notifiedPersonContent) {
    final NotifiedPersonAddressInfo info = notifiedPersonContent.getResidenceAddress();
    return fhirObjectCreationService.createAddress(info, true);
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
