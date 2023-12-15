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

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.NAMING_SYSTEM_BSNR;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.PROFILE_NOTIFIED_PERSON_FACILITY;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.PROFILE_NOTIFIER_FACILITY;

import de.gematik.demis.notificationgateway.common.constants.FhirConstants;
import de.gematik.demis.notificationgateway.common.dto.ContactPointInfo;
import de.gematik.demis.notificationgateway.common.dto.ContactsInfo;
import de.gematik.demis.notificationgateway.common.dto.InfectionProtectionFacilityInfo;
import de.gematik.demis.notificationgateway.common.dto.LabInfo;
import de.gematik.demis.notificationgateway.common.dto.NotifierFacility;
import de.gematik.demis.notificationgateway.common.dto.PractitionerInfo;
import de.gematik.demis.notificationgateway.common.dto.PractitionerInfo.SalutationEnum;
import de.gematik.demis.notificationgateway.common.utils.ConfiguredCodeSystems;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class OrganizationCreationService {

  private final FhirObjectCreationService fhirObjectCreationService;

  public Organization createLabNotifierFacility(NotifierFacility notifierFacilityContent) {
    return createFacility(notifierFacilityContent, PROFILE_NOTIFIER_FACILITY, "othPrivatLab");
  }

  public Organization createHospitalNotifierFacility(NotifierFacility notifierFacilityContent) {
    return createFacility(notifierFacilityContent, PROFILE_NOTIFIER_FACILITY, "hospital");
  }

  public Organization createNotifiedPersonFacility(NotifierFacility notifierFacilityContent) {
    return createFacility(notifierFacilityContent, PROFILE_NOTIFIED_PERSON_FACILITY, "hospital");
  }

  public Optional<Organization> createLab(@Nullable LabInfo labInfo) {
    if (labInfo == null) {
      return Optional.empty();
    }

    Organization lab = new Organization();
    lab.setId(UUID.randomUUID().toString());
    lab.setMeta(new Meta().addProfile(FhirConstants.PROFILE_ORGANIZATION));

    lab.setName(labInfo.getName());
    final Address fhirAddress =
        fhirObjectCreationService.createAddress(labInfo.getAddress(), false);
    lab.addAddress(fhirAddress);

    return Optional.of(lab);
  }

  public Optional<Organization> createInfectProtectFacility(
      @Nullable InfectionProtectionFacilityInfo infectProtectFacilityInfo) {
    if (infectProtectFacilityInfo == null) {
      return Optional.empty();
    }

    Organization infectProtectFacility = new Organization();
    infectProtectFacility.setId(UUID.randomUUID().toString());
    infectProtectFacility.setMeta(new Meta().addProfile(FhirConstants.PROFILE_ORGANIZATION));

    final String facilityName = infectProtectFacilityInfo.getFacilityName();
    if (StringUtils.isNotBlank(facilityName)) {
      infectProtectFacility.setName(facilityName);
    }

    final Address fhirAddress =
        fhirObjectCreationService.createAddress(infectProtectFacilityInfo.getAddress(), false);
    infectProtectFacility.addAddress(fhirAddress);

    ContactsInfo contactsInfo = infectProtectFacilityInfo.getContactsInfo();
    List<ContactPoint> contactsInfos = fhirObjectCreationService.createContactPoints(contactsInfo);
    infectProtectFacility.setTelecom(contactsInfos);

    return Optional.of(infectProtectFacility);
  }

  private Organization createFacility(
      NotifierFacility notifierFacilityContent, String profile, String organizationTypeCode) {
    final Organization notifierFacility = new Organization();
    notifierFacility.setId(UUID.randomUUID().toString());
    notifierFacility.setMeta(new Meta().addProfile(profile));

    addBSNR(notifierFacility, notifierFacilityContent);
    addOrganizationType(notifierFacility, organizationTypeCode);
    addAddress(notifierFacility, notifierFacilityContent);

    notifierFacility.setName(notifierFacilityContent.getFacilityInfo().getInstitutionName());

    addContacts(notifierFacility, notifierFacilityContent.getContacts());
    addContact(notifierFacility, notifierFacilityContent.getContact());

    return notifierFacility;
  }

  private void addContact(Organization notifierFacility, PractitionerInfo contact) {
    final String firstnameInfo = contact.getFirstname();
    final String lastname = contact.getLastname();

    List<StringType> firstNameList = new ArrayList<>();
    for (String firstName : firstnameInfo.split("\\s+")) {
      firstNameList.add(new StringType(firstName));
    }

    final HumanName name = new HumanName().setFamily(lastname).setGiven(firstNameList);

    StringBuilder fullNameTextBuilder = new StringBuilder();
    final SalutationEnum salutation = contact.getSalutation();
    if (salutation != null) {
      switch (salutation) {
        case MR:
          fullNameTextBuilder.append("Herr").append(' ');
          break;
        case MRS:
          fullNameTextBuilder.append("Frau").append(' ');
          break;
        default:
          throw new IllegalStateException("unkown salutation type: " + salutation);
      }
    }

    final String prefix = contact.getPrefix();
    if (prefix != null) {
      fullNameTextBuilder.append(prefix).append(' ');
      name.addPrefix(prefix);
    }

    fullNameTextBuilder.append(firstnameInfo).append(' ').append(lastname);
    name.setText(fullNameTextBuilder.toString());

    notifierFacility.addContact().setName(name);
  }

  private void addContacts(Organization notifierFacility, List<ContactPointInfo> contacts) {
    for (ContactPointInfo contact : contacts) {
      ContactPoint contactPoint = fhirObjectCreationService.createContactPoint(contact);
      notifierFacility.addTelecom(contactPoint);
    }
  }

  private void addAddress(Organization notifierFacility, NotifierFacility notifierFacilityContent) {
    final Address fhirAddress =
        fhirObjectCreationService.createAddress(notifierFacilityContent.getAddress());
    notifierFacility.addAddress(fhirAddress);
  }

  private void addOrganizationType(Organization notifierFacility, String organizationTypeCode) {
    notifierFacility
        .addType()
        .addCoding(
            ConfiguredCodeSystems.getInstance().getOrganizationTypeCoding(organizationTypeCode));
  }

  private void addBSNR(Organization notifierFacility, NotifierFacility notifierFacilityContent) {
    final String bsnrString = notifierFacilityContent.getFacilityInfo().getBsnr();
    if (StringUtils.isNotBlank(bsnrString)) {
      notifierFacility.addIdentifier().setSystem(NAMING_SYSTEM_BSNR).setValue(bsnrString);
    }
  }
}
