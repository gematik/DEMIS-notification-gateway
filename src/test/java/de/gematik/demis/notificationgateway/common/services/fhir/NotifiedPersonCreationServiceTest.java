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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.gematik.demis.notificationgateway.common.constants.FhirConstants;
import de.gematik.demis.notificationgateway.common.dto.Hospitalization;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPersonAddressInfo;
import de.gematik.demis.notificationgateway.common.dto.QuickTest;
import de.gematik.demis.notificationgateway.utils.FileUtils;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.HumanName.NameUse;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotifiedPersonCreationServiceTest {

  @InjectMocks private NotifiedPersonCreationService notifiedPersonCreationService;
  @Mock private FhirObjectCreationService fhirObjectCreationServiceMock;

  @Test
  void testCreateNotifiedPersonWithMinimumInput() throws JsonProcessingException {
    final QuickTest quickTest =
        FileUtils.createQuickTest("portal/laboratory/notification_content_min.json");

    final Address currentAddress = new Address();
    currentAddress.setId(UUID.randomUUID().toString());
    Mockito.when(
            fhirObjectCreationServiceMock.createAddress(
                quickTest.getNotifiedPerson().getCurrentAddress(), true))
        .thenReturn(currentAddress);

    final Patient notifiedPerson =
        notifiedPersonCreationService.createNotifiedPerson(quickTest.getNotifiedPerson());
    Assertions.assertNotNull(notifiedPerson);

    assertTrue(notifiedPerson.hasId());

    final Meta meta = notifiedPerson.getMeta();
    assertEquals(FhirConstants.PROFILE_NOTIFIED_PERSON, meta.getProfile().get(0).asStringValue());

    final List<HumanName> names = notifiedPerson.getName();
    assertEquals(1, names.size());
    final HumanName name = names.get(0);
    assertEquals(NameUse.OFFICIAL, name.getUse());
    assertEquals("Betroffen", name.getFamily());
    assertEquals("Bertha", name.getGiven().get(0).asStringValue());

    assertFalse(notifiedPerson.hasTelecom());

    assertEquals("FEMALE", notifiedPerson.getGender().toString());

    Assertions.assertNull(notifiedPerson.getBirthDate());

    final List<Address> addresses = notifiedPerson.getAddress();
    assertEquals(1, addresses.size());
    final Address address = addresses.get(0);
    assertEquals(currentAddress, address);

    Mockito.verify(fhirObjectCreationServiceMock)
        .createAddress(quickTest.getNotifiedPerson().getCurrentAddress(), true);
  }

  @Test
  void testCreateNotifiedPersonWithMaximumInput() throws JsonProcessingException {
    final QuickTest quickTest =
        FileUtils.createQuickTest("portal/laboratory/notification_content_max.json");

    Mockito.when(
            fhirObjectCreationServiceMock.createAddress(
                quickTest.getNotifiedPerson().getCurrentAddress(), true))
        .thenReturn(
            new Address()
                .setLine(List.of(new StringType("Betroffenenstraße 1")))
                .setPostalCode("21481")
                .setCity("Buchhorst")
                .setCountry("20422"));
    Mockito.when(
            fhirObjectCreationServiceMock.createAddress(
                quickTest.getNotifiedPerson().getOrdinaryAddress(), true))
        .thenReturn(
            new Address()
                .setLine(List.of(new StringType("Andere Straße 3")))
                .setPostalCode("11223")
                .setCity("Stadt")
                .setCountry("20422"));
    Mockito.when(
            fhirObjectCreationServiceMock.createAddress(
                quickTest.getNotifiedPerson().getPrimaryAddress(), true))
        .thenReturn(
            new Address()
                .setLine(List.of(new StringType("Berthastraße 123")))
                .setPostalCode("12345")
                .setCity("Betroffenenstadt")
                .setCountry("20422"));
    Mockito.when(
            fhirObjectCreationServiceMock.createContactPoint(
                quickTest.getNotifiedPerson().getContacts().get(0)))
        .thenReturn(new ContactPoint().setSystem(ContactPointSystem.PHONE).setValue("01234567"));
    Mockito.when(
            fhirObjectCreationServiceMock.createContactPoint(
                quickTest.getNotifiedPerson().getContacts().get(1)))
        .thenReturn(
            new ContactPoint().setSystem(ContactPointSystem.EMAIL).setValue("bertha@betroffen.de"));

    final Patient notifiedPerson =
        notifiedPersonCreationService.createNotifiedPerson(quickTest.getNotifiedPerson());
    Assertions.assertNotNull(notifiedPerson);

    assertTrue(notifiedPerson.hasId());

    final Meta meta = notifiedPerson.getMeta();
    assertEquals(FhirConstants.PROFILE_NOTIFIED_PERSON, meta.getProfile().get(0).asStringValue());

    final List<HumanName> names = notifiedPerson.getName();
    assertEquals(1, names.size());
    final HumanName name = names.get(0);
    assertEquals(NameUse.OFFICIAL, name.getUse());
    assertEquals("Betroffen", name.getFamily());
    final List<StringType> givens = name.getGiven();
    assertEquals(3, givens.size());
    assertEquals("Bertha-Luise", givens.get(0).asStringValue());
    assertEquals("Hanna", givens.get(1).asStringValue());
    assertEquals("Karin", givens.get(2).asStringValue());

    assertTrue(notifiedPerson.hasTelecom());
    final List<ContactPoint> telecom = notifiedPerson.getTelecom();
    assertEquals(2, telecom.size());
    assertEquals("phone", telecom.get(0).getSystem().toCode());
    assertEquals("01234567", telecom.get(0).getValue());
    assertEquals("email", telecom.get(1).getSystem().toCode());
    assertEquals("bertha@betroffen.de", telecom.get(1).getValue());

    assertEquals("FEMALE", notifiedPerson.getGender().toString());

    final LocalDate birthdate =
        LocalDate.ofInstant(notifiedPerson.getBirthDate().toInstant(), ZoneId.systemDefault());
    assertEquals(LocalDate.of(1999, 6, 9), birthdate);

    final List<Address> addresses = notifiedPerson.getAddress();
    assertEquals(3, addresses.size());
    final Address currentAddress = addresses.get(0);
    assertEquals("Betroffenenstraße 1", currentAddress.getLine().get(0).asStringValue());

    final Address primaryAddress = addresses.get(1);
    assertEquals("Berthastraße 123", primaryAddress.getLine().get(0).asStringValue());

    final Address ordinaryAddress = addresses.get(2);
    assertEquals("Andere Straße 3", ordinaryAddress.getLine().get(0).asStringValue());

    Mockito.verify(fhirObjectCreationServiceMock, Mockito.times(3))
        .createAddress(Mockito.any(NotifiedPersonAddressInfo.class), Mockito.eq(true));
  }

  @Test
  void testCreateNotifiedPersonHospitalizedInFacility() throws JsonProcessingException {
    final Hospitalization hospitalization =
        FileUtils.createHospitalization("portal/disease/notification_content_min.json");

    Mockito.when(
            fhirObjectCreationServiceMock.createAddress(
                hospitalization.getNotifiedPerson().getPrimaryAddress(), true))
        .thenReturn(
            new Address()
                .setLine(List.of(new StringType("Berthastraße 123")))
                .setPostalCode("12345")
                .setCity("Betroffenenstadt")
                .setCountry("20422"));

    final Patient notifiedPerson =
        notifiedPersonCreationService.createNotifiedPersonHospitalizedInFacility(
            hospitalization.getNotifiedPerson(), new Organization());
    Assertions.assertNotNull(notifiedPerson);

    assertTrue(notifiedPerson.hasId());

    final Meta meta = notifiedPerson.getMeta();
    assertEquals(FhirConstants.PROFILE_NOTIFIED_PERSON, meta.getProfile().get(0).asStringValue());

    final List<HumanName> names = notifiedPerson.getName();
    assertEquals(1, names.size());
    final HumanName name = names.get(0);
    assertEquals(NameUse.OFFICIAL, name.getUse());
    assertEquals("Betroffen", name.getFamily());
    final List<StringType> givens = name.getGiven();
    assertEquals(1, givens.size());
    assertEquals("Bertha", givens.get(0).asStringValue());

    assertEquals("FEMALE", notifiedPerson.getGender().toString());

    final List<Address> addresses = notifiedPerson.getAddress();
    assertEquals(2, addresses.size());

    final Address primaryAddress = addresses.get(0);
    assertEquals("Berthastraße 123", primaryAddress.getLine().get(0).asStringValue());

    final Address currentAddress = addresses.get(1);
    assertTrue(currentAddress.hasExtension());
    final List<Extension> currentFacilityAddressExtensions = currentAddress.getExtension();
    assertEquals(2, currentFacilityAddressExtensions.size());

    final Extension currentFacilityAddressUseExtension = currentFacilityAddressExtensions.get(0);
    assertEquals(
        FhirConstants.STRUCTURE_DEFINITION_ADDRESS_USE,
        currentFacilityAddressUseExtension.getUrl());
    final Coding currentAddressUseCode = (Coding) currentFacilityAddressUseExtension.getValue();
    assertEquals(FhirConstants.CODE_SYSTEM_ADDRESS_USE, currentAddressUseCode.getSystem());
    assertEquals("current", currentAddressUseCode.getCode());

    final Extension facilityAddressExtension = currentFacilityAddressExtensions.get(1);
    assertEquals(
        FhirConstants.STRUCTURE_DEFINITION_FACILITY_ADDRESS_NOTIFIED_PERSON,
        facilityAddressExtension.getUrl());
    assertTrue(((Reference) facilityAddressExtension.getValue()).hasReference());

    Mockito.verify(fhirObjectCreationServiceMock, Mockito.times(1))
        .createAddress(Mockito.any(NotifiedPersonAddressInfo.class), Mockito.eq(true));
  }
}
