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

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.CODE_SYSTEM_ORGANIZATION_TYPE;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.gematik.demis.notificationgateway.common.constants.FhirConstants;
import de.gematik.demis.notificationgateway.common.dto.FacilityInfo;
import de.gematik.demis.notificationgateway.common.dto.NotifierFacility;
import de.gematik.demis.notificationgateway.common.dto.QuickTest;
import de.gematik.demis.notificationgateway.common.services.fhir.FhirObjectCreationService;
import de.gematik.demis.notificationgateway.utils.FileUtils;
import java.util.List;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Organization.OrganizationContactComponent;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationCreationServiceTest {

  @Mock private FhirObjectCreationService fhirObjectCreationServiceMock;
  @InjectMocks private OrganizationCreationService creationService;

  @Test
  void testNotifierFacilityContainsGeneralFixedData() throws JsonProcessingException {
    final QuickTest quickTest =
        FileUtils.createQuickTest("portal/laboratory/notification_content_min.json");
    final NotifierFacility notifierFacilityContent = quickTest.getNotifierFacility();

    Mockito.when(
            fhirObjectCreationServiceMock.createContactPoint(
                notifierFacilityContent.getContacts().getFirst()))
        .thenReturn(new ContactPoint().setSystem(ContactPointSystem.PHONE).setValue("01234567"));
    Mockito.when(fhirObjectCreationServiceMock.createAddress(notifierFacilityContent.getAddress()))
        .thenReturn(
            new Address()
                .setLine(List.of(new StringType("Straße 1")))
                .setPostalCode("21481")
                .setCity("Buchhorst")
                .setCountry("20422"));

    final Organization notifierFacility =
        creationService.createLabNotifierFacility(notifierFacilityContent);

    assertTrue(notifierFacility.hasId());

    assertTrue(notifierFacility.hasMeta());
    final Meta meta = notifierFacility.getMeta();
    assertTrue(meta.hasProfile());
    assertEquals(
        FhirConstants.PROFILE_NOTIFIER_FACILITY, meta.getProfile().getFirst().asStringValue());

    assertTrue(notifierFacility.hasName());

    assertOrgaType("othPrivatLab", "Sonstige private Untersuchungsstelle", notifierFacility);

    assertEquals(
        notifierFacilityContent.getFacilityInfo().getInstitutionName(), notifierFacility.getName());

    assertTrue(notifierFacility.hasTelecom());
    assertTrue(notifierFacility.hasAddress());

    Mockito.verify(fhirObjectCreationServiceMock)
        .createContactPoint(notifierFacilityContent.getContacts().getFirst());
    Mockito.verify(fhirObjectCreationServiceMock)
        .createAddress(notifierFacilityContent.getAddress());
  }

  @Test
  void testNotifierFacilityContainsDataFromPortalMinimumInput() throws JsonProcessingException {
    final QuickTest quickTest =
        FileUtils.createQuickTest("portal/laboratory/notification_content_min.json");
    final NotifierFacility notifierFacilityContent = quickTest.getNotifierFacility();

    Mockito.when(
            fhirObjectCreationServiceMock.createContactPoint(
                notifierFacilityContent.getContacts().getFirst()))
        .thenReturn(new ContactPoint().setSystem(ContactPointSystem.PHONE).setValue("01234567"));
    Mockito.when(fhirObjectCreationServiceMock.createAddress(notifierFacilityContent.getAddress()))
        .thenReturn(
            new Address()
                .setLine(List.of(new StringType("Straße 1")))
                .setPostalCode("21481")
                .setCity("Buchhorst")
                .setCountry("20422"));

    final Organization notifierFacility =
        creationService.createLabNotifierFacility(notifierFacilityContent);

    assertEquals("TEST Organisation", notifierFacility.getName());
    assertFalse(notifierFacility.hasIdentifier());

    final List<ContactPoint> telecomList = notifierFacility.getTelecom();
    assertEquals(1, telecomList.size());
    final ContactPoint contactPoint = telecomList.getFirst();
    assertEquals("phone", contactPoint.getSystem().toCode());
    assertEquals("01234567", contactPoint.getValue());

    final List<Address> addresses = notifierFacility.getAddress();
    assertEquals(1, addresses.size());
    final Address address = addresses.getFirst();
    assertEquals("Straße 1", address.getLine().getFirst().asStringValue());
    assertEquals("21481", address.getPostalCode());
    assertEquals("Buchhorst", address.getCity());
    assertEquals("20422", address.getCountry());

    final List<OrganizationContactComponent> contactList = notifierFacility.getContact();
    assertEquals(1, contactList.size());
    final HumanName contactName = contactList.getFirst().getName();
    assertEquals("Anna Ansprechpartner", contactName.getText());
    assertEquals("Ansprechpartner", contactName.getFamily());
    assertEquals("Anna", contactName.getGiven().getFirst().asStringValue());
    assertFalse(contactName.hasPrefix());

    Mockito.verify(fhirObjectCreationServiceMock)
        .createContactPoint(notifierFacilityContent.getContacts().getFirst());
    Mockito.verify(fhirObjectCreationServiceMock)
        .createAddress(notifierFacilityContent.getAddress());
  }

  @Test
  void testNotifierFacilityContainsBsnr() throws JsonProcessingException {
    final QuickTest quickTest =
        FileUtils.createQuickTest("portal/laboratory/notification_content_max.json");

    final NotifierFacility notifierFacilityContent = quickTest.getNotifierFacility();
    final Organization notifierFacility =
        creationService.createLabNotifierFacility(notifierFacilityContent);

    assertTrue(notifierFacility.hasIdentifier());
    final List<Identifier> identifiers = notifierFacility.getIdentifier();
    assertEquals(1, identifiers.size());
    final Identifier identifier = identifiers.getFirst();
    assertEquals(FhirConstants.NAMING_SYSTEM_BSNR, identifier.getSystem());
    assertEquals("123456789", identifier.getValue());
  }

  @Test
  void testNotifierFacilityContainsMultipleGivens() throws JsonProcessingException {
    final QuickTest quickTest =
        FileUtils.createQuickTest("portal/laboratory/notification_content_max.json");

    final NotifierFacility notifierFacilityContent = quickTest.getNotifierFacility();
    final Organization notifierFacility =
        creationService.createLabNotifierFacility(notifierFacilityContent);

    final List<OrganizationContactComponent> contactList = notifierFacility.getContact();
    assertEquals(1, contactList.size());
    final HumanName contactName = contactList.getFirst().getName();
    assertEquals("Frau Dr. Anna Beate Carolin Ansprechpartner", contactName.getText());
    assertEquals("Ansprechpartner", contactName.getFamily());
    final List<StringType> given = contactName.getGiven();
    assertEquals(3, given.size());
    assertEquals("Anna", given.get(0).asStringValue());
    assertEquals("Beate", given.get(1).asStringValue());
    assertEquals("Carolin", given.get(2).asStringValue());
    assertEquals("Dr.", contactName.getPrefix().getFirst().asStringValue());
  }

  @Test
  void testNotifierFacilityContainsMultipleContactPoints() throws JsonProcessingException {
    final QuickTest quickTest =
        FileUtils.createQuickTest("portal/laboratory/notification_content_max.json");
    final NotifierFacility notifierFacilityContent = quickTest.getNotifierFacility();

    Mockito.when(
            fhirObjectCreationServiceMock.createContactPoint(
                notifierFacilityContent.getContacts().get(0)))
        .thenReturn(new ContactPoint().setSystem(ContactPointSystem.PHONE).setValue("01234567"));
    Mockito.when(
            fhirObjectCreationServiceMock.createContactPoint(
                notifierFacilityContent.getContacts().get(1)))
        .thenReturn(
            new ContactPoint()
                .setSystem(ContactPointSystem.EMAIL)
                .setValue("anna@ansprechpartner.de"));

    final Organization notifierFacility =
        creationService.createLabNotifierFacility(notifierFacilityContent);

    final List<ContactPoint> telecomList = notifierFacility.getTelecom();
    assertEquals(2, telecomList.size());
    final ContactPoint phone = telecomList.getFirst();
    assertEquals("phone", phone.getSystem().toCode());
    assertEquals("01234567", phone.getValue());
    final ContactPoint email = telecomList.get(1);
    assertEquals("email", email.getSystem().toCode());
    assertEquals("anna@ansprechpartner.de", email.getValue());

    Mockito.verify(fhirObjectCreationServiceMock, Mockito.times(2))
        .createContactPoint(Mockito.isNotNull());
  }

  @Test
  void createNotifierFacilitySuccessfully() throws JsonProcessingException {
    final QuickTest quickTest =
        FileUtils.createQuickTest("portal/laboratory/notification_content_min.json");

    final NotifierFacility notifierFacility = quickTest.getNotifierFacility();
    final FacilityInfo facilityInfo = notifierFacility.getFacilityInfo();
    final String orgaTypeCode = "mySpecialCodeValue";
    facilityInfo.setOrganizationType(orgaTypeCode);

    final Organization result = creationService.createNotifierFacility(notifierFacility);

    assertOrgaType(orgaTypeCode, null, result);
  }

  @Test
  void testNotifierFacilityIsHospital() throws JsonProcessingException {
    final QuickTest quickTest =
        FileUtils.createQuickTest("portal/laboratory/notification_content_min.json");

    final NotifierFacility notifierFacilityContent = quickTest.getNotifierFacility();
    final Organization notifierFacility =
        creationService.createHospitalNotifierFacility(notifierFacilityContent);

    assertOrgaType("hospital", "Krankenhaus", notifierFacility);
  }

  private void assertOrgaType(
      final String expectedCode, final String expectedDisplay, final Organization actual) {
    final List<CodeableConcept> types = actual.getType();
    assertEquals(1, types.size());
    final List<Coding> typeCodings = types.getFirst().getCoding();
    assertEquals(1, typeCodings.size());
    final Coding typeCoding = typeCodings.getFirst();
    assertEquals(CODE_SYSTEM_ORGANIZATION_TYPE, typeCoding.getSystem());
    assertEquals(expectedCode, typeCoding.getCode());
    assertEquals(expectedDisplay, typeCoding.getDisplay());
  }
}
