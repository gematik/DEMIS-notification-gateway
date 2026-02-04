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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.demis.notificationgateway.common.dto.AddressType;
import de.gematik.demis.notificationgateway.common.dto.FacilityAddressInfo;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPersonAddressInfo;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Organization;
import org.junit.jupiter.api.Test;

class AddressCreatorTest {

  @Test
  void createsAddressFromFacilityAddressInfo() {
    FacilityAddressInfo facilityAddressInfo = mock(FacilityAddressInfo.class);
    when(facilityAddressInfo.getHouseNumber()).thenReturn("123");
    when(facilityAddressInfo.getStreet()).thenReturn("Musterstr.");
    when(facilityAddressInfo.getCity()).thenReturn("Berlin");
    when(facilityAddressInfo.getZip()).thenReturn("10115");
    when(facilityAddressInfo.getCountry()).thenReturn("DE");

    Address result = AddressCreator.createAddress(facilityAddressInfo);

    assertThat(result.getLine().getFirst().getValue()).isEqualTo("Musterstr. 123");
    assertThat(result.getCity()).isEqualTo("Berlin");
    assertThat(result.getPostalCode()).isEqualTo("10115");
    assertThat(result.getCountry()).isEqualTo("DE");
  }

  @Test
  void returnsNullForNullFacilityAddressInfo() {
    Address result = AddressCreator.createAddress((FacilityAddressInfo) null);

    assertThat(result).isNull();
  }

  @Test
  void createsAddressFromNotifiedPersonAddressInfo() {
    NotifiedPersonAddressInfo personAddressInfo = mock(NotifiedPersonAddressInfo.class);
    when(personAddressInfo.getHouseNumber()).thenReturn("123");
    when(personAddressInfo.getStreet()).thenReturn("Musterstr.");
    when(personAddressInfo.getCity()).thenReturn("Berlin");
    when(personAddressInfo.getZip()).thenReturn("10115");
    when(personAddressInfo.getCountry()).thenReturn("DE");
    when(personAddressInfo.getAddressType()).thenReturn(AddressType.PRIMARY_AS_CURRENT);

    Address result = AddressCreator.createAddress(personAddressInfo);

    assertThat(result.getLine().getFirst().getValue()).isEqualTo("Musterstr. 123");
    assertThat(result.getCity()).isEqualTo("Berlin");
    assertThat(result.getPostalCode()).isEqualTo("10115");
    assertThat(result.getCountry()).isEqualTo("DE");
    assertThat(result.getExtension()).isNotEmpty();
  }

  @Test
  void returnsNullForNullNotifiedPersonAddressInfo() {
    Address result = AddressCreator.createAddress((NotifiedPersonAddressInfo) null);

    assertThat(result).isNull();
  }

  @Test
  void shouldThrowExceptionWhenAddressTypeIsMissing() {
    NotifiedPersonAddressInfo personAddressInfo = mock(NotifiedPersonAddressInfo.class);
    when(personAddressInfo.getHouseNumber()).thenReturn("123");
    when(personAddressInfo.getStreet()).thenReturn("Musterstr.");
    when(personAddressInfo.getCity()).thenReturn("Berlin");
    when(personAddressInfo.getZip()).thenReturn("10115");
    when(personAddressInfo.getCountry()).thenReturn("DE");

    assertThatThrownBy(() -> AddressCreator.createAddressWithoutAddressUse(personAddressInfo))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("AddressType cannot be null");
  }

  @Test
  void createsAddressWithOrganizationReference() {
    NotifiedPersonAddressInfo personAddressInfo = mock(NotifiedPersonAddressInfo.class);
    Organization organization = mock(Organization.class);
    when(personAddressInfo.getAddressType()).thenReturn(AddressType.SUBMITTING_FACILITY);

    Address result = AddressCreator.createAddress(personAddressInfo, organization);

    assertThat(result.getExtension()).isNotEmpty();
  }
}
