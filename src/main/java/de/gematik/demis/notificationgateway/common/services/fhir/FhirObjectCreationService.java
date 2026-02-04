package de.gematik.demis.notificationgateway.common.services.fhir;

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
import de.gematik.demis.notificationgateway.common.dto.AddressType;
import de.gematik.demis.notificationgateway.common.dto.ContactPointInfo;
import de.gematik.demis.notificationgateway.common.dto.ContactPointInfo.UsageEnum;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPersonAddressInfo;
import de.gematik.demis.notificationgateway.common.utils.ConfiguredCodeSystems;
import lombok.AllArgsConstructor;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointUse;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Parameters;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class FhirObjectCreationService {

  public Parameters createParameters(Bundle bundle) {
    Parameters parameters = new Parameters();
    parameters.addParameter().setName("content").setResource(bundle);

    return parameters;
  }

  public Address createAddress(NotifiedPersonAddressInfo address, boolean withAddressUse) {
    final Address fhirAddress;
    fhirAddress =
        new AddressDataBuilder()
            .setStreet(address.getStreet())
            .setHouseNumber(address.getHouseNumber())
            .setAdditionalInfo(address.getAdditionalInfo())
            .setCountry(address.getCountry())
            .setPostalCode(address.getZip())
            .setCity(address.getCity())
            .build();

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

  public Address createAddress(NotifiedPersonAddressInfo personAddress) {
    final Address fhirAddress = createAddress(personAddress, false);
    final Coding addressUse = getAddressUseCoding(personAddress.getAddressType());
    if (addressUse != null) {
      fhirAddress.addExtension().setUrl(STRUCTURE_DEFINITION_ADDRESS_USE).setValue(addressUse);
    }
    return fhirAddress;
  }

  /**
   * Creates an Address resource based on the provided NotifiedPersonAddressInfo and Organization.
   * If the NotifiedPersonAddressInfo is null, an empty Optional is returned. The created Address
   * will include an extension for the address use coding based on the address type.
   *
   * @param addressInfo the notified person address information
   * @param organization the organization associated with the address
   * @return an Optional containing the created Address resource, or an empty Optional if
   *     addressInfo is null
   */
  public Address createAddressWithReferenceToOrganization(
      final NotifiedPersonAddressInfo addressInfo, final Organization organization) {
    final Address fhirAddress =
        new AddressDataBuilder().withOrganizationReferenceExtension(organization).build();
    final Coding addressUse = getAddressUseCoding(addressInfo.getAddressType());
    if (addressUse != null) {
      fhirAddress.addExtension().setUrl(STRUCTURE_DEFINITION_ADDRESS_USE).setValue(addressUse);
    }
    return fhirAddress;
  }

  /**
   * This method translates the portal-disease frontend types to the internal code system.
   *
   * @param addressType the address type from the portal-disease frontend
   * @return a Coding object representing the FHIR address type
   */
  private Coding getAddressUseCoding(final AddressType addressType) {
    final String referencedCodingValue =
        switch (addressType) {
          case SUBMITTING_FACILITY, OTHER_FACILITY, PRIMARY_AS_CURRENT -> "current";
          default -> addressType.getValue();
        };

    return ConfiguredCodeSystems.getInstance().getAddressUseCoding(referencedCodingValue);
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
}
