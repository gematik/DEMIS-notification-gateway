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

package de.gematik.demis.notificationgateway.common.services.fhir;

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.PROFILE_NOTIFIED_PERSON;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.STRUCTURE_DEFINITION_ADDRESS_USE;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.STRUCTURE_DEFINITION_FACILITY_ADDRESS_NOTIFIED_PERSON;

import de.gematik.demis.notificationgateway.common.dto.ContactPointInfo;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPerson;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPersonAddressInfo;
import de.gematik.demis.notificationgateway.common.utils.ConfiguredCodeSystems;
import de.gematik.demis.notificationgateway.common.utils.DateUtils;
import de.gematik.demis.notificationgateway.common.utils.ReferenceUtils;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.HumanName.NameUse;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class NotifiedPersonCreationService {

  private final FhirObjectCreationService fhirObjectCreationService;

  public Patient createNotifiedPerson(NotifiedPerson notifiedPersonContent) {
    final Patient notifiedPerson = createNotifiedPersonWithoutAddresses(notifiedPersonContent);
    addAddresses(notifiedPerson, notifiedPersonContent);

    return notifiedPerson;
  }

  public Patient createNotifiedPersonHospitalizedInFacility(
      NotifiedPerson notifiedPersonContent, Organization notifiedPersonFacility) {
    final Patient notifiedPerson = createNotifiedPersonWithoutAddresses(notifiedPersonContent);
    addAddress(notifiedPerson, notifiedPersonContent.getPrimaryAddress());

    final Address currentFacilityAddress = new Address();
    currentFacilityAddress
        .addExtension()
        .setUrl(STRUCTURE_DEFINITION_ADDRESS_USE)
        .setValue(ConfiguredCodeSystems.getInstance().getAddressUseCoding("current"));

    currentFacilityAddress
        .addExtension()
        .setUrl(STRUCTURE_DEFINITION_FACILITY_ADDRESS_NOTIFIED_PERSON)
        .setValue(ReferenceUtils.createReference(notifiedPersonFacility));
    notifiedPerson.addAddress(currentFacilityAddress);

    return notifiedPerson;
  }

  private Patient createNotifiedPersonWithoutAddresses(NotifiedPerson notifiedPersonContent) {
    final Patient notifiedPerson = new Patient();
    notifiedPerson.setId(UUID.randomUUID().toString());
    notifiedPerson.setMeta(new Meta().addProfile(PROFILE_NOTIFIED_PERSON));

    notifiedPerson.setGender(
        AdministrativeGender.valueOf(notifiedPersonContent.getInfo().getGender().getValue()));

    addName(notifiedPerson, notifiedPersonContent);

    final LocalDate dateOfBirth = notifiedPersonContent.getInfo().getBirthDate();
    if (dateOfBirth != null) {
      notifiedPerson.setBirthDate(DateUtils.createDate(dateOfBirth));
    }

    addContacts(notifiedPerson, notifiedPersonContent.getContacts());
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

  private void addAddresses(Patient notifiedPerson, NotifiedPerson notifiedPersonContent) {
    addAddress(notifiedPerson, notifiedPersonContent.getCurrentAddress());
    addAddress(notifiedPerson, notifiedPersonContent.getPrimaryAddress());
    addAddress(notifiedPerson, notifiedPersonContent.getOrdinaryAddress());
  }

  @Nullable
  private Address addAddress(
      Patient notifiedPerson, @Nullable NotifiedPersonAddressInfo addressInfo) {
    if (addressInfo == null) {
      return null;
    } else {
      final Address fhirAddress = fhirObjectCreationService.createAddress(addressInfo, true);
      notifiedPerson.addAddress(fhirAddress);
      return fhirAddress;
    }
  }

  private void addContacts(Patient notifiedPerson, List<ContactPointInfo> contacts) {
    for (ContactPointInfo contact : contacts) {
      ContactPoint contactPoint = fhirObjectCreationService.createContactPoint(contact);
      notifiedPerson.addTelecom(contactPoint);
    }
  }
}
