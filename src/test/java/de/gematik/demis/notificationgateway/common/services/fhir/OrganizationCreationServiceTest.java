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
import de.gematik.demis.notificationgateway.common.dto.InfectionProtectionFacilityInfo;
import de.gematik.demis.notificationgateway.common.dto.LabInfo;
import de.gematik.demis.notificationgateway.common.dto.NotifierFacility;
import de.gematik.demis.notificationgateway.common.dto.QuickTest;
import de.gematik.demis.notificationgateway.utils.FileUtils;
import java.util.List;
import java.util.Optional;
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
                notifierFacilityContent.getContacts().get(0)))
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
    assertEquals(FhirConstants.PROFILE_NOTIFIER_FACILITY, meta.getProfile().get(0).asStringValue());

    assertTrue(notifierFacility.hasName());

    assertTrue(notifierFacility.hasType());
    final List<CodeableConcept> types = notifierFacility.getType();
    assertEquals(1, types.size());
    final List<Coding> typeCodings = types.get(0).getCoding();
    assertEquals(1, typeCodings.size());
    final Coding typeCoding = typeCodings.get(0);
    assertEquals(FhirConstants.CODE_SYSTEM_ORGANIZATION_TYPE, typeCoding.getSystem());
    assertEquals("othPrivatLab", typeCoding.getCode());
    assertEquals("Sonstige private Untersuchungsstelle", typeCoding.getDisplay());

    assertEquals(
        notifierFacilityContent.getFacilityInfo().getInstitutionName(), notifierFacility.getName());

    assertTrue(notifierFacility.hasTelecom());
    assertTrue(notifierFacility.hasAddress());

    Mockito.verify(fhirObjectCreationServiceMock)
        .createContactPoint(notifierFacilityContent.getContacts().get(0));
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
                notifierFacilityContent.getContacts().get(0)))
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
    final ContactPoint contactPoint = telecomList.get(0);
    assertEquals("phone", contactPoint.getSystem().toCode());
    assertEquals("01234567", contactPoint.getValue());

    final List<Address> addresses = notifierFacility.getAddress();
    assertEquals(1, addresses.size());
    final Address address = addresses.get(0);
    assertEquals("Straße 1", address.getLine().get(0).asStringValue());
    assertEquals("21481", address.getPostalCode());
    assertEquals("Buchhorst", address.getCity());
    assertEquals("20422", address.getCountry());

    final List<OrganizationContactComponent> contactList = notifierFacility.getContact();
    assertEquals(1, contactList.size());
    final HumanName contactName = contactList.get(0).getName();
    assertEquals("Anna Ansprechpartner", contactName.getText());
    assertEquals("Ansprechpartner", contactName.getFamily());
    assertEquals("Anna", contactName.getGiven().get(0).asStringValue());
    assertFalse(contactName.hasPrefix());

    Mockito.verify(fhirObjectCreationServiceMock)
        .createContactPoint(notifierFacilityContent.getContacts().get(0));
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
    final Identifier identifier = identifiers.get(0);
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
    final HumanName contactName = contactList.get(0).getName();
    assertEquals("Frau Dr. Anna Beate Carolin Ansprechpartner", contactName.getText());
    assertEquals("Ansprechpartner", contactName.getFamily());
    final List<StringType> given = contactName.getGiven();
    assertEquals(3, given.size());
    assertEquals("Anna", given.get(0).asStringValue());
    assertEquals("Beate", given.get(1).asStringValue());
    assertEquals("Carolin", given.get(2).asStringValue());
    assertEquals("Dr.", contactName.getPrefix().get(0).asStringValue());
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
    final ContactPoint phone = telecomList.get(0);
    assertEquals("phone", phone.getSystem().toCode());
    assertEquals("01234567", phone.getValue());
    final ContactPoint email = telecomList.get(1);
    assertEquals("email", email.getSystem().toCode());
    assertEquals("anna@ansprechpartner.de", email.getValue());

    Mockito.verify(fhirObjectCreationServiceMock, Mockito.times(2))
        .createContactPoint(Mockito.isNotNull());
  }

  @Test
  void testNotifierFacilityIsHospital() throws JsonProcessingException {
    final QuickTest quickTest =
        FileUtils.createQuickTest("portal/laboratory/notification_content_min.json");

    final NotifierFacility notifierFacilityContent = quickTest.getNotifierFacility();
    final Organization notifierFacility =
        creationService.createHospitalNotifierFacility(notifierFacilityContent);

    assertTrue(notifierFacility.hasType());
    final List<CodeableConcept> types = notifierFacility.getType();
    assertEquals(1, types.size());
    final List<Coding> typeCodings = types.get(0).getCoding();
    assertEquals(1, typeCodings.size());
    final Coding typeCoding = typeCodings.get(0);
    assertEquals(FhirConstants.CODE_SYSTEM_ORGANIZATION_TYPE, typeCoding.getSystem());
    assertEquals("hospital", typeCoding.getCode());
    assertEquals("Krankenhaus", typeCoding.getDisplay());
  }

  @Test
  void testCreateLabWithMinimumInput() throws JsonProcessingException {
    Hospitalization hospitalization =
        FileUtils.createHospitalization(
            "portal/disease/organization/notification_content_lab_min.json");
    final LabInfo labInfo =
        hospitalization.getDisease().getDiseaseInfoCommon().getLabQuestion().getLabInfo();

    Mockito.when(fhirObjectCreationServiceMock.createAddress(labInfo.getAddress(), false))
        .thenReturn(new Address().setPostalCode("21481").setCity("Buchhorst").setCountry("20422"));

    final Optional<Organization> labOptional = creationService.createLab(labInfo);

    assertTrue(labOptional.isPresent());
    Organization lab = labOptional.get();

    assertTrue(lab.hasId());

    assertTrue(lab.hasMeta());
    final Meta meta = lab.getMeta();
    assertTrue(meta.hasProfile());
    assertEquals(FhirConstants.PROFILE_ORGANIZATION, meta.getProfile().get(0).asStringValue());

    assertEquals("Labor", lab.getName());

    final List<Address> addresses = lab.getAddress();
    assertEquals(1, addresses.size());
    final Address address = addresses.get(0);

    assertFalse(address.hasLine());
    assertEquals("Buchhorst", address.getCity());
    assertEquals("20422", address.getCountry());
    assertEquals("21481", address.getPostalCode());

    assertFalse(lab.hasType());

    Mockito.verify(fhirObjectCreationServiceMock).createAddress(labInfo.getAddress(), false);
  }

  @Test
  void testCreateInfectProtectFacilityWithMinimumInput() throws JsonProcessingException {
    Hospitalization hospitalization =
        FileUtils.createHospitalization(
            "portal/disease/organization/notification_content_infectprotectfacility_min.json");
    final InfectionProtectionFacilityInfo infectProtectFacilityInfo =
        hospitalization
            .getDisease()
            .getDiseaseInfoCommon()
            .getInfectionProtectionFacilityQuestion()
            .getInfectionProtectionFacilityInfo();

    Mockito.when(
            fhirObjectCreationServiceMock.createAddress(
                infectProtectFacilityInfo.getAddress(), false))
        .thenReturn(new Address().setPostalCode("21481").setCity("Buchhorst").setCountry("20422"));

    final Optional<Organization> infectProtectFacilityOptional =
        creationService.createInfectProtectFacility(infectProtectFacilityInfo);

    assertTrue(infectProtectFacilityOptional.isPresent());
    Organization infectProtectFacility = infectProtectFacilityOptional.get();

    assertTrue(infectProtectFacility.hasId());

    assertTrue(infectProtectFacility.hasMeta());
    final Meta meta = infectProtectFacility.getMeta();
    assertTrue(meta.hasProfile());
    assertEquals(FhirConstants.PROFILE_ORGANIZATION, meta.getProfile().get(0).asStringValue());

    assertFalse(infectProtectFacility.hasName());

    assertTrue(infectProtectFacility.hasAddress());
    final List<Address> addresses = infectProtectFacility.getAddress();
    assertEquals(1, addresses.size());
    final Address address = addresses.get(0);
    assertFalse(address.hasLine());
    assertEquals("Buchhorst", address.getCity());
    assertEquals("20422", address.getCountry());
    assertEquals("21481", address.getPostalCode());

    assertFalse(infectProtectFacility.hasType());
    assertFalse(infectProtectFacility.hasTelecom());

    Mockito.verify(fhirObjectCreationServiceMock)
        .createAddress(infectProtectFacilityInfo.getAddress(), false);
  }

  @Test
  void testReturnsLabNullWhenInfoIsNull() {
    final Optional<Organization> labOptional = creationService.createLab(null);

    assertFalse(labOptional.isPresent());
  }

  @Test
  void testReturnsInfectProtectFacilityNullWhenInfoIsNull() {
    final Optional<Organization> infectProtectFacilityOptional =
        creationService.createInfectProtectFacility(null);

    assertFalse(infectProtectFacilityOptional.isPresent());
  }
}
