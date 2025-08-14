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

import static de.gematik.demis.notificationgateway.domain.pathogen.creator.BundleCreator.createBundle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.gematik.demis.notificationgateway.common.dto.*;
import de.gematik.demis.notificationgateway.common.enums.NotificationType;
import java.time.LocalDate;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Specimen;
import org.hl7.fhir.r4.model.Type;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BundleCreatorTest {

  @Test
  void createBundleShouldReturnValidBundleForValidPathogenTest() {
    PathogenTest pathogenTest = new PathogenTest();
    NotifiedPerson notifiedPerson = new NotifiedPerson();

    NotifiedPersonAddressInfo addressInfo = new NotifiedPersonAddressInfo();
    PathogenDTO pathogenDTO = new PathogenDTO();
    NotificationLaboratoryCategory notificationCategory = new NotificationLaboratoryCategory();
    NotifierFacility notifierFacility = new NotifierFacility();
    SubmitterFacility submitterFacility = new SubmitterFacility();
    SpecimenDTO specimenDTO = new SpecimenDTO();
    NotifiedPersonAddressInfo addressInfo2 = new NotifiedPersonAddressInfo();
    NotifiedPersonBasicInfo basicInfo = new NotifiedPersonBasicInfo();
    MethodPathogenDTO methodPathogenDTO = new MethodPathogenDTO();

    pathogenTest.setNotifiedPerson(notifiedPerson);
    pathogenTest.setPathogenDTO(pathogenDTO);
    pathogenDTO.setCodeDisplay(new CodeDisplay("PathogenCode"));
    notifiedPerson.setCurrentAddress(addressInfo);
    notifiedPerson.setResidenceAddress(addressInfo);
    addressInfo.setAddressType(AddressType.PRIMARY);
    notifiedPerson.setCurrentAddress(addressInfo2);
    addressInfo2.setAddressType(AddressType.CURRENT);
    notifiedPerson.setInfo(basicInfo);
    basicInfo.setLastname("ExpectedFamilyName");
    basicInfo.setFirstname("ExpectedFirstName");
    pathogenTest.setPathogenDTO(pathogenDTO);
    pathogenTest.setNotificationCategory(notificationCategory);
    pathogenTest.setNotifierFacility(notifierFacility);
    pathogenTest.setSubmittingFacility(submitterFacility);
    notificationCategory.setPathogen(new CodeDisplay("12345"));
    notificationCategory.setReportStatus(NotificationLaboratoryCategory.ReportStatusEnum.FINAL);
    specimenDTO.setMaterial(new CodeDisplay("MaterialCode").display("MaterialDisplay"));
    specimenDTO.setMethodPathogenList(List.of(methodPathogenDTO));
    methodPathogenDTO.setResult(MethodPathogenDTO.ResultEnum.POS);
    methodPathogenDTO.setMethod(new CodeDisplay("MethodCode").display("MethodDisplay"));
    pathogenDTO.setSpecimenList(List.of(specimenDTO));
    LocalDate date = LocalDate.now();
    specimenDTO.setExtractionDate(date);

    FacilityAddressInfo addressInfo3 = new FacilityAddressInfo();
    notifierFacility.setAddress(addressInfo3);
    FacilityInfo facilityInfo3 = new FacilityInfo();
    facilityInfo3.setInstitutionName("Test Institution");
    facilityInfo3.setExistsBsnr(true);
    facilityInfo3.setBsnr("123456789");
    notifierFacility.setFacilityInfo(facilityInfo3);
    PractitionerInfo practitionerInfo = new PractitionerInfo();
    notifierFacility.setContact(practitionerInfo);
    ContactPointInfo contactPointInfo = new ContactPointInfo();
    contactPointInfo.setContactType(ContactPointInfo.ContactTypeEnum.PHONE);
    notifierFacility.setContacts(List.of(contactPointInfo));

    SubmittingFacilityInfo facilityInfo4 = new SubmittingFacilityInfo();
    facilityInfo4.setInstitutionName("Submitter Institution");
    facilityInfo4.setDepartmentName("Department A");
    submitterFacility.setFacilityInfo(facilityInfo4);
    FacilityAddressInfo addressInfo4 = new FacilityAddressInfo();
    submitterFacility.setAddress(addressInfo4);
    PractitionerInfo practitionerInfo2 = new PractitionerInfo();
    submitterFacility.setContact(practitionerInfo2);
    ContactPointInfo contactPointInfo2 = new ContactPointInfo();
    contactPointInfo2.setContactType(ContactPointInfo.ContactTypeEnum.PHONE);
    submitterFacility.setContacts(List.of(contactPointInfo2));

    Bundle bundle = createBundle(pathogenTest, NotificationType.NOMINAL, false, false);

    assertThat(bundle).isNotNull();
    assertThat(bundle.getType()).isEqualTo(Bundle.BundleType.DOCUMENT);
    assertThat(bundle.getEntry()).isNotEmpty();
    assertThat(bundle.getEntry())
        .hasSizeGreaterThanOrEqualTo(
            7); // Ensure at least 6 entries (Patient, 2 PractitionerRole, Observation, Specimen,
    // DiagnosticReport, Composition)

    // Verify Patient resource
    Patient patient =
        (Patient)
            bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> resource instanceof Patient)
                .findFirst()
                .orElseThrow(
                    () -> new AssertionError("Patient resource not found in bundleBuilder"));
    assertThat(patient.getNameFirstRep().getFamily()).isEqualTo("ExpectedFamilyName");
    //    assertThat(patient.getN().getFamily()).isEqualTo("ExpectedFamilyName");

    // Verify PractitionerRole resources
    long practitionerRoleCount =
        bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> resource instanceof PractitionerRole)
            .count();
    assertThat(practitionerRoleCount).isEqualTo(2);

    // Verify Specimen resource
    Specimen specimen =
        (Specimen)
            bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> resource instanceof Specimen)
                .findFirst()
                .orElseThrow(
                    () -> new AssertionError("Specimen resource not found in bundleBuilder"));
    assertThat(specimen.getType().getCodingFirstRep().getCode()).isEqualTo("MaterialCode");
    assertThat(specimen.getCollection().getCollectedDateTimeType().getValue())
        .isEqualTo(java.sql.Date.valueOf(date));

    // Verify DiagnosticReport resource
    DiagnosticReport diagnosticReport =
        (DiagnosticReport)
            bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> resource instanceof DiagnosticReport)
                .findFirst()
                .orElseThrow(
                    () ->
                        new AssertionError("DiagnosticReport resource not found in bundleBuilder"));
    assertThat(diagnosticReport.getConclusionCodeFirstRep().getCodingFirstRep().getCode())
        .isEqualTo("pathogenDetected");
    assertThat(diagnosticReport.getStatus())
        .isEqualTo(DiagnosticReport.DiagnosticReportStatus.FINAL);

    // Verify Composition resource
    Composition composition =
        (Composition)
            bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> resource instanceof Composition)
                .findFirst()
                .orElseThrow(
                    () -> new AssertionError("Composition resource not found in bundleBuilder"));
    assertThat(composition.getSubject().getReference()).isEqualTo(patient.getId());
    assertThat(composition.getSectionFirstRep().getEntryFirstRep().getResource())
        .isEqualTo(diagnosticReport);
  }

  @Test
  void createBundleShouldThrowExceptionWhenSubmittingFacilityIsNull() {
    PathogenTest pathogenTest = new PathogenTest();
    NotifiedPerson notifiedPerson = new NotifiedPerson();

    NotifiedPersonAddressInfo addressInfo = new NotifiedPersonAddressInfo();
    PathogenDTO pathogenDTO = new PathogenDTO();
    NotificationLaboratoryCategory notificationCategory = new NotificationLaboratoryCategory();
    NotifierFacility notifierFacility = new NotifierFacility();
    SubmitterFacility submitterFacility = new SubmitterFacility();
    SpecimenDTO specimenDTO = new SpecimenDTO();
    NotifiedPersonAddressInfo addressInfo2 = new NotifiedPersonAddressInfo();
    NotifiedPersonBasicInfo basicInfo = new NotifiedPersonBasicInfo();
    MethodPathogenDTO methodPathogenDTO = new MethodPathogenDTO();

    pathogenTest.setNotifiedPerson(notifiedPerson);
    pathogenTest.setPathogenDTO(pathogenDTO);
    pathogenDTO.setCodeDisplay(new CodeDisplay("PathogenCode"));
    notifiedPerson.setCurrentAddress(addressInfo);
    notifiedPerson.setResidenceAddress(addressInfo);
    addressInfo.setAddressType(AddressType.PRIMARY);
    notifiedPerson.setCurrentAddress(addressInfo2);
    addressInfo2.setAddressType(AddressType.CURRENT);
    notifiedPerson.setInfo(basicInfo);
    basicInfo.setLastname("ExpectedFamilyName");
    pathogenTest.setPathogenDTO(pathogenDTO);
    pathogenTest.setNotificationCategory(notificationCategory);
    pathogenTest.setNotifierFacility(notifierFacility);
    pathogenTest.setSubmittingFacility(null);
    notificationCategory.setPathogen(new CodeDisplay("12345"));
    notificationCategory.setReportStatus(NotificationLaboratoryCategory.ReportStatusEnum.FINAL);
    specimenDTO.setMaterial(new CodeDisplay("MaterialCode").display("MaterialDisplay"));
    specimenDTO.setMethodPathogenList(List.of(methodPathogenDTO));
    methodPathogenDTO.setResult(MethodPathogenDTO.ResultEnum.POS);
    methodPathogenDTO.setMethod(new CodeDisplay("MethodCode").display("MethodDisplay"));
    pathogenDTO.setSpecimenList(List.of(specimenDTO));
    LocalDate date = LocalDate.now();
    specimenDTO.setExtractionDate(date);

    FacilityAddressInfo addressInfo3 = new FacilityAddressInfo();
    notifierFacility.setAddress(addressInfo3);
    FacilityInfo facilityInfo3 = new FacilityInfo();
    facilityInfo3.setInstitutionName("Test Institution");
    facilityInfo3.setExistsBsnr(true);
    facilityInfo3.setBsnr("123456789");
    notifierFacility.setFacilityInfo(facilityInfo3);
    PractitionerInfo practitionerInfo = new PractitionerInfo();
    notifierFacility.setContact(practitionerInfo);
    ContactPointInfo contactPointInfo = new ContactPointInfo();
    contactPointInfo.setContactType(ContactPointInfo.ContactTypeEnum.PHONE);
    notifierFacility.setContacts(List.of(contactPointInfo));

    assertThrows(
            IllegalArgumentException.class,
            () -> createBundle(pathogenTest, NotificationType.NOMINAL, false, false))
        .getMessage()
        .contains("Submitting facility must not be null");
    ;
  }

  @Test
  void createBundleShouldHandleNotifiedPersonWithSubmittingFacilityAddressType() {
    PathogenTest pathogenTest = new PathogenTest();
    NotifiedPerson notifiedPerson = new NotifiedPerson();

    NotifiedPersonAddressInfo addressInfo = new NotifiedPersonAddressInfo();
    PathogenDTO pathogenDTO = new PathogenDTO();
    NotificationLaboratoryCategory notificationCategory = new NotificationLaboratoryCategory();
    NotifierFacility notifierFacility = new NotifierFacility();
    SubmitterFacility submitterFacility = new SubmitterFacility();
    SpecimenDTO specimenDTO = new SpecimenDTO();
    NotifiedPersonAddressInfo addressInfo2 = new NotifiedPersonAddressInfo();
    NotifiedPersonBasicInfo basicInfo = new NotifiedPersonBasicInfo();
    MethodPathogenDTO methodPathogenDTO = new MethodPathogenDTO();

    pathogenTest.setNotifiedPerson(notifiedPerson);
    pathogenTest.setPathogenDTO(pathogenDTO);
    pathogenDTO.setCodeDisplay(new CodeDisplay("PathogenCode"));
    notifiedPerson.setCurrentAddress(addressInfo);
    notifiedPerson.setResidenceAddress(addressInfo);
    addressInfo.setAddressType(AddressType.PRIMARY);
    notifiedPerson.setCurrentAddress(addressInfo2);
    addressInfo2.setAddressType(AddressType.SUBMITTING_FACILITY);
    notifiedPerson.setInfo(basicInfo);
    basicInfo.setLastname("ExpectedFamilyName");
    basicInfo.setFirstname("ExpectedFirstName");
    pathogenTest.setPathogenDTO(pathogenDTO);
    pathogenTest.setNotificationCategory(notificationCategory);
    pathogenTest.setNotifierFacility(notifierFacility);
    pathogenTest.setSubmittingFacility(submitterFacility);
    notificationCategory.setPathogen(new CodeDisplay("12345"));
    notificationCategory.setReportStatus(NotificationLaboratoryCategory.ReportStatusEnum.FINAL);
    specimenDTO.setMaterial(new CodeDisplay("MaterialCode").display("MaterialDisplay"));
    specimenDTO.setMethodPathogenList(List.of(methodPathogenDTO));
    methodPathogenDTO.setResult(MethodPathogenDTO.ResultEnum.POS);
    methodPathogenDTO.setMethod(new CodeDisplay("MethodCode").display("MethodDisplay"));
    pathogenDTO.setSpecimenList(List.of(specimenDTO));
    LocalDate date = LocalDate.now();
    specimenDTO.setExtractionDate(date);

    FacilityAddressInfo addressInfo3 = new FacilityAddressInfo();
    notifierFacility.setAddress(addressInfo3);
    FacilityInfo facilityInfo3 = new FacilityInfo();
    facilityInfo3.setInstitutionName("Test Institution");
    facilityInfo3.setExistsBsnr(true);
    facilityInfo3.setBsnr("123456789");
    notifierFacility.setFacilityInfo(facilityInfo3);
    PractitionerInfo practitionerInfo = new PractitionerInfo();
    notifierFacility.setContact(practitionerInfo);
    ContactPointInfo contactPointInfo = new ContactPointInfo();
    contactPointInfo.setContactType(ContactPointInfo.ContactTypeEnum.PHONE);
    notifierFacility.setContacts(List.of(contactPointInfo));

    SubmittingFacilityInfo facilityInfo4 = new SubmittingFacilityInfo();
    facilityInfo4.setInstitutionName("Submitter Institution");
    facilityInfo4.setDepartmentName("Department A");
    submitterFacility.setFacilityInfo(facilityInfo4);
    FacilityAddressInfo addressInfo4 = new FacilityAddressInfo();
    addressInfo4.setStreet("Submitter Street");
    addressInfo4.setCity("Submitter City");
    addressInfo4.setZip("12345");
    submitterFacility.setAddress(addressInfo4);
    PractitionerInfo practitionerInfo2 = new PractitionerInfo();
    submitterFacility.setContact(practitionerInfo2);
    ContactPointInfo contactPointInfo2 = new ContactPointInfo();
    contactPointInfo2.setContactType(ContactPointInfo.ContactTypeEnum.PHONE);
    submitterFacility.setContacts(List.of(contactPointInfo2));

    Bundle bundle = createBundle(pathogenTest, NotificationType.NOMINAL, false, false);

    assertThat(bundle).isNotNull();
    assertThat(bundle.getType()).isEqualTo(Bundle.BundleType.DOCUMENT);
    assertThat(bundle.getEntry()).isNotEmpty();
    assertThat(bundle.getEntry()).hasSizeGreaterThanOrEqualTo(8);

    // Verify Patient resource
    Patient patient =
        (Patient)
            bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> resource instanceof Patient)
                .findFirst()
                .orElseThrow(
                    () -> new AssertionError("Patient resource not found in bundleBuilder"));
    assertThat(patient.getNameFirstRep().getFamily()).isEqualTo("ExpectedFamilyName");

    // Verify Patient address matches SubmitterFacility address through an extension
    Type value = patient.getAddressFirstRep().getExtension().getFirst().getValue();
    assertThat(value).isInstanceOf(Reference.class);
    Reference reference = (Reference) value;
    IBaseResource resource1 = reference.getResource();
    assertThat(resource1).isInstanceOf(Organization.class);
    Organization organization = (Organization) resource1;
    assertThat(organization.getName()).isEqualTo("Submitter Institution");
    Type value1 = patient.getAddressFirstRep().getExtension().get(1).getValue();
    assertThat(((Coding) value1).getCode()).isEqualTo("current");

    // Verify PractitionerRole resources
    long practitionerRoleCount =
        bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> resource instanceof PractitionerRole)
            .count();
    assertThat(practitionerRoleCount).isEqualTo(2);

    // Verify Specimen resource
    Specimen specimen =
        (Specimen)
            bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> resource instanceof Specimen)
                .findFirst()
                .orElseThrow(
                    () -> new AssertionError("Specimen resource not found in bundleBuilder"));
    assertThat(specimen.getType().getCodingFirstRep().getCode()).isEqualTo("MaterialCode");
    assertThat(specimen.getCollection().getCollectedDateTimeType().getValue())
        .isEqualTo(java.sql.Date.valueOf(date));

    // Verify DiagnosticReport resource
    DiagnosticReport diagnosticReport =
        (DiagnosticReport)
            bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> resource instanceof DiagnosticReport)
                .findFirst()
                .orElseThrow(
                    () ->
                        new AssertionError("DiagnosticReport resource not found in bundleBuilder"));
    assertThat(diagnosticReport.getConclusionCodeFirstRep().getCodingFirstRep().getCode())
        .isEqualTo("pathogenDetected");
    assertThat(diagnosticReport.getStatus())
        .isEqualTo(DiagnosticReport.DiagnosticReportStatus.FINAL);

    // Verify Composition resource
    Composition composition =
        (Composition)
            bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> resource instanceof Composition)
                .findFirst()
                .orElseThrow(
                    () -> new AssertionError("Composition resource not found in bundleBuilder"));
    assertThat(composition.getSubject().getReference()).isEqualTo(patient.getId());
    assertThat(composition.getSectionFirstRep().getEntryFirstRep().getResource())
        .isEqualTo(diagnosticReport);
  }
}
