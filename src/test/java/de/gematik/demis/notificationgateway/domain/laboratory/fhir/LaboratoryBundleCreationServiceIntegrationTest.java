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

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.SYSTEM_LOINC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.gematik.demis.notificationgateway.common.constants.FhirConstants;
import de.gematik.demis.notificationgateway.common.dto.ContactPointInfo;
import de.gematik.demis.notificationgateway.common.dto.Diagnosis;
import de.gematik.demis.notificationgateway.common.dto.FacilityAddressInfo;
import de.gematik.demis.notificationgateway.common.dto.FacilityInfo;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPerson;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPersonAddressInfo;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPersonBasicInfo;
import de.gematik.demis.notificationgateway.common.dto.NotifierFacility;
import de.gematik.demis.notificationgateway.common.dto.PractitionerInfo;
import de.gematik.demis.notificationgateway.common.dto.QuickTest;
import de.gematik.demis.notificationgateway.utils.FileUtils;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Composition.CompositionStatus;
import org.hl7.fhir.r4.model.Composition.SectionComponent;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.DiagnosticReport.DiagnosticReportStatus;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.HumanName.NameUse;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Organization.OrganizationContactComponent;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Specimen;
import org.hl7.fhir.r4.model.Specimen.SpecimenStatus;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LaboratoryBundleCreationServiceIntegrationTest {

  @Autowired private LaboratoryBundleCreationService laboratoryBundleCreationService;

  @Test
  void testCreateBundle() throws JsonProcessingException {
    QuickTest quickTest = FileUtils.createQuickTest("portal/laboratory/notification_content.json");
    final Bundle bundle = laboratoryBundleCreationService.createLaboratoryBundle(quickTest);

    assertNotNull(bundle);

    assertTrue(bundle.hasMeta());
    final Meta meta = bundle.getMeta();
    assertTrue(meta.hasLastUpdated());
    assertTrue(meta.hasProfile());
    assertEquals(
        LocalDate.now(),
        LocalDate.ofInstant(meta.getLastUpdated().toInstant(), ZoneId.systemDefault()));
    assertEquals(
        FhirConstants.PROFILE_NOTIFICATION_BUNDLE_LABORATORY,
        meta.getProfile().get(0).asStringValue());

    assertTrue(bundle.hasIdentifier());
    final Identifier identifier = bundle.getIdentifier();
    assertEquals(FhirConstants.NAMING_SYSTEM_NOTIFICATION_BUNDLE_ID, identifier.getSystem());
    assertTrue(identifier.hasValue());

    assertTrue(bundle.hasType());
    final BundleType type = bundle.getType();
    assertEquals(BundleType.DOCUMENT, type);

    assertTrue(bundle.hasTimestamp());
    assertEquals(
        LocalDate.now(),
        LocalDate.ofInstant(bundle.getTimestamp().toInstant(), ZoneId.systemDefault()));

    assertTrue(bundle.hasEntry());
    final List<BundleEntryComponent> entryList = bundle.getEntry();
    assertEquals(9, entryList.size());

    checkEntries(entryList, quickTest);
  }

  private void checkEntries(List<BundleEntryComponent> entryList, QuickTest quickTest) {
    final BundleEntryComponent notificationLaboratoryEntry = entryList.get(0);
    assertTrue(notificationLaboratoryEntry.hasFullUrl());

    final BundleEntryComponent notifiedPersonEntry = entryList.get(1);
    assertTrue(notifiedPersonEntry.hasFullUrl());
    final String notifiedPersonShortUrl =
        notifiedPersonEntry.getFullUrl().replace(FhirConstants.DEMIS_RKI_DE_FHIR, "");

    final BundleEntryComponent notifierFacilityEntry = entryList.get(2);
    assertTrue(notifierFacilityEntry.hasFullUrl());
    final String notifierFacilityShortUrl =
        notifierFacilityEntry.getFullUrl().replace(FhirConstants.DEMIS_RKI_DE_FHIR, "");

    final BundleEntryComponent notifierRoleEntry = entryList.get(3);
    assertTrue(notifierRoleEntry.hasFullUrl());
    final String notifierRoleShortUrl =
        notifierRoleEntry.getFullUrl().replace(FhirConstants.DEMIS_RKI_DE_FHIR, "");

    final BundleEntryComponent submittingFacilityEntry = entryList.get(4);
    assertTrue(submittingFacilityEntry.hasFullUrl());
    final String submittingFacilityShortUrl =
        submittingFacilityEntry.getFullUrl().replace(FhirConstants.DEMIS_RKI_DE_FHIR, "");

    final BundleEntryComponent submittingRoleEntry = entryList.get(5);
    assertTrue(submittingRoleEntry.hasFullUrl());
    final String submittingRoleShortUrl =
        submittingRoleEntry.getFullUrl().replace(FhirConstants.DEMIS_RKI_DE_FHIR, "");

    final BundleEntryComponent laboratoryReportEntry = entryList.get(6);
    assertTrue(laboratoryReportEntry.hasFullUrl());
    final String laboratoryReportShortUrl =
        laboratoryReportEntry.getFullUrl().replace(FhirConstants.DEMIS_RKI_DE_FHIR, "");

    final BundleEntryComponent pathogenDetectionEntry = entryList.get(7);
    assertTrue(pathogenDetectionEntry.hasFullUrl());
    final String pathogenDetectionShortUrl =
        pathogenDetectionEntry.getFullUrl().replace(FhirConstants.DEMIS_RKI_DE_FHIR, "");

    final BundleEntryComponent specimenEntry = entryList.get(8);
    assertTrue(specimenEntry.hasFullUrl());
    final String specimenShortUrl =
        specimenEntry.getFullUrl().replace(FhirConstants.DEMIS_RKI_DE_FHIR, "");

    checkNotificationLaboratory(
        notificationLaboratoryEntry,
        quickTest.getDiagnostic(),
        notifiedPersonShortUrl,
        notifierRoleShortUrl,
        laboratoryReportShortUrl);
    checkNotifiedPerson(notifiedPersonEntry, quickTest.getNotifiedPerson());
    checkNotifierRole(notifierRoleEntry, notifierFacilityShortUrl);
    checkNotifierFacility(notifierFacilityEntry, quickTest.getNotifierFacility());
    checkSubmittingFacility(submittingFacilityEntry, quickTest.getNotifierFacility());
    checkSubmittingRole(submittingRoleEntry, submittingFacilityShortUrl);
    checkLaboratoryReportCvdp(
        laboratoryReportEntry,
        quickTest.getDiagnostic(),
        notifiedPersonShortUrl,
        pathogenDetectionShortUrl);
    checkPathogenDetectionCvdp(pathogenDetectionEntry, notifiedPersonShortUrl, specimenShortUrl);
    checkSpecimenCvdp(
        specimenEntry, quickTest.getDiagnostic(), notifiedPersonShortUrl, submittingRoleShortUrl);
  }

  private void checkSpecimenCvdp(
      BundleEntryComponent specimenEntry,
      Diagnosis diagnosticInfo,
      String notifiedPersonShortUrl,
      String submittingRoleShortUrl) {
    assertTrue(specimenEntry.hasResource());
    final Specimen specimen = (Specimen) specimenEntry.getResource();

    assertTrue(specimen.hasId());

    assertTrue(specimen.hasMeta());
    final Meta meta = specimen.getMeta();
    assertTrue(meta.hasProfile());
    assertEquals(FhirConstants.PROFILE_SPECIMEN_CVDP, meta.getProfile().get(0).asStringValue());

    assertTrue(specimen.hasStatus());
    assertEquals(SpecimenStatus.AVAILABLE, specimen.getStatus());

    assertTrue(specimen.hasType());
    final CodeableConcept type = specimen.getType();
    assertEquals(1, type.getCoding().size());
    final Coding typeCoding = type.getCoding().get(0);
    assertEquals(FhirConstants.SYSTEM_SNOMED, typeCoding.getSystem());
    assertEquals("309164002", typeCoding.getCode());
    assertEquals("Upper respiratory swab sample (specimen)", typeCoding.getDisplay());

    assertTrue(specimen.hasSubject());
    final Reference subject = specimen.getSubject();
    assertTrue(subject.hasReference());
    assertEquals(notifiedPersonShortUrl, subject.getReference());

    assertTrue(specimen.hasReceivedTime());
    assertEquals(
        diagnosticInfo.getReceivedDate().toInstant(), specimen.getReceivedTime().toInstant());

    assertTrue(specimen.hasCollection());
    final Reference collector = specimen.getCollection().getCollector();
    assertTrue(collector.hasReference());
    assertEquals(submittingRoleShortUrl, collector.getReference());
  }

  private void checkPathogenDetectionCvdp(
      BundleEntryComponent pathogenDetectionEntry,
      String notifiedPersonShortUrl,
      String specimenShortUrl) {
    assertTrue(pathogenDetectionEntry.hasResource());
    final Observation pathogenDetection = (Observation) pathogenDetectionEntry.getResource();

    assertTrue(pathogenDetection.hasId());

    assertTrue(pathogenDetection.hasMeta());
    final Meta meta = pathogenDetection.getMeta();
    assertTrue(meta.hasProfile());
    assertEquals(
        FhirConstants.PROFILE_PATHOGEN_DETECTION_CVDP, meta.getProfile().get(0).asStringValue());

    assertTrue(pathogenDetection.hasStatus());
    assertEquals(ObservationStatus.FINAL, pathogenDetection.getStatus());

    assertTrue(pathogenDetection.hasCategory());
    assertEquals(1, pathogenDetection.getCategory().size());
    final List<Coding> categoryCodings = pathogenDetection.getCategory().get(0).getCoding();
    assertEquals(1, categoryCodings.size());
    final Coding categoryCoding = categoryCodings.get(0);
    assertEquals(FhirConstants.CODE_SYSTEM_OBSERVATION_CATEGORY, categoryCoding.getSystem());
    assertEquals("laboratory", categoryCoding.getCode());
    assertEquals("Laboratory", categoryCoding.getDisplay());

    assertTrue(pathogenDetection.hasCode());
    final CodeableConcept code = pathogenDetection.getCode();
    assertEquals(1, code.getCoding().size());
    final Coding codeCoding = code.getCoding().get(0);
    assertEquals(SYSTEM_LOINC, codeCoding.getSystem());
    assertEquals("94746-5", codeCoding.getCode());
    assertEquals(
        "SARS-CoV-2 (COVID-19) RNA [Cycle Threshold #] in Specimen by NAA with probe detection",
        codeCoding.getDisplay());
    assertFalse(code.hasText());

    assertTrue(pathogenDetection.hasSubject());
    final Reference subject = pathogenDetection.getSubject();
    assertTrue(subject.hasReference());
    assertEquals(notifiedPersonShortUrl, subject.getReference());

    assertTrue(pathogenDetection.hasValue());
    assertEquals("PCR-Schnelltest positiv", pathogenDetection.getValueStringType().asStringValue());

    assertTrue(pathogenDetection.hasInterpretation());
    assertEquals(1, pathogenDetection.getInterpretation().size());
    final List<Coding> interpretationCodings =
        pathogenDetection.getInterpretation().get(0).getCoding();
    assertEquals(1, interpretationCodings.size());
    final Coding interpretationCoding = interpretationCodings.get(0);
    assertEquals(
        FhirConstants.CODE_SYSTEM_OBSERVATION_INTERPRETATION, interpretationCoding.getSystem());
    assertEquals("POS", interpretationCoding.getCode());

    assertTrue(pathogenDetection.hasSpecimen());
    final Reference specimen = pathogenDetection.getSpecimen();
    assertTrue(specimen.hasReference());
    assertEquals(specimenShortUrl, specimen.getReference());
  }

  private void checkLaboratoryReportCvdp(
      BundleEntryComponent laboratoryReportEntry,
      Diagnosis diagnosticInfo,
      String notifiedPersonShortUrl,
      String pathogenDetectionShortUrl) {
    assertTrue(laboratoryReportEntry.hasResource());
    final DiagnosticReport laboratoryReport =
        (DiagnosticReport) laboratoryReportEntry.getResource();

    assertTrue(laboratoryReport.hasId());

    assertTrue(laboratoryReport.hasMeta());
    final Meta meta = laboratoryReport.getMeta();
    assertTrue(meta.hasProfile());
    assertEquals(
        FhirConstants.PROFILE_LABORATORY_REPORT_CVDP, meta.getProfile().get(0).asStringValue());

    assertTrue(laboratoryReport.hasStatus());
    assertEquals(DiagnosticReportStatus.FINAL, laboratoryReport.getStatus());

    assertTrue(laboratoryReport.hasCode());
    final CodeableConcept code = laboratoryReport.getCode();
    assertTrue(code.hasCoding());
    assertEquals(1, code.getCoding().size());
    final Coding codeCoding = code.getCoding().get(0);
    assertEquals(FhirConstants.CODE_SYSTEM_NOTIFICATION_CATEGORY, codeCoding.getSystem());
    assertEquals("cvdp", codeCoding.getCode());
    assertEquals(
        "Severe-Acute-Respiratory-Syndrome-Coronavirus-2 (SARS-CoV-2)", codeCoding.getDisplay());

    assertTrue(laboratoryReport.hasSubject());
    final Reference subject = laboratoryReport.getSubject();
    assertTrue(subject.hasReference());
    assertEquals(notifiedPersonShortUrl, subject.getReference());

    assertTrue(laboratoryReport.hasIssued());
    assertEquals(
        diagnosticInfo.getReceivedDate().toInstant(), laboratoryReport.getIssued().toInstant());

    assertTrue(laboratoryReport.hasResult());
    assertEquals(1, laboratoryReport.getResult().size());
    final Reference reference = laboratoryReport.getResult().get(0);
    assertTrue(reference.hasReference());
    assertEquals(pathogenDetectionShortUrl, reference.getReference());

    assertTrue(laboratoryReport.hasConclusionCode());
    assertEquals(1, laboratoryReport.getConclusionCode().size());
    final List<Coding> conclusionCodeCodings =
        laboratoryReport.getConclusionCode().get(0).getCoding();
    assertEquals(1, conclusionCodeCodings.size());
    final Coding conclusionCodeCoding = conclusionCodeCodings.get(0);
    assertEquals(FhirConstants.CODE_SYSTEM_CONCLUSION_CODE, conclusionCodeCoding.getSystem());
    assertEquals("pathogenDetected", conclusionCodeCoding.getCode());
    assertEquals("Meldepflichtiger Erreger nachgewiesen", conclusionCodeCoding.getDisplay());
  }

  private void checkSubmittingRole(
      BundleEntryComponent submittingRoleEntry, String submittingFacilityShortUrl) {
    assertTrue(submittingRoleEntry.hasResource());
    final PractitionerRole submittingRole = (PractitionerRole) submittingRoleEntry.getResource();

    assertTrue(submittingRole.hasId());

    assertTrue(submittingRole.hasMeta());
    final Meta meta = submittingRole.getMeta();
    assertTrue(meta.hasProfile());
    assertEquals(FhirConstants.PROFILE_SUBMITTING_ROLE, meta.getProfile().get(0).asStringValue());

    assertTrue(submittingRole.hasOrganization());
    final Reference organization = submittingRole.getOrganization();
    assertTrue(organization.hasReference());
    assertEquals(submittingFacilityShortUrl, organization.getReference());
  }

  private void checkSubmittingFacility(
      BundleEntryComponent submittingFacilityEntry, NotifierFacility notifierFacilityInfo) {
    final FacilityInfo facilityInfo = notifierFacilityInfo.getFacilityInfo();

    assertTrue(submittingFacilityEntry.hasResource());
    final Organization submittingFacility = (Organization) submittingFacilityEntry.getResource();

    assertTrue(submittingFacility.hasId());

    assertTrue(submittingFacility.hasMeta());
    final Meta meta = submittingFacility.getMeta();
    assertTrue(meta.hasProfile());
    assertEquals(
        FhirConstants.PROFILE_SUBMITTING_FACILITY, meta.getProfile().get(0).asStringValue());

    assertTrue(submittingFacility.hasType());
    final List<CodeableConcept> types = submittingFacility.getType();
    assertEquals(1, types.size());
    final List<Coding> typeCodings = types.get(0).getCoding();
    assertEquals(1, typeCodings.size());
    final Coding typeCoding = typeCodings.get(0);
    assertEquals(FhirConstants.CODE_SYSTEM_ORGANIZATION_TYPE, typeCoding.getSystem());
    assertEquals("physicianOffice", typeCoding.getCode());
    assertEquals("Arztpraxis", typeCoding.getDisplay());

    assertTrue(submittingFacility.hasName());
    assertEquals(facilityInfo.getInstitutionName(), submittingFacility.getName());

    assertTrue(submittingFacility.hasTelecom());
    final List<ContactPoint> telecomList = submittingFacility.getTelecom();
    assertEquals(1, telecomList.size());
    final ContactPoint contactPoint = telecomList.get(0);
    final ContactPointInfo contactPointInfo = notifierFacilityInfo.getContacts().get(0);
    assertEquals(contactPointInfo.getContactType().getValue(), contactPoint.getSystem().toCode());
    assertEquals(contactPointInfo.getValue(), contactPoint.getValue());

    assertTrue(submittingFacility.hasAddress());
    final List<Address> addresses = submittingFacility.getAddress();
    assertEquals(1, addresses.size());
    final Address address = addresses.get(0);
    checkAddress(address, notifierFacilityInfo.getAddress());
  }

  private void checkNotifierFacility(
      BundleEntryComponent notifierFacilityEntry,
      @NotNull @Valid NotifierFacility notifierFacilityInfo) {
    final FacilityInfo facilityInfo = notifierFacilityInfo.getFacilityInfo();

    assertTrue(notifierFacilityEntry.hasResource());
    final Organization notifierFacility = (Organization) notifierFacilityEntry.getResource();

    assertTrue(notifierFacility.hasId());

    assertTrue(notifierFacility.hasMeta());
    final Meta meta = notifierFacility.getMeta();
    assertTrue(meta.hasProfile());
    assertEquals(FhirConstants.PROFILE_NOTIFIER_FACILITY, meta.getProfile().get(0).asStringValue());

    assertTrue(notifierFacility.hasIdentifier());
    final List<Identifier> identifiers = notifierFacility.getIdentifier();
    assertEquals(1, identifiers.size());
    final Identifier identifier = identifiers.get(0);
    assertEquals(FhirConstants.NAMING_SYSTEM_BSNR, identifier.getSystem());
    assertEquals(facilityInfo.getBsnr(), identifier.getValue());

    assertTrue(notifierFacility.hasType());
    final List<CodeableConcept> types = notifierFacility.getType();
    assertEquals(1, types.size());
    final List<Coding> typeCodings = types.get(0).getCoding();
    assertEquals(1, typeCodings.size());
    final Coding typeCoding = typeCodings.get(0);
    assertEquals(FhirConstants.CODE_SYSTEM_ORGANIZATION_TYPE, typeCoding.getSystem());
    assertEquals("othPrivatLab", typeCoding.getCode());
    assertEquals("Sonstige private Untersuchungsstelle", typeCoding.getDisplay());

    assertTrue(notifierFacility.hasName());
    assertEquals(facilityInfo.getInstitutionName(), notifierFacility.getName());

    assertTrue(notifierFacility.hasTelecom());
    final List<ContactPoint> telecomList = notifierFacility.getTelecom();
    assertEquals(1, telecomList.size());
    final ContactPoint contactPoint = telecomList.get(0);
    final ContactPointInfo contactPointInfo = notifierFacilityInfo.getContacts().get(0);
    assertEquals(contactPointInfo.getContactType().getValue(), contactPoint.getSystem().toCode());
    assertEquals(contactPointInfo.getValue(), contactPoint.getValue());

    assertTrue(notifierFacility.hasAddress());
    final List<Address> addresses = notifierFacility.getAddress();
    assertEquals(1, addresses.size());
    final Address address = addresses.get(0);
    checkAddress(address, notifierFacilityInfo.getAddress());

    final PractitionerInfo contactInfo = notifierFacilityInfo.getContact();
    assertTrue(notifierFacility.hasContact());
    final List<OrganizationContactComponent> contactList = notifierFacility.getContact();
    assertEquals(1, contactList.size());
    final HumanName contactName = contactList.get(0).getName();
    assertEquals("Frau Dr. Melder Melderson", contactName.getText());
    assertEquals(contactInfo.getLastname(), contactName.getFamily());
    assertEquals(contactInfo.getFirstname(), contactName.getGiven().get(0).asStringValue());
    assertEquals(contactInfo.getPrefix(), contactName.getPrefix().get(0).asStringValue());
  }

  private void checkNotifierRole(
      BundleEntryComponent notifierRoleEntry, String notifierFacilityShortUrl) {
    assertTrue(notifierRoleEntry.hasResource());
    final PractitionerRole notifierRole = (PractitionerRole) notifierRoleEntry.getResource();

    assertTrue(notifierRole.hasId());

    assertTrue(notifierRole.hasMeta());
    final Meta meta = notifierRole.getMeta();
    assertTrue(meta.hasProfile());
    assertEquals(FhirConstants.PROFILE_NOTIFIER_ROLE, meta.getProfile().get(0).asStringValue());

    assertTrue(notifierRole.hasOrganization());
    final Reference organization = notifierRole.getOrganization();
    assertTrue(organization.hasReference());
    assertEquals(notifierFacilityShortUrl, organization.getReference());
  }

  private void checkNotifiedPerson(
      BundleEntryComponent notifiedPersonEntry, NotifiedPerson notifiedPersonInfo) {
    final NotifiedPersonBasicInfo notifiedPersonBasicInfo = notifiedPersonInfo.getInfo();

    assertTrue(notifiedPersonEntry.hasResource());
    final Patient notifiedPerson = (Patient) notifiedPersonEntry.getResource();

    assertTrue(notifiedPerson.hasId());

    assertTrue(notifiedPerson.hasMeta());
    final Meta meta = notifiedPerson.getMeta();
    assertTrue(meta.hasProfile());
    assertEquals(FhirConstants.PROFILE_NOTIFIED_PERSON, meta.getProfile().get(0).asStringValue());

    assertTrue(notifiedPerson.hasName());
    final List<HumanName> names = notifiedPerson.getName();
    assertEquals(1, names.size());
    final HumanName name = names.get(0);
    assertEquals(NameUse.OFFICIAL, name.getUse());
    assertEquals(notifiedPersonBasicInfo.getLastname(), name.getFamily());
    assertEquals(notifiedPersonBasicInfo.getFirstname(), name.getGiven().get(0).asStringValue());

    assertTrue(notifiedPerson.hasTelecom());
    final List<ContactPoint> telecomList = notifiedPerson.getTelecom();
    assertEquals(1, telecomList.size());
    final ContactPoint contactPoint = telecomList.get(0);
    final ContactPointInfo contactPointInfo = notifiedPersonInfo.getContacts().get(0);
    assertEquals(contactPointInfo.getContactType().getValue(), contactPoint.getSystem().toCode());
    assertEquals(contactPointInfo.getValue(), contactPoint.getValue());
    assertEquals(contactPointInfo.getUsage().getValue(), contactPoint.getUse().toCode());

    assertTrue(notifiedPerson.hasGender());
    assertEquals(
        notifiedPersonBasicInfo.getGender().getValue(), notifiedPerson.getGender().toString());

    assertTrue(notifiedPerson.hasBirthDate());
    final LocalDate birthdate =
        LocalDate.ofInstant(notifiedPerson.getBirthDate().toInstant(), ZoneId.systemDefault());
    assertEquals(notifiedPersonBasicInfo.getBirthDate(), birthdate);

    assertTrue(notifiedPerson.hasAddress());
    final List<Address> addresses = notifiedPerson.getAddress();
    assertEquals(1, addresses.size());
    final Address address = addresses.get(0);
    checkAddress(address, notifiedPersonInfo.getCurrentAddress());
  }

  private void checkNotificationLaboratory(
      BundleEntryComponent notificationLaboratoryEntry,
      @NotNull @Valid Diagnosis diagnosticInfo,
      String notifiedPersonShortUrl,
      String notifierRoleShortUrl,
      String laboratoryReportShortUrl) {
    assertTrue(notificationLaboratoryEntry.hasResource());
    final Composition notificationLaboratory =
        (Composition) notificationLaboratoryEntry.getResource();
    assertTrue(notificationLaboratory.hasId());

    assertTrue(notificationLaboratory.hasMeta());
    final Meta meta = notificationLaboratory.getMeta();
    assertTrue(meta.hasProfile());
    assertEquals(
        FhirConstants.PROFILE_NOTIFICATION_LABORATORY, meta.getProfile().get(0).asStringValue());

    assertTrue(notificationLaboratory.hasIdentifier());
    final Identifier identifier = notificationLaboratory.getIdentifier();
    assertEquals(FhirConstants.NAMING_SYSTEM_NOTIFICATION_ID, identifier.getSystem());

    assertTrue(notificationLaboratory.hasStatus());
    assertEquals(CompositionStatus.FINAL, notificationLaboratory.getStatus());

    assertTrue(notificationLaboratory.hasType());
    final List<Coding> typeCodings = notificationLaboratory.getType().getCoding();
    assertEquals(1, typeCodings.size());
    final Coding typeCoding = typeCodings.get(0);
    assertEquals(SYSTEM_LOINC, typeCoding.getSystem());
    assertEquals("34782-3", typeCoding.getCode());
    assertEquals("Infectious disease Note", typeCoding.getDisplay());

    assertTrue(notificationLaboratory.hasCategory());
    final List<CodeableConcept> categoryList = notificationLaboratory.getCategory();
    assertEquals(1, categoryList.size());
    final List<Coding> categoryCodings = categoryList.get(0).getCoding();
    assertEquals(1, categoryCodings.size());
    final Coding categoryCoding = categoryCodings.get(0);
    assertEquals(SYSTEM_LOINC, categoryCoding.getSystem());
    assertEquals("11502-2", categoryCoding.getCode());
    assertEquals("Laboratory report", categoryCoding.getDisplay());

    assertTrue(notificationLaboratory.hasSubject());
    final Reference subject = notificationLaboratory.getSubject();
    assertTrue(subject.hasReference());
    assertEquals(notifiedPersonShortUrl, subject.getReference());

    assertTrue(notificationLaboratory.hasDate());
    final Date date = notificationLaboratory.getDate();
    assertEquals(diagnosticInfo.getReceivedDate().toInstant(), date.toInstant());

    assertTrue(notificationLaboratory.hasAuthor());
    final List<Reference> authors = notificationLaboratory.getAuthor();
    assertEquals(1, authors.size());
    final Reference author = authors.get(0);
    assertTrue(author.hasReference());
    assertEquals(notifierRoleShortUrl, author.getReference());

    assertTrue(notificationLaboratory.hasTitle());
    assertEquals("Erregernachweismeldung", notificationLaboratory.getTitle());

    assertTrue(notificationLaboratory.hasSection());
    final List<SectionComponent> sections = notificationLaboratory.getSection();
    assertEquals(1, sections.size());
    final SectionComponent sectionComponent = sections.get(0);
    assertTrue(sectionComponent.hasCode());
    final List<Coding> codeCodings = sectionComponent.getCode().getCoding();
    assertEquals(1, codeCodings.size());
    final Coding codeCoding = codeCodings.get(0);
    assertEquals(SYSTEM_LOINC, codeCoding.getSystem());
    assertEquals("11502-2", codeCoding.getCode());
    assertEquals("Laboratory report", codeCoding.getDisplay());
    assertTrue(sectionComponent.hasEntry());
    final List<Reference> entries = sectionComponent.getEntry();
    assertEquals(1, entries.size());
    final Reference entry = entries.get(0);
    assertTrue(entry.hasReference());
    assertEquals(laboratoryReportShortUrl, entry.getReference());
  }

  private void checkAddress(Address addressToCheck, NotifiedPersonAddressInfo addressInfo) {
    assertTrue(addressToCheck.hasExtension());
    final List<Extension> addressExtensions = addressToCheck.getExtension();
    assertEquals(1, addressExtensions.size());
    final Extension addressExtension = addressExtensions.get(0);
    assertEquals(FhirConstants.STRUCTURE_DEFINITION_ADDRESS_USE, addressExtension.getUrl());
    final Coding value = (Coding) addressExtension.getValue();
    assertEquals(FhirConstants.CODE_SYSTEM_ADDRESS_USE, value.getSystem());
    assertEquals(addressInfo.getAddressType().getValue(), value.getCode());
    assertEquals("Derzeitiger Aufenthaltsort", value.getDisplay());

    assertTrue(addressToCheck.hasLine());
    final StringType addressLine = addressToCheck.getLine().get(0);
    assertEquals(
        addressInfo.getStreet() + " " + addressInfo.getHouseNumber(), addressLine.asStringValue());
    assertTrue(addressLine.hasExtension());
    final List<Extension> addressLineExtensions = addressLine.getExtension();
    assertEquals(2, addressLineExtensions.size());
    final Extension streetNameExtension = addressLineExtensions.get(0);
    assertEquals(FhirConstants.STRUCTURE_DEFINITION_ADXP_STREET_NAME, streetNameExtension.getUrl());
    assertEquals(addressInfo.getStreet(), streetNameExtension.getValue().toString());
    final Extension houseNumberExtension = addressLineExtensions.get(1);
    assertEquals(
        FhirConstants.STRUCTURE_DEFINITION_ADXP_HOUSE_NUMBER, houseNumberExtension.getUrl());
    assertEquals(addressInfo.getHouseNumber(), houseNumberExtension.getValue().toString());

    assertTrue(addressToCheck.hasCity());
    assertEquals(addressInfo.getCity(), addressToCheck.getCity());

    assertTrue(addressToCheck.hasCountry());
    assertEquals(addressInfo.getCountry(), addressToCheck.getCountry());

    assertTrue(addressToCheck.hasPostalCode());
    assertEquals(addressInfo.getZip(), addressToCheck.getPostalCode());
  }

  private void checkAddress(Address addressToCheck, FacilityAddressInfo addressInfo) {
    assertFalse(addressToCheck.hasExtension());
    assertTrue(addressToCheck.hasLine());
    final StringType addressLine = addressToCheck.getLine().get(0);
    assertEquals(
        addressInfo.getStreet() + " " + addressInfo.getHouseNumber(), addressLine.asStringValue());
    assertTrue(addressLine.hasExtension());
    final List<Extension> addressLineExtensions = addressLine.getExtension();
    assertEquals(2, addressLineExtensions.size());
    final Extension streetNameExtension = addressLineExtensions.get(0);
    assertEquals(FhirConstants.STRUCTURE_DEFINITION_ADXP_STREET_NAME, streetNameExtension.getUrl());
    assertEquals(addressInfo.getStreet(), streetNameExtension.getValue().toString());
    final Extension houseNumberExtension = addressLineExtensions.get(1);
    assertEquals(
        FhirConstants.STRUCTURE_DEFINITION_ADXP_HOUSE_NUMBER, houseNumberExtension.getUrl());
    assertEquals(addressInfo.getHouseNumber(), houseNumberExtension.getValue().toString());

    assertTrue(addressToCheck.hasCity());
    assertEquals(addressInfo.getCity(), addressToCheck.getCity());

    assertTrue(addressToCheck.hasCountry());
    assertEquals(addressInfo.getCountry(), addressToCheck.getCountry());

    assertTrue(addressToCheck.hasPostalCode());
    assertEquals(addressInfo.getZip(), addressToCheck.getPostalCode());
  }
}
