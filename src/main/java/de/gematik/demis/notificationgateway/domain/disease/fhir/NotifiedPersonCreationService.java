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

import de.gematik.demis.notification.builder.demis.fhir.notification.utils.Utils;
import de.gematik.demis.notificationgateway.common.dto.AddressType;
import de.gematik.demis.notificationgateway.common.dto.ContactPointInfo;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPerson;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPersonAddressInfo;
import de.gematik.demis.notificationgateway.common.services.fhir.FhirObjectCreationService;
import de.gematik.demis.notificationgateway.common.utils.DateUtils;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.HumanName.NameUse;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
class NotifiedPersonCreationService {

  private final FhirObjectCreationService fhirObjectCreationService;
  private final OrganizationCreationService organizationCreationService;

  private static void setGender(Patient notifiedPerson, NotifiedPerson notifiedPersonContent) {
    notifiedPerson.setGender(
        AdministrativeGender.valueOf(notifiedPersonContent.getInfo().getGender().getValue()));
  }

  public Patient createPatient(
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

  private void addResidenceAddress(Patient notifiedPerson, NotifiedPerson notifiedPersonContent) {
    final NotifiedPersonAddressInfo info = notifiedPersonContent.getResidenceAddress();
    if (info != null) {
      notifiedPerson.addAddress(fhirObjectCreationService.createAddress(info, true));
    }
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
