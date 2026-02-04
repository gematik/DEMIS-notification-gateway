package de.gematik.demis.notificationgateway.domain.pathogen.creator;

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

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.STRUCTURE_DEFINITION_ADDRESS_USE;

import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.AddressDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.HumanNameDataBuilder;
import de.gematik.demis.notificationgateway.common.dto.AddressType;
import de.gematik.demis.notificationgateway.common.dto.FacilityAddressInfo;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPersonAddressInfo;
import de.gematik.demis.notificationgateway.common.dto.PractitionerInfo;
import de.gematik.demis.notificationgateway.common.utils.ConfiguredCodeSystems;
import java.util.Optional;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Organization;

/**
 * Determines the salutation for the given {@link PractitionerInfo}.
 *
 * @param contact The practitioner information containing the salutation enum.
 * @return An {@link Optional} containing the corresponding {@link HumanNameDataBuilder.Salutation},
 *     or an empty {@link Optional} if no salutation is provided.
 */
public class AddressCreator {

  /**
   * Private constructor to prevent instantiation of this utility class.
   *
   * <p>This class is designed as a utility class and should not be instantiated.
   */
  private AddressCreator() {}

  /**
   * Creates a FHIR {@link Address} object using the provided {@link FacilityAddressInfo}.
   *
   * @param addressInfo The facility address information containing details such as house number,
   *     street, city, postal code, and country.
   * @return A {@link Address} object populated with the provided data, or null if the input is
   *     null.
   */
  public static Address createAddress(FacilityAddressInfo addressInfo) {
    return addressInfo == null ? null : buildAddress(addressInfo);
  }

  /**
   * Creates a FHIR {@link Address} object using the provided {@link NotifiedPersonAddressInfo}.
   * Adds additional information and address use extensions based on the address type.
   *
   * @param personAddress The notified person's address information containing details such as house
   *     number, street, city, postal code, and country.
   * @return A {@link Address} object populated with the provided data, or null if the input is
   *     null.
   */
  public static Address createAddress(NotifiedPersonAddressInfo personAddress) {
    if (personAddress == null) {
      return null;
    }

    AddressDataBuilder builder = buildAddressData(personAddress);
    if (personAddress.getAddressType() != AddressType.OTHER_FACILITY) {
      builder.setAdditionalInfo(personAddress.getAdditionalInfo());
    }

    Address address = builder.build();
    addAddressUseExtension(address, personAddress.getAddressType());
    return address;
  }

  /**
   * Creates a FHIR {@link Address} object without adding address use extensions.
   *
   * @param personAddress The notified person's address information.
   * @return A {@link Address} object without extensions, or null if the input is null.
   */
  public static Address createAddressWithoutAddressUse(NotifiedPersonAddressInfo personAddress) {
    Address address = createAddress(personAddress);
    if (address != null) {
      address.getExtension().clear();
    }
    return address;
  }

  /**
   * Creates a FHIR {@link Address} object with an organization reference extension.
   *
   * @param addressInfo The notified person's address information.
   * @param organization The organization to be referenced in the address.
   * @return A {@link Address} object populated with the provided data.
   */
  public static Address createAddress(
      NotifiedPersonAddressInfo addressInfo, Organization organization) {
    Address address =
        new AddressDataBuilder().withOrganizationReferenceExtension(organization).build();
    addAddressUseExtension(address, addressInfo.getAddressType());
    return address;
  }

  /**
   * Builds an {@link AddressDataBuilder} object using the provided {@link
   * NotifiedPersonAddressInfo}.
   *
   * @param addressInfo The notified person's address information.
   * @return An {@link AddressDataBuilder} object populated with the provided data.
   */
  private static AddressDataBuilder buildAddressData(NotifiedPersonAddressInfo addressInfo) {
    return new AddressDataBuilder()
        .setHouseNumber(addressInfo.getHouseNumber())
        .setStreet(addressInfo.getStreet())
        .setPostalCode(addressInfo.getZip())
        .setCity(addressInfo.getCity())
        .setCountry(addressInfo.getCountry());
  }

  /**
   * Builds a FHIR {@link Address} object using the provided {@link FacilityAddressInfo}.
   *
   * @param addressInfo The facility address information.
   * @return A {@link Address} object populated with the provided data.
   */
  private static Address buildAddress(FacilityAddressInfo addressInfo) {
    return new AddressDataBuilder()
        .setHouseNumber(addressInfo.getHouseNumber())
        .setStreet(addressInfo.getStreet())
        .setCity(addressInfo.getCity())
        .setPostalCode(addressInfo.getZip())
        .setCountry(addressInfo.getCountry())
        .build();
  }

  /**
   * Adds an address use extension to the provided {@link Address} object.
   *
   * @param address The address to which the extension will be added.
   * @param addressType The type of the address, used to determine the extension value.
   */
  private static void addAddressUseExtension(Address address, AddressType addressType) {
    Coding addressUse = getAddressUseCoding(addressType);
    if (addressUse != null) {
      address.addExtension().setUrl(STRUCTURE_DEFINITION_ADDRESS_USE).setValue(addressUse);
    }
  }

  /**
   * Retrieves the {@link Coding} object representing the address use based on the provided {@link
   * AddressType}.
   *
   * @param addressType The type of the address.
   * @return A {@link Coding} object representing the address use.
   * @throws IllegalArgumentException if the address type is null.
   */
  private static Coding getAddressUseCoding(AddressType addressType) {
    if (addressType == null) throw new IllegalArgumentException("AddressType cannot be null");
    String codingValue =
        switch (addressType) {
          case SUBMITTING_FACILITY, OTHER_FACILITY, PRIMARY_AS_CURRENT -> "current";
          default -> addressType.getValue();
        };
    return ConfiguredCodeSystems.getInstance().getAddressUseCoding(codingValue);
  }
}
