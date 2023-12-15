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

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.STRUCTURE_DEFINITION_ADDRESS_USE;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.STRUCTURE_DEFINITION_ADXP_HOUSE_NUMBER;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.STRUCTURE_DEFINITION_ADXP_STREET_NAME;

import de.gematik.demis.notificationgateway.common.dto.AddressType;
import de.gematik.demis.notificationgateway.common.dto.ContactPointInfo;
import de.gematik.demis.notificationgateway.common.dto.ContactPointInfo.UsageEnum;
import de.gematik.demis.notificationgateway.common.dto.ContactsInfo;
import de.gematik.demis.notificationgateway.common.dto.FacilityAddressInfo;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPersonAddressInfo;
import de.gematik.demis.notificationgateway.common.utils.ConfiguredCodeSystems;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointUse;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Service
public class FhirObjectCreationService {

  public Parameters createParameters(Bundle bundle) {
    Parameters parameters = new Parameters();
    parameters.addParameter().setName("content").setResource(bundle);

    return parameters;
  }

  public Address createAddress(NotifiedPersonAddressInfo address, boolean withAddressUse) {
    final Address fhirAddress =
        createAddress(
            address.getStreet(),
            address.getHouseNumber(),
            address.getAdditionalInfo(),
            address.getZip(),
            address.getCity(),
            address.getCountry());

    final AddressType addressType = address.getAddressType();
    if (withAddressUse && addressType != null) {
      Coding addressUse =
          ConfiguredCodeSystems.getInstance().getAddressUseCoding(addressType.getValue());
      if (addressUse != null) {
        fhirAddress.addExtension().setUrl(STRUCTURE_DEFINITION_ADDRESS_USE).setValue(addressUse);
      }
    }

    return fhirAddress;
  }

  public Address createAddress(FacilityAddressInfo address) {
    return createAddress(
        address.getStreet(),
        address.getHouseNumber(),
        address.getAdditionalInfo(),
        address.getZip(),
        address.getCity(),
        address.getCountry());
  }

  public Address createAddress(
      String street,
      String houseNumber,
      String additionalInfo,
      String zip,
      String city,
      String country) {
    final Address fhirAddress = new Address();
    if (StringUtils.isNotBlank(zip)) {
      fhirAddress.setPostalCode(zip);
    }
    if (StringUtils.isNotBlank(country)) {
      fhirAddress.setCountry(country);
    }

    if (StringUtils.isNotBlank(street)) {
      StringBuilder lineBuilder = new StringBuilder();
      lineBuilder.append(street);

      final StringType lineElement = fhirAddress.addLineElement();
      lineElement
          .addExtension()
          .setUrl(STRUCTURE_DEFINITION_ADXP_STREET_NAME)
          .setValue(new StringType(street));

      if (StringUtils.isNotBlank(houseNumber)) {
        lineBuilder.append(" ").append(houseNumber);
        lineElement
            .addExtension()
            .setUrl(STRUCTURE_DEFINITION_ADXP_HOUSE_NUMBER)
            .setValue(new StringType(houseNumber));
      }

      if (StringUtils.isNotBlank(additionalInfo)) {
        lineBuilder.append(" ").append(additionalInfo);
      }

      lineElement.setValue(lineBuilder.toString());
    }

    if (StringUtils.isNotBlank(city)) {
      fhirAddress.setCity(city);
    }

    return fhirAddress;
  }

  public ContactPoint createContactPoint(ContactPointInfo contact) {
    ContactPoint contactPoint =
        new ContactPoint()
            .setSystem(ContactPointSystem.fromCode(contact.getContactType().getValue()))
            .setValue(contact.getValue());

    final UsageEnum usage = contact.getUsage();
    if (usage != null) {
      contactPoint.setUse(ContactPointUse.fromCode(usage.getValue()));
    }
    return contactPoint;
  }

  public List<ContactPoint> createContactPoints(@Nullable ContactsInfo contactInfo) {
    if (contactInfo == null) {
      return Collections.emptyList();
    }

    return Stream.of(contactInfo.getPhone(), contactInfo.getEmail())
        .filter(Objects::nonNull)
        .map(
            c ->
                new ContactPoint()
                    .setSystem(ContactPointSystem.fromCode(c.getContactType().getValue()))
                    .setValue(c.getValue()))
        .collect(Collectors.toList());
  }
}
