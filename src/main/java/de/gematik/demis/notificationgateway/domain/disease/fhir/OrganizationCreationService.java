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

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.*;

import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.AddressDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.OrganizationBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.utils.Utils;
import de.gematik.demis.notificationgateway.common.constants.FhirConstants;
import de.gematik.demis.notificationgateway.common.dto.ContactPointInfo;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPersonAddressInfo;
import de.gematik.demis.notificationgateway.common.dto.NotifierFacility;
import de.gematik.demis.notificationgateway.common.dto.PractitionerInfo;
import de.gematik.demis.notificationgateway.common.dto.PractitionerInfo.SalutationEnum;
import de.gematik.demis.notificationgateway.common.services.fhir.FhirObjectCreationService;
import de.gematik.demis.notificationgateway.common.utils.ConfiguredCodeSystems;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.stereotype.Service;

/** Creates FHIR Organization resources that are not within the questionnaires. */
@Service
@AllArgsConstructor
class OrganizationCreationService {

  private final FhirObjectCreationService fhirObjectCreationService;

  public Organization createNotifierFacility(final NotifierFacility notifierFacilityContent) {
    final String organizationType = notifierFacilityContent.getFacilityInfo().getOrganizationType();
    return createFacility(notifierFacilityContent, organizationType);
  }

  public Organization createHospitalNotifierFacility(NotifierFacility notifierFacilityContent) {
    return createFacility(notifierFacilityContent, "hospital");
  }

  public Organization createNotifiedPersonFacility(NotifiedPersonAddressInfo address) {
    final String facilityName = address.getAdditionalInfo();
    // In this context additionalInfo is only used to store the name of the other facility
    // but in further steps it shouldn't be processed as part of the address, that's why it's
    // removed here
    final var addressWithoutAdditionalInfo = withoutAdditionalInfo(address);
    final Address fhirAddress =
        fhirObjectCreationService.createAddress(addressWithoutAdditionalInfo);
    fhirAddress.getExtension().clear();
    return new OrganizationBuilder()
        .setDefaults()
        .setMetaProfileUrl(PROFILE_NOTIFIED_PERSON_FACILITY)
        .setFacilityName(facilityName)
        .setAddress(fhirAddress)
        .build();
  }

  private NotifiedPersonAddressInfo withoutAdditionalInfo(
      NotifiedPersonAddressInfo notifiedPersonAddressInfo) {
    var clone = new NotifiedPersonAddressInfo();
    clone.setStreet(notifiedPersonAddressInfo.getStreet());
    clone.setHouseNumber(notifiedPersonAddressInfo.getHouseNumber());
    clone.setZip(notifiedPersonAddressInfo.getZip());
    clone.setCity(notifiedPersonAddressInfo.getCity());
    clone.setCountry(notifiedPersonAddressInfo.getCountry());
    clone.setAddressType(notifiedPersonAddressInfo.getAddressType());
    return clone;
  }

  /**
   * Disease notification currently requires creating a clone of the notifier facility as a notified
   * person facility. This is different to pathogen notifications.
   *
   * @param notifier the notifier
   * @return notified person facility
   */
  Organization createNotifiedPersonFacility(PractitionerRole notifier) {
    final Organization source = (Organization) notifier.getOrganization().getResource();
    final OrganizationBuilder clone = new OrganizationBuilder();
    clone.setDefaults();
    clone.setMetaProfileUrl(PROFILE_NOTIFIED_PERSON_FACILITY);
    clone.setFacilityName(source.getName());
    clone.setAddress(source.getAddressFirstRep());
    source.getContact().forEach(clone::addContact);
    source.getTelecom().forEach(clone::addTelecom);
    return clone.build();
  }

  private Organization createFacility(
      NotifierFacility notifierFacilityContent, String organizationTypeCode) {
    final Organization notifierFacility = new Organization();
    notifierFacility.setId(Utils.generateUuidString());
    notifierFacility.setMeta(new Meta().addProfile(FhirConstants.PROFILE_NOTIFIER_FACILITY));

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
          throw new IllegalArgumentException("unknown salutation type: " + salutation);
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
    contacts.stream()
        .map(this.fhirObjectCreationService::createContactPoint)
        .forEach(notifierFacility::addTelecom);
  }

  private void addAddress(Organization notifierFacility, NotifierFacility notifierFacilityContent) {
    var address = notifierFacilityContent.getAddress();
    final Address fhirAddress;
    fhirAddress =
        new AddressDataBuilder()
            .setStreet(address.getStreet())
            .setHouseNumber(address.getHouseNumber())
            .setCity(address.getCity())
            .setPostalCode(address.getZip())
            .setCountry(address.getCountry())
            .build();

    notifierFacility.addAddress(fhirAddress);
  }

  private void addOrganizationType(Organization notifierFacility, String organizationTypeCode) {
    Coding organizationTypeCoding =
        ConfiguredCodeSystems.getInstance().getOrganizationTypeCoding(organizationTypeCode);
    if (organizationTypeCoding == null) {
      organizationTypeCoding =
          new Coding()
              .setSystem(FhirConstants.CODE_SYSTEM_ORGANIZATION_TYPE)
              .setCode(organizationTypeCode);
    }

    notifierFacility.addType().addCoding(organizationTypeCoding);
  }

  private void addBSNR(Organization notifierFacility, NotifierFacility notifierFacilityContent) {
    final String bsnrString = notifierFacilityContent.getFacilityInfo().getBsnr();
    if (StringUtils.isNotBlank(bsnrString)) {
      notifierFacility.addIdentifier().setSystem(NAMING_SYSTEM_BSNR).setValue(bsnrString);
    }
  }
}
