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
 * #L%
 */

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.gematik.demis.notificationgateway.common.constants.FhirConstants;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPersonAddressInfo;
import de.gematik.demis.notificationgateway.common.dto.QuickTest;
import de.gematik.demis.notificationgateway.common.services.fhir.FhirObjectCreationService;
import de.gematik.demis.notificationgateway.utils.FileUtils;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.HumanName.NameUse;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
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
        notifiedPersonCreationService.createPatient(quickTest.getNotifiedPerson());
    Assertions.assertNotNull(notifiedPerson);

    assertTrue(notifiedPerson.hasId());

    final Meta meta = notifiedPerson.getMeta();
    assertEquals(
        FhirConstants.PROFILE_NOTIFIED_PERSON, meta.getProfile().getFirst().asStringValue());

    final List<HumanName> names = notifiedPerson.getName();
    assertEquals(1, names.size());
    final HumanName name = names.getFirst();
    assertEquals(NameUse.OFFICIAL, name.getUse());
    assertEquals("Betroffen", name.getFamily());
    assertEquals("Bertha", name.getGiven().getFirst().asStringValue());

    assertFalse(notifiedPerson.hasTelecom());

    assertEquals("FEMALE", notifiedPerson.getGender().toString());

    Assertions.assertNull(notifiedPerson.getBirthDate());

    final List<Address> addresses = notifiedPerson.getAddress();
    assertEquals(1, addresses.size());
    final Address address = addresses.getFirst();
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
                quickTest.getNotifiedPerson().getContacts().getFirst()))
        .thenReturn(new ContactPoint().setSystem(ContactPointSystem.PHONE).setValue("01234567"));
    Mockito.when(
            fhirObjectCreationServiceMock.createContactPoint(
                quickTest.getNotifiedPerson().getContacts().get(1)))
        .thenReturn(
            new ContactPoint().setSystem(ContactPointSystem.EMAIL).setValue("bertha@betroffen.de"));

    final Patient notifiedPerson =
        notifiedPersonCreationService.createPatient(quickTest.getNotifiedPerson());
    Assertions.assertNotNull(notifiedPerson);

    assertTrue(notifiedPerson.hasId());

    final Meta meta = notifiedPerson.getMeta();
    assertEquals(
        FhirConstants.PROFILE_NOTIFIED_PERSON, meta.getProfile().getFirst().asStringValue());

    final List<HumanName> names = notifiedPerson.getName();
    assertEquals(1, names.size());
    final HumanName name = names.getFirst();
    assertEquals(NameUse.OFFICIAL, name.getUse());
    assertEquals("Betroffen", name.getFamily());
    final List<StringType> givens = name.getGiven();
    assertEquals(3, givens.size());
    assertEquals("Bertha-Luise", givens.getFirst().asStringValue());
    assertEquals("Hanna", givens.get(1).asStringValue());
    assertEquals("Karin", givens.get(2).asStringValue());

    assertTrue(notifiedPerson.hasTelecom());
    final List<ContactPoint> telecom = notifiedPerson.getTelecom();
    assertEquals(2, telecom.size());
    assertEquals("phone", telecom.getFirst().getSystem().toCode());
    assertEquals("01234567", telecom.getFirst().getValue());
    assertEquals("email", telecom.get(1).getSystem().toCode());
    assertEquals("bertha@betroffen.de", telecom.get(1).getValue());

    assertEquals("FEMALE", notifiedPerson.getGender().toString());

    final LocalDate birthdate =
        LocalDate.ofInstant(notifiedPerson.getBirthDate().toInstant(), ZoneId.systemDefault());
    assertEquals(LocalDate.of(1999, 6, 9), birthdate);

    final List<Address> addresses = notifiedPerson.getAddress();
    assertEquals(3, addresses.size());
    final Address currentAddress = addresses.getFirst();
    assertEquals("Betroffenenstraße 1", currentAddress.getLine().getFirst().asStringValue());

    final Address primaryAddress = addresses.get(1);
    assertEquals("Berthastraße 123", primaryAddress.getLine().getFirst().asStringValue());

    final Address ordinaryAddress = addresses.get(2);
    assertEquals("Andere Straße 3", ordinaryAddress.getLine().getFirst().asStringValue());

    Mockito.verify(fhirObjectCreationServiceMock, Mockito.times(3))
        .createAddress(Mockito.any(NotifiedPersonAddressInfo.class), Mockito.eq(true));
  }
}
