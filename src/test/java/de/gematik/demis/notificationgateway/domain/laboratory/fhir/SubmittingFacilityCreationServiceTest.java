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

package de.gematik.demis.notificationgateway.domain.laboratory.fhir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.gematik.demis.notificationgateway.common.constants.FhirConstants;
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
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubmittingFacilityCreationServiceTest {

  @Mock private FhirObjectCreationService fhirObjectCreationServiceMock;
  @InjectMocks private SubmittingFacilityCreationService creationService;

  @Test
  void testSubmittingFacilityContainsGeneralFixedData() throws JsonProcessingException {
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

    final Organization submittingFacility =
        creationService.createSubmittingFacility(notifierFacilityContent);

    assertTrue(submittingFacility.hasId());

    assertTrue(submittingFacility.hasMeta());
    final Meta meta = submittingFacility.getMeta();
    assertTrue(meta.hasProfile());
    assertEquals(
        FhirConstants.PROFILE_SUBMITTING_FACILITY, meta.getProfile().get(0).asStringValue());

    assertTrue(submittingFacility.hasName());

    assertTrue(submittingFacility.hasType());
    final List<CodeableConcept> types = submittingFacility.getType();
    assertEquals(1, types.size());
    final List<Coding> typeCodings = types.get(0).getCoding();
    assertEquals(1, typeCodings.size());
    final Coding typeCoding = typeCodings.get(0);
    assertEquals(FhirConstants.CODE_SYSTEM_ORGANIZATION_TYPE, typeCoding.getSystem());
    assertEquals("physicianOffice", typeCoding.getCode());
    assertEquals("Arztpraxis", typeCoding.getDisplay());

    assertEquals(
        notifierFacilityContent.getFacilityInfo().getInstitutionName(),
        submittingFacility.getName());

    assertTrue(submittingFacility.hasTelecom());
    assertTrue(submittingFacility.hasAddress());
    assertFalse(submittingFacility.hasContact());

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

    final Organization submittingFacility =
        creationService.createSubmittingFacility(notifierFacilityContent);

    assertEquals("TEST Organisation", submittingFacility.getName());
    assertFalse(submittingFacility.hasIdentifier());

    final List<ContactPoint> telecomList = submittingFacility.getTelecom();
    assertEquals(1, telecomList.size());
    final ContactPoint contactPoint = telecomList.get(0);
    assertEquals("phone", contactPoint.getSystem().toCode());
    assertEquals("01234567", contactPoint.getValue());

    final List<Address> addresses = submittingFacility.getAddress();
    assertEquals(1, addresses.size());
    final Address address = addresses.get(0);
    assertEquals("Straße 1", address.getLine().get(0).asStringValue());
    assertEquals("21481", address.getPostalCode());
    assertEquals("Buchhorst", address.getCity());
    assertEquals("20422", address.getCountry());

    Mockito.verify(fhirObjectCreationServiceMock)
        .createContactPoint(notifierFacilityContent.getContacts().get(0));
    Mockito.verify(fhirObjectCreationServiceMock)
        .createAddress(notifierFacilityContent.getAddress());
  }
}
