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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.laboratory.NotificationBundleLaboratoryDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.AddressDataBuilder;
import de.gematik.demis.notificationgateway.common.dto.AddressType;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPerson;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPersonAddressInfo;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPersonAnonymous;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPersonBasicInfo;
import de.gematik.demis.notificationgateway.common.dto.PathogenTest;
import java.time.LocalDate;
import java.util.Collections;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PatientCreatorTest {

  @DisplayName("Regression Test for follow up notifications")
  @Nested
  class RegressionTest {
    @Test
    void createsPatientWithValidData() {
      NotificationBundleLaboratoryDataBuilder bundleBuilder =
          mock(NotificationBundleLaboratoryDataBuilder.class);
      PathogenTest rawData = new PathogenTest();
      PractitionerRole submittingRole = new PractitionerRole();
      NotifiedPerson notifiedPerson = new NotifiedPerson();
      rawData.setNotifiedPerson(notifiedPerson);
      NotifiedPersonAddressInfo addressInfo = new NotifiedPersonAddressInfo();
      notifiedPerson.setResidenceAddress(addressInfo);
      addressInfo.setAddressType(AddressType.PRIMARY);
      NotifiedPersonAddressInfo addressInfo2 = new NotifiedPersonAddressInfo();
      notifiedPerson.setCurrentAddress(addressInfo2);
      addressInfo2.setAddressType(AddressType.CURRENT);
      NotifiedPersonBasicInfo basicInfo = new NotifiedPersonBasicInfo();
      notifiedPerson.setInfo(basicInfo);

      basicInfo.setBirthDate(LocalDate.of(1990, 1, 1));
      basicInfo.setGender(NotifiedPersonBasicInfo.GenderEnum.MALE);
      basicInfo.setFirstname("Max");
      basicInfo.setLastname("Mustermann");

      Organization organization = mock(Organization.class);
      submittingRole.setOrganization(new Reference(organization));

      Patient result = PatientCreator.createPatient(bundleBuilder, rawData, submittingRole, false);

      assertThat(result.getNameFirstRep().getGivenAsSingleString()).isEqualTo("Max");
      assertThat(result.getNameFirstRep().getFamily()).isEqualTo("Mustermann");
      assertThat(result.getGender()).isEqualTo(Enumerations.AdministrativeGender.MALE);
      assertThat(result.getBirthDateElement().getValue()).isEqualTo("1990-01-01");
      verifyNoInteractions(bundleBuilder);
    }

    @Test
    void shouldReturnExceptionForMissingAddressData() {
      NotificationBundleLaboratoryDataBuilder bundleBuilder =
          mock(NotificationBundleLaboratoryDataBuilder.class);
      PathogenTest rawData = mock(PathogenTest.class);
      PractitionerRole submittingRole = mock(PractitionerRole.class);
      NotifiedPerson notifiedPerson = mock(NotifiedPerson.class);

      when(rawData.getNotifiedPerson()).thenReturn(notifiedPerson);
      when(notifiedPerson.getCurrentAddress()).thenReturn(null);

      assertThatThrownBy(
              () -> PatientCreator.createPatient(bundleBuilder, rawData, submittingRole, false))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Current address of patient cannot be null");
    }

    @Test
    void shouldUseFacilityAddressWithOtherFacilityAsType() {
      NotificationBundleLaboratoryDataBuilder bundleBuilder =
          mock(NotificationBundleLaboratoryDataBuilder.class);

      NotifiedPersonAddressInfo addressInfoResidence = new NotifiedPersonAddressInfo();
      addressInfoResidence.setAddressType(AddressType.PRIMARY);
      addressInfoResidence.setStreet("Straße");

      NotifiedPersonAddressInfo addressInfoSubmittingAddress = new NotifiedPersonAddressInfo();
      addressInfoSubmittingAddress.setAddressType(AddressType.OTHER_FACILITY);

      NotifiedPersonBasicInfo basicInfo = new NotifiedPersonBasicInfo();
      basicInfo.setGender(NotifiedPersonBasicInfo.GenderEnum.MALE);
      basicInfo.setFirstname("Max");
      basicInfo.setLastname("Mustermann");
      NotifiedPerson notifiedPerson = new NotifiedPerson();
      notifiedPerson.setResidenceAddress(addressInfoResidence);
      notifiedPerson.setCurrentAddress(addressInfoSubmittingAddress);
      notifiedPerson.setInfo(basicInfo);

      PathogenTest rawData = new PathogenTest();
      rawData.setNotifiedPerson(notifiedPerson);

      PractitionerRole submittingRole = new PractitionerRole();
      Organization submittingOrganization = new Organization();
      submittingOrganization.setName("submitter");
      submittingOrganization.addAddress(
          new Address().setLine(Collections.singletonList(new StringType("Pfad"))));
      submittingRole.setOrganization(new Reference(submittingOrganization));

      Patient result = PatientCreator.createPatient(bundleBuilder, rawData, submittingRole, false);

      assertThat(result.getAddress()).hasSize(2);
      assertThat(result.getAddress().get(0).getExtension()).hasSize(2);
      assertThat(result.getAddress().get(0).getExtension().get(0).getUrl())
          .contains("FacilityAddressNotifiedPerson");
      assertThat(result.getAddress().get(0).getExtension().get(1).getUrl()).contains("AddressUse");
      assertThat(result.getAddress().get(0).getExtension().get(1).getValue())
          .extracting("code")
          .isEqualTo("current");
      assertThat(result.getAddress().get(1).getExtension().get(0).getUrl()).contains("AddressUse");
      assertThat(result.getAddress().get(1).getExtension().get(0).getValue())
          .extracting("code")
          .isEqualTo("primary");
      verify(bundleBuilder).addAdditionalEntry(any(Organization.class));
    }

    @Test
    void addsSubmittingFacilityAddress() {
      NotificationBundleLaboratoryDataBuilder bundleBuilder =
          new NotificationBundleLaboratoryDataBuilder();

      NotifiedPersonAddressInfo addressInfoResidence = new NotifiedPersonAddressInfo();
      addressInfoResidence.setAddressType(AddressType.PRIMARY);
      addressInfoResidence.setStreet("Straße");

      NotifiedPersonAddressInfo addressInfoSubmittingAddress = new NotifiedPersonAddressInfo();
      addressInfoSubmittingAddress.setAddressType(AddressType.SUBMITTING_FACILITY);

      NotifiedPersonBasicInfo basicInfo = new NotifiedPersonBasicInfo();
      basicInfo.setGender(NotifiedPersonBasicInfo.GenderEnum.MALE);
      basicInfo.setLastname("Lastname");
      basicInfo.setFirstname("Firstname");
      NotifiedPerson notifiedPerson = new NotifiedPerson();
      notifiedPerson.setResidenceAddress(addressInfoResidence);
      notifiedPerson.setCurrentAddress(addressInfoSubmittingAddress);
      notifiedPerson.setInfo(basicInfo);

      PathogenTest rawData = new PathogenTest();
      rawData.setNotifiedPerson(notifiedPerson);

      PractitionerRole submittingRole = new PractitionerRole();
      Organization submittingOrganization = new Organization();
      submittingOrganization.setName("submitter");
      submittingOrganization.addAddress(
          new Address().setLine(Collections.singletonList(new StringType("Pfad"))));
      submittingRole.setOrganization(new Reference(submittingOrganization));

      Patient result = PatientCreator.createPatient(bundleBuilder, rawData, submittingRole, false);

      assertThat(result.getAddress()).hasSize(2);
      assertThat(result.getAddress().get(0).getExtension()).hasSize(2);
      assertThat(result.getAddress().get(0).getExtension().get(0).getUrl())
          .contains("FacilityAddressNotifiedPerson");
      assertThat(result.getAddress().get(0).getExtension().get(1).getUrl()).contains("AddressUse");
      assertThat(result.getAddress().get(0).getExtension().get(1).getValue())
          .extracting("code")
          .isEqualTo("current");
      assertThat(result.getAddress().get(1).getExtension().get(0).getUrl()).contains("AddressUse");
      assertThat(result.getAddress().get(1).getExtension().get(0).getValue())
          .extracting("code")
          .isEqualTo("primary");
    }
  }

  @Test
  void createsPatientWithValidData() {
    NotificationBundleLaboratoryDataBuilder bundleBuilder =
        mock(NotificationBundleLaboratoryDataBuilder.class);
    PathogenTest rawData = new PathogenTest();
    PractitionerRole submittingRole = new PractitionerRole();
    NotifiedPerson notifiedPerson = new NotifiedPerson();
    rawData.setNotifiedPerson(notifiedPerson);
    NotifiedPersonAddressInfo addressInfo = new NotifiedPersonAddressInfo();
    notifiedPerson.setResidenceAddress(addressInfo);
    addressInfo.setAddressType(AddressType.PRIMARY);
    NotifiedPersonAddressInfo addressInfo2 = new NotifiedPersonAddressInfo();
    notifiedPerson.setCurrentAddress(addressInfo2);
    addressInfo2.setAddressType(AddressType.CURRENT);
    NotifiedPersonBasicInfo basicInfo = new NotifiedPersonBasicInfo();
    notifiedPerson.setInfo(basicInfo);

    basicInfo.setBirthDate(LocalDate.of(1990, 1, 1));
    basicInfo.setGender(NotifiedPersonBasicInfo.GenderEnum.MALE);
    basicInfo.setFirstname("Max");
    basicInfo.setLastname("Mustermann");

    Organization organization = mock(Organization.class);
    submittingRole.setOrganization(new Reference(organization));

    Patient result = PatientCreator.createPatient(bundleBuilder, rawData, submittingRole, true);

    assertThat(result.getNameFirstRep().getGivenAsSingleString()).isEqualTo("Max");
    assertThat(result.getNameFirstRep().getFamily()).isEqualTo("Mustermann");
    assertThat(result.getGender()).isEqualTo(Enumerations.AdministrativeGender.MALE);
    assertThat(result.getBirthDateElement().getValue()).isEqualTo("1990-01-01");
    verifyNoInteractions(bundleBuilder);
  }

  @Test
  void shouldReturnExceptionForMissingAddressData() {
    NotificationBundleLaboratoryDataBuilder bundleBuilder =
        mock(NotificationBundleLaboratoryDataBuilder.class);
    PathogenTest rawData = mock(PathogenTest.class);
    PractitionerRole submittingRole = mock(PractitionerRole.class);
    NotifiedPerson notifiedPerson = mock(NotifiedPerson.class);

    when(rawData.getNotifiedPerson()).thenReturn(notifiedPerson);
    when(notifiedPerson.getCurrentAddress()).thenReturn(null);

    assertThatThrownBy(
            () -> PatientCreator.createPatient(bundleBuilder, rawData, submittingRole, true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Current address of patient cannot be null");
  }

  @Test
  void shouldUseDifferentFacilityAddress() {
    NotificationBundleLaboratoryDataBuilder bundleBuilder =
        mock(NotificationBundleLaboratoryDataBuilder.class);

    NotifiedPersonAddressInfo addressInfoResidence = new NotifiedPersonAddressInfo();
    addressInfoResidence.setAddressType(AddressType.PRIMARY);
    addressInfoResidence.setStreet("Straße");

    NotifiedPersonAddressInfo addressInfoSubmittingAddress = new NotifiedPersonAddressInfo();
    addressInfoSubmittingAddress.setAddressType(AddressType.OTHER_FACILITY);

    NotifiedPersonBasicInfo basicInfo = new NotifiedPersonBasicInfo();
    basicInfo.setGender(NotifiedPersonBasicInfo.GenderEnum.MALE);
    basicInfo.setFirstname("Max");
    basicInfo.setLastname("Mustermann");
    NotifiedPerson notifiedPerson = new NotifiedPerson();
    notifiedPerson.setResidenceAddress(addressInfoResidence);
    notifiedPerson.setCurrentAddress(addressInfoSubmittingAddress);
    notifiedPerson.setInfo(basicInfo);

    PathogenTest rawData = new PathogenTest();
    rawData.setNotifiedPerson(notifiedPerson);

    PractitionerRole submittingRole = new PractitionerRole();
    Organization submittingOrganization = new Organization();
    submittingOrganization.setName("submitter");
    submittingOrganization.addAddress(
        new Address().setLine(Collections.singletonList(new StringType("Pfad"))));
    submittingRole.setOrganization(new Reference(submittingOrganization));

    Patient result = PatientCreator.createPatient(bundleBuilder, rawData, submittingRole, true);

    assertThat(result.getAddress()).hasSize(2);
    assertThat(result.getAddress().get(0).getExtension()).hasSize(2);
    assertThat(result.getAddress().get(0).getExtension().get(0).getUrl())
        .contains("FacilityAddressNotifiedPerson");
    assertThat(result.getAddress().get(0).getExtension().get(1).getUrl()).contains("AddressUse");
    assertThat(result.getAddress().get(0).getExtension().get(1).getValue())
        .extracting("code")
        .isEqualTo("current");
    assertThat(result.getAddress().get(1).getExtension().get(0).getUrl()).contains("AddressUse");
    assertThat(result.getAddress().get(1).getExtension().get(0).getValue())
        .extracting("code")
        .isEqualTo("primary");
    verify(bundleBuilder).addAdditionalEntry(any(Organization.class));
  }

  @Test
  void addsSubmittingFacilityAddress() {
    NotificationBundleLaboratoryDataBuilder bundleBuilder =
        new NotificationBundleLaboratoryDataBuilder();

    NotifiedPersonAddressInfo addressInfoResidence = new NotifiedPersonAddressInfo();
    addressInfoResidence.setAddressType(AddressType.PRIMARY);
    addressInfoResidence.setStreet("Straße");

    NotifiedPersonAddressInfo addressInfoSubmittingAddress = new NotifiedPersonAddressInfo();
    addressInfoSubmittingAddress.setAddressType(AddressType.SUBMITTING_FACILITY);

    NotifiedPersonBasicInfo basicInfo = new NotifiedPersonBasicInfo();
    basicInfo.setGender(NotifiedPersonBasicInfo.GenderEnum.MALE);
    basicInfo.setLastname("Lastname");
    basicInfo.setFirstname("Firstname");
    NotifiedPerson notifiedPerson = new NotifiedPerson();
    notifiedPerson.setResidenceAddress(addressInfoResidence);
    notifiedPerson.setCurrentAddress(addressInfoSubmittingAddress);
    notifiedPerson.setInfo(basicInfo);

    PathogenTest rawData = new PathogenTest();
    rawData.setNotifiedPerson(notifiedPerson);

    PractitionerRole submittingRole = new PractitionerRole();
    Organization submittingOrganization = new Organization();
    submittingOrganization.setName("submitter");
    submittingOrganization.addAddress(
        new Address().setLine(Collections.singletonList(new StringType("Pfad"))));
    submittingRole.setOrganization(new Reference(submittingOrganization));

    Patient result = PatientCreator.createPatient(bundleBuilder, rawData, submittingRole, false);

    assertThat(result.getAddress()).hasSize(2);
    assertThat(result.getAddress().get(0).getExtension()).hasSize(2);
    assertThat(result.getAddress().get(0).getExtension().get(0).getUrl())
        .contains("FacilityAddressNotifiedPerson");
    assertThat(result.getAddress().get(0).getExtension().get(1).getUrl()).contains("AddressUse");
    assertThat(result.getAddress().get(0).getExtension().get(1).getValue())
        .extracting("code")
        .isEqualTo("current");
    assertThat(result.getAddress().get(1).getExtension().get(0).getUrl()).contains("AddressUse");
    assertThat(result.getAddress().get(1).getExtension().get(0).getValue())
        .extracting("code")
        .isEqualTo("primary");
  }

  @Test
  void shouldCreateNotifiedPersonAnonymous() {
    NotificationBundleLaboratoryDataBuilder bundleBuilder =
        mock(NotificationBundleLaboratoryDataBuilder.class);
    PathogenTest rawData = new PathogenTest();
    PractitionerRole submittingRole = new PractitionerRole();
    NotifiedPersonAnonymous notifiedPersonAnonymous = new NotifiedPersonAnonymous();
    rawData.setNotifiedPersonAnonymous(notifiedPersonAnonymous);
    NotifiedPersonAddressInfo addressInfo = new NotifiedPersonAddressInfo();
    notifiedPersonAnonymous.setResidenceAddress(addressInfo);
    addressInfo.setAddressType(AddressType.PRIMARY);

    notifiedPersonAnonymous.setBirthDate("1990-01");
    notifiedPersonAnonymous.setGender(NotifiedPersonAnonymous.GenderEnum.MALE);

    Organization organization = mock(Organization.class);
    submittingRole.setOrganization(new Reference(organization));

    Patient result = PatientCreator.createPatient(bundleBuilder, rawData, submittingRole, true);

    assertThat(result.getNameFirstRep().getGivenAsSingleString()).isEmpty();
    assertThat(result.getNameFirstRep().getFamily()).isNull();
    assertThat(result.getGender()).isEqualTo(Enumerations.AdministrativeGender.MALE);
    assertThat(result.getBirthDateElement().getValue()).isEqualTo("1990-01-01");
    assertThat(result.getAddress().get(0).getExtension().get(0).getValue())
        .extracting("code")
        .isEqualTo(AddressDataBuilder.PRIMARY);
    verifyNoInteractions(bundleBuilder);
  }

  @Test
  void shouldCreateNotifiedPersonAnonymous_MissingAddressType() {
    NotificationBundleLaboratoryDataBuilder bundleBuilder =
        mock(NotificationBundleLaboratoryDataBuilder.class);
    PathogenTest rawData = new PathogenTest();
    PractitionerRole submittingRole = new PractitionerRole();
    NotifiedPersonAnonymous notifiedPersonAnonymous = new NotifiedPersonAnonymous();
    rawData.setNotifiedPersonAnonymous(notifiedPersonAnonymous);
    NotifiedPersonAddressInfo addressInfo = new NotifiedPersonAddressInfo();
    notifiedPersonAnonymous.setResidenceAddress(addressInfo);
    notifiedPersonAnonymous.setBirthDate("1990-01");
    notifiedPersonAnonymous.setGender(NotifiedPersonAnonymous.GenderEnum.MALE);

    Organization organization = mock(Organization.class);
    submittingRole.setOrganization(new Reference(organization));

    Patient result = PatientCreator.createPatient(bundleBuilder, rawData, submittingRole, true);

    assertThat(result.getNameFirstRep().getGivenAsSingleString()).isEmpty();
    assertThat(result.getNameFirstRep().getFamily()).isNull();
    assertThat(result.getGender()).isEqualTo(Enumerations.AdministrativeGender.MALE);
    assertThat(result.getBirthDateElement().getValue()).isEqualTo("1990-01-01");
    assertThat(result.getAddress().get(0).getExtension().get(0).getValue())
        .extracting("code")
        .isEqualTo(AddressDataBuilder.PRIMARY);
    verifyNoInteractions(bundleBuilder);
  }
}
