package de.gematik.demis.notificationgateway.domain.pathogen.creator;

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
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import de.gematik.demis.notificationgateway.common.dto.*;
import java.util.List;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;

class PractitionerOrganizationCreatorTest {

  @Test
  void createNotifierPractitionerRoleShouldReturnValidPractitionerRole() {
    NotifierFacility notifierFacility = new NotifierFacility();
    FacilityAddressInfo addressInfo = new FacilityAddressInfo();
    notifierFacility.setAddress(addressInfo);
    FacilityInfo facilityInfo = new FacilityInfo();
    facilityInfo.setInstitutionName("Test Institution");
    facilityInfo.setExistsBsnr(true);
    facilityInfo.setBsnr("123456789");
    notifierFacility.setFacilityInfo(facilityInfo);
    PractitionerInfo practitionerInfo = new PractitionerInfo();
    notifierFacility.setContact(practitionerInfo);
    ContactPointInfo contactPointInfo = new ContactPointInfo();
    contactPointInfo.setContactType(ContactPointInfo.ContactTypeEnum.PHONE);
    notifierFacility.setContacts(List.of(contactPointInfo));

    PractitionerRole practitionerRole =
        PractitionerOrganizationCreator.createNotifierPractitionerRole(notifierFacility);

    assertThat(practitionerRole.getOrganization().getResource()).isInstanceOf(Organization.class);
    Organization organization = (Organization) practitionerRole.getOrganization().getResource();
    assertThat(organization.getName()).isEqualTo("Test Institution");
    assertThat(organization.getIdentifierFirstRep().getValue()).isEqualTo("123456789");
  }

  @Test
  void createNotifierPractitionerRoleShouldHandleMissingBsnrGracefully() {
    NotifierFacility notifierFacility = new NotifierFacility();
    FacilityAddressInfo addressInfo = new FacilityAddressInfo();
    notifierFacility.setAddress(addressInfo);
    FacilityInfo facilityInfo = new FacilityInfo();
    facilityInfo.setInstitutionName("Test Institution");
    facilityInfo.setExistsBsnr(false);
    notifierFacility.setFacilityInfo(facilityInfo);
    PractitionerInfo practitionerInfo = new PractitionerInfo();
    notifierFacility.setContact(practitionerInfo);
    ContactPointInfo contactPointInfo = new ContactPointInfo();
    contactPointInfo.setContactType(ContactPointInfo.ContactTypeEnum.PHONE);
    notifierFacility.setContacts(List.of(contactPointInfo));

    PractitionerRole practitionerRole =
        PractitionerOrganizationCreator.createNotifierPractitionerRole(notifierFacility);

    assertThat(practitionerRole.getOrganization().getResource()).isInstanceOf(Organization.class);
    Organization organization = (Organization) practitionerRole.getOrganization().getResource();
    assertThat(organization.getName()).isEqualTo("Test Institution");
    assertThat(organization.getIdentifier()).isEmpty();
  }

  @Test
  void createSubmitterPractitionerRoleShouldReturnValidPractitionerRole() {
    SubmitterFacility submitterFacility = new SubmitterFacility();
    SubmittingFacilityInfo facilityInfo = new SubmittingFacilityInfo();
    facilityInfo.setInstitutionName("Submitter Institution");
    facilityInfo.setDepartmentName("Department A");
    submitterFacility.setFacilityInfo(facilityInfo);
    FacilityAddressInfo addressInfo = new FacilityAddressInfo();
    submitterFacility.setAddress(addressInfo);
    PractitionerInfo practitionerInfo = new PractitionerInfo();
    submitterFacility.setContact(practitionerInfo);
    ContactPointInfo contactPointInfo = new ContactPointInfo();
    contactPointInfo.setContactType(ContactPointInfo.ContactTypeEnum.PHONE);
    submitterFacility.setContacts(List.of(contactPointInfo));

    PractitionerRole practitionerRole =
        PractitionerOrganizationCreator.createSubmitterPractitionerRole(submitterFacility, false);

    assertThat(practitionerRole.getOrganization().getResource()).isInstanceOf(Organization.class);
    Organization organization = (Organization) practitionerRole.getOrganization().getResource();
    assertThat(organization.getName()).isEqualTo("Submitter Institution");
    assertThat(organization.getContactFirstRep().getAddress().getLine().get(0).getValue())
        .isEqualTo("Department A");
  }

  @Test
  void createSubmitterPractitionerRoleShouldAddNotifiedPersonFacilityProfileWhenFlagIsTrue() {
    SubmitterFacility submitterFacility = new SubmitterFacility();
    SubmittingFacilityInfo facilityInfo = new SubmittingFacilityInfo();
    facilityInfo.setInstitutionName("Submitter Institution");
    submitterFacility.setFacilityInfo(facilityInfo);
    FacilityAddressInfo addressInfo = new FacilityAddressInfo();
    submitterFacility.setAddress(addressInfo);
    PractitionerInfo practitionerInfo = new PractitionerInfo();
    submitterFacility.setContact(practitionerInfo);
    ContactPointInfo contactPointInfo = new ContactPointInfo();
    contactPointInfo.setContactType(ContactPointInfo.ContactTypeEnum.PHONE);
    submitterFacility.setContacts(List.of(contactPointInfo));

    PractitionerRole practitionerRole =
        PractitionerOrganizationCreator.createSubmitterPractitionerRole(submitterFacility, true);

    assertThat(practitionerRole.getOrganization().getResource()).isInstanceOf(Organization.class);
    Organization organization = (Organization) practitionerRole.getOrganization().getResource();
    assertThat(organization.getMeta().getProfile())
        .extracting("value")
        .containsExactlyInAnyOrder(
            "https://demis.rki.de/fhir/StructureDefinition/SubmittingFacility",
            "https://demis.rki.de/fhir/StructureDefinition/NotifiedPersonFacility");
  }

  @Test
  void createNotifierPractitionerRoleShouldThrowExceptionForNullNotifierFacility() {
    assertThrows(
        NullPointerException.class,
        () -> PractitionerOrganizationCreator.createNotifierPractitionerRole(null));
  }

  @Test
  void createSubmitterPractitionerRoleShouldThrowExceptionForNullSubmitterFacility() {
    assertThrows(
        NullPointerException.class,
        () -> PractitionerOrganizationCreator.createSubmitterPractitionerRole(null, false));
  }
}
