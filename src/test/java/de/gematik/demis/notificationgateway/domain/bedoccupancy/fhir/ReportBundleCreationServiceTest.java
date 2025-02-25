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

package de.gematik.demis.notificationgateway.domain.bedoccupancy.fhir;

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

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.CODE_SYSTEM_REPORT_CATEGORY;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.CODE_SYSTEM_REPORT_SECTION;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.NAMING_SYSTEM_INEK_STANDORT_ID;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.NAMING_SYSTEM_NOTIFICATION_ID;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.PROFILE_REPORT_BED_OCCUPANCY;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.PROFILE_STATISTIC_INFORMATION_BED_OCCUPANCY;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.QUESTIONAIRE_STATISTIC_QUESTIONS_BED_OCCUPANCY;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.SYSTEM_LOINC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.gematik.demis.notificationgateway.common.constants.FhirConstants;
import de.gematik.demis.notificationgateway.common.dto.BedOccupancy;
import de.gematik.demis.notificationgateway.common.dto.FacilityAddressInfo;
import de.gematik.demis.notificationgateway.utils.FileUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
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
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Organization.OrganizationContactComponent;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseStatus;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"feature.flag.specimen.preparation.enabled=false"})
class ReportBundleCreationServiceTest {

  @Autowired private ReportBundleCreationService reportBundleCreationService;

  @Test
  void testCreateBundleWithMaxContent() throws JsonProcessingException {
    final BedOccupancy bedOccupancy =
        FileUtils.createBedOccupancy("portal/bedoccupancy/report_content_max.json");

    final Bundle bundle = reportBundleCreationService.createReportBundle(bedOccupancy);
    assertNotNull(bundle);

    assertTrue(bundle.hasMeta());
    final Meta meta = bundle.getMeta();
    assertTrue(meta.hasLastUpdated());
    assertEquals(
        LocalDate.now(),
        LocalDate.ofInstant(meta.getLastUpdated().toInstant(), ZoneId.systemDefault()));
    assertTrue(meta.hasProfile());
    assertEquals(FhirConstants.PROFILE_REPORT_BUNLDE, meta.getProfile().get(0).asStringValue());

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
    assertEquals(4, entryList.size());

    checkEntries(entryList, bedOccupancy);
  }

  @Test
  void testCreateBundleWithMinContent() throws JsonProcessingException {
    final BedOccupancy bedOccupancy =
        FileUtils.createBedOccupancy("portal/bedoccupancy/report_content_min.json");

    final Bundle bundle = reportBundleCreationService.createReportBundle(bedOccupancy);
    assertNotNull(bundle);

    assertTrue(bundle.hasEntry());
    final List<BundleEntryComponent> entryList = bundle.getEntry();
    assertEquals(4, entryList.size());

    final BundleEntryComponent statisticInformationBedOccupancyEntry = entryList.get(3);
    assertTrue(statisticInformationBedOccupancyEntry.hasResource());
    final QuestionnaireResponse statisticInformationBedOccupancy =
        (QuestionnaireResponse) statisticInformationBedOccupancyEntry.getResource();

    final List<QuestionnaireResponseItemComponent> items =
        statisticInformationBedOccupancy.getItem();
    assertEquals(2, items.size());

    final QuestionnaireResponseItemComponent numberOccupiedBedsGeneralWardAdultsItem = items.get(0);
    assertEquals(
        "numberOccupiedBedsGeneralWardAdults", numberOccupiedBedsGeneralWardAdultsItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent>
        numberOccupiedBedsGeneralWardAdultsAnswerList =
            numberOccupiedBedsGeneralWardAdultsItem.getAnswer();
    assertEquals(1, numberOccupiedBedsGeneralWardAdultsAnswerList.size());
    final QuestionnaireResponseItemAnswerComponent numberOccupiedBedsGeneralWardAdultsAnswer =
        numberOccupiedBedsGeneralWardAdultsAnswerList.get(0);
    assertEquals(
        3, ((IntegerType) numberOccupiedBedsGeneralWardAdultsAnswer.getValue()).getValue());

    final QuestionnaireResponseItemComponent numberOccupiedBedsGeneralWardChildrenItem =
        items.get(1);
    assertEquals(
        "numberOccupiedBedsGeneralWardChildren",
        numberOccupiedBedsGeneralWardChildrenItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent>
        numberOccupiedBedsGeneralWardChildrenAnswerList =
            numberOccupiedBedsGeneralWardChildrenItem.getAnswer();
    assertEquals(1, numberOccupiedBedsGeneralWardChildrenAnswerList.size());
    final QuestionnaireResponseItemAnswerComponent numberOccupiedBedsGeneralWardChildrenAnswer =
        numberOccupiedBedsGeneralWardChildrenAnswerList.get(0);
    assertEquals(
        0, ((IntegerType) numberOccupiedBedsGeneralWardChildrenAnswer.getValue()).getValue());
  }

  private void checkEntries(List<BundleEntryComponent> entryList, BedOccupancy bedOccupancy) {
    final BundleEntryComponent reportBedOccupancyEntry = entryList.get(0);
    assertTrue(reportBedOccupancyEntry.hasFullUrl());

    final BundleEntryComponent notifierRoleEntry = entryList.get(1);
    assertTrue(notifierRoleEntry.hasFullUrl());

    final BundleEntryComponent notifierFacilityEntry = entryList.get(2);
    assertTrue(notifierFacilityEntry.hasFullUrl());

    final BundleEntryComponent statisticInformationBedOccupancyEntry = entryList.get(3);
    assertTrue(statisticInformationBedOccupancyEntry.hasFullUrl());

    checkReportBedOccupancy(
        reportBedOccupancyEntry,
        notifierRoleEntry.getResource(),
        statisticInformationBedOccupancyEntry.getResource());
    checkNotifierFacility(notifierFacilityEntry, bedOccupancy.getNotifierFacility().getAddress());
    checkNotifierRole(notifierRoleEntry, notifierFacilityEntry.getResource());
    checkStatisticInformationBedOccupancy(statisticInformationBedOccupancyEntry);
  }

  private void checkReportBedOccupancy(
      BundleEntryComponent reportBedOccupancyEntry,
      Resource notifierRole,
      Resource statisticInformationBedOccupancy) {
    assertTrue(reportBedOccupancyEntry.hasResource());
    final Composition reportBedOccupancy = (Composition) reportBedOccupancyEntry.getResource();
    assertTrue(reportBedOccupancy.hasId());

    assertTrue(reportBedOccupancy.hasMeta());
    final Meta meta = reportBedOccupancy.getMeta();
    assertTrue(meta.hasProfile());
    assertEquals(PROFILE_REPORT_BED_OCCUPANCY, meta.getProfile().get(0).asStringValue());

    assertTrue(reportBedOccupancy.hasIdentifier());
    final Identifier identifier = reportBedOccupancy.getIdentifier();
    assertEquals(NAMING_SYSTEM_NOTIFICATION_ID, identifier.getSystem());

    assertEquals(CompositionStatus.FINAL, reportBedOccupancy.getStatus());

    assertTrue(reportBedOccupancy.hasType());
    final List<Coding> typeCodings = reportBedOccupancy.getType().getCoding();
    assertEquals(1, typeCodings.size());
    final Coding typeCoding = typeCodings.get(0);
    assertEquals(SYSTEM_LOINC, typeCoding.getSystem());
    assertEquals("80563-0", typeCoding.getCode());
    assertEquals("Report", typeCoding.getDisplay());

    final List<CodeableConcept> categoryList = reportBedOccupancy.getCategory();
    assertEquals(1, categoryList.size());
    final List<Coding> categoryCodings = categoryList.get(0).getCoding();
    assertEquals(1, categoryCodings.size());
    final Coding categoryCoding = categoryCodings.get(0);
    assertEquals(CODE_SYSTEM_REPORT_CATEGORY, categoryCoding.getSystem());
    assertEquals("bedOccupancyReport", categoryCoding.getCode());
    assertEquals("Bettenbelegungsstatistik", categoryCoding.getDisplay());

    assertTrue(reportBedOccupancy.hasSubject());
    final Reference subject = reportBedOccupancy.getSubject();
    assertTrue(subject.hasIdentifier());
    final Identifier subjectIdentifier = subject.getIdentifier();
    assertEquals(NAMING_SYSTEM_INEK_STANDORT_ID, subjectIdentifier.getSystem());
    assertEquals("123456", subjectIdentifier.getValue());

    assertTrue(reportBedOccupancy.hasDate());

    final List<Reference> authors = reportBedOccupancy.getAuthor();
    assertEquals(1, authors.size());
    IBaseResource actualNotifierRole = authors.getFirst().getResource();
    assertEquals(
        notifierRole.getId(),
        actualNotifierRole.getIdElement().getValue(),
        "notifier role ID is not equal");
    assertEquals(notifierRole, actualNotifierRole, "Author is not the notifier role");

    assertEquals("Bericht (Krankenhausbettenbelegungsstatistik)", reportBedOccupancy.getTitle());

    final List<SectionComponent> sections = reportBedOccupancy.getSection();
    assertEquals(1, sections.size());

    final SectionComponent reportSection = sections.get(0);
    assertTrue(reportSection.hasCode());
    final List<Coding> reportSectionCodings = reportSection.getCode().getCoding();
    assertEquals(1, reportSectionCodings.size());
    final Coding reportSectionCoding = reportSectionCodings.get(0);
    assertEquals(CODE_SYSTEM_REPORT_SECTION, reportSectionCoding.getSystem());
    assertEquals("statisticInformationBedOccupancySection", reportSectionCoding.getCode());
    assertEquals(
        "Abschnitt 'Statistische Informationen zur Krankenhausbettenbelegung'",
        reportSectionCoding.getDisplay());
    final List<Reference> reportEntries = reportSection.getEntry();
    assertEquals(1, reportEntries.size());
    final Reference reportEntry = reportEntries.get(0);
    assertEquals(statisticInformationBedOccupancy, reportEntry.getResource());
  }

  private void checkNotifierFacility(
      BundleEntryComponent notifierFacilityEntry,
      @NotNull @Valid FacilityAddressInfo facilityAddressInfo) {
    assertTrue(notifierFacilityEntry.hasResource());
    final Organization notifierFacility = (Organization) notifierFacilityEntry.getResource();

    assertTrue(notifierFacility.hasId());

    assertTrue(notifierFacility.hasMeta());
    final Meta meta = notifierFacility.getMeta();
    assertTrue(meta.hasProfile());
    assertEquals(FhirConstants.PROFILE_NOTIFIER_FACILITY, meta.getProfile().get(0).asStringValue());

    assertTrue(notifierFacility.hasType());
    final List<CodeableConcept> types = notifierFacility.getType();
    assertEquals(1, types.size());
    final List<Coding> typeCodings = types.get(0).getCoding();
    assertEquals(1, typeCodings.size());
    final Coding typeCoding = typeCodings.get(0);
    assertEquals(FhirConstants.CODE_SYSTEM_ORGANIZATION_TYPE, typeCoding.getSystem());
    assertEquals("hospital", typeCoding.getCode());
    assertEquals("Krankenhaus", typeCoding.getDisplay());

    assertTrue(notifierFacility.hasName());
    assertEquals("TEST Organisation", notifierFacility.getName());

    assertTrue(notifierFacility.hasTelecom());
    final List<ContactPoint> telecomList = notifierFacility.getTelecom();
    assertEquals(2, telecomList.size());
    final ContactPoint phone = telecomList.get(0);
    assertEquals("phone", phone.getSystem().toCode());
    assertEquals("01234567", phone.getValue());
    final ContactPoint email = telecomList.get(1);
    assertEquals("email", email.getSystem().toCode());
    assertEquals("anna@ansprechpartner.de", email.getValue());

    assertTrue(notifierFacility.hasAddress());
    final List<Address> addresses = notifierFacility.getAddress();
    assertEquals(1, addresses.size());
    final Address address = addresses.get(0);
    checkAddress(address, facilityAddressInfo);

    assertTrue(notifierFacility.hasContact());
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

  private void checkNotifierRole(
      BundleEntryComponent notifierRoleEntry, Resource notifierFacility) {
    assertTrue(notifierRoleEntry.hasResource());
    final PractitionerRole notifierRole = (PractitionerRole) notifierRoleEntry.getResource();

    assertTrue(notifierRole.hasId());

    assertTrue(notifierRole.hasMeta());
    final Meta meta = notifierRole.getMeta();
    assertTrue(meta.hasProfile());
    assertEquals(FhirConstants.PROFILE_NOTIFIER_ROLE, meta.getProfile().get(0).asStringValue());

    assertTrue(notifierRole.hasOrganization());
    final Reference organization = notifierRole.getOrganization();
    assertEquals(notifierFacility, organization.getResource());
  }

  private void checkStatisticInformationBedOccupancy(
      BundleEntryComponent statisticInformationBedOccupancyEntry) {
    assertTrue(statisticInformationBedOccupancyEntry.hasResource());
    final QuestionnaireResponse statisticInformationBedOccupancy =
        (QuestionnaireResponse) statisticInformationBedOccupancyEntry.getResource();
    assertTrue(statisticInformationBedOccupancy.hasId());

    assertTrue(statisticInformationBedOccupancy.hasMeta());
    final Meta meta = statisticInformationBedOccupancy.getMeta();
    assertTrue(meta.hasProfile());
    assertEquals(
        PROFILE_STATISTIC_INFORMATION_BED_OCCUPANCY, meta.getProfile().get(0).asStringValue());

    assertEquals(
        QUESTIONAIRE_STATISTIC_QUESTIONS_BED_OCCUPANCY,
        statisticInformationBedOccupancy.getQuestionnaire());
    assertEquals(
        QuestionnaireResponseStatus.COMPLETED, statisticInformationBedOccupancy.getStatus());

    final List<QuestionnaireResponseItemComponent> items =
        statisticInformationBedOccupancy.getItem();
    assertEquals(4, items.size());

    final QuestionnaireResponseItemComponent numberOperableBedsGeneralWardAdultsItem = items.get(0);
    assertEquals(
        "numberOperableBedsGeneralWardAdults", numberOperableBedsGeneralWardAdultsItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent>
        numberOperableBedsGeneralWardAdultsAnswerList =
            numberOperableBedsGeneralWardAdultsItem.getAnswer();
    assertEquals(1, numberOperableBedsGeneralWardAdultsAnswerList.size());
    final QuestionnaireResponseItemAnswerComponent numberOperableBedsGeneralWardAdultsAnswer =
        numberOperableBedsGeneralWardAdultsAnswerList.get(0);
    assertEquals(
        30, ((IntegerType) numberOperableBedsGeneralWardAdultsAnswer.getValue()).getValue());

    final QuestionnaireResponseItemComponent numberOccupiedBedsGeneralWardAdultsItem = items.get(1);
    assertEquals(
        "numberOccupiedBedsGeneralWardAdults", numberOccupiedBedsGeneralWardAdultsItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent>
        numberOccupiedBedsGeneralWardAdultsAnswerList =
            numberOccupiedBedsGeneralWardAdultsItem.getAnswer();
    assertEquals(1, numberOccupiedBedsGeneralWardAdultsAnswerList.size());
    final QuestionnaireResponseItemAnswerComponent numberOccupiedBedsGeneralWardAdultsAnswer =
        numberOccupiedBedsGeneralWardAdultsAnswerList.get(0);
    assertEquals(
        22, ((IntegerType) numberOccupiedBedsGeneralWardAdultsAnswer.getValue()).getValue());

    final QuestionnaireResponseItemComponent numberOperableBedsGeneralWardChildrenItem =
        items.get(2);
    assertEquals(
        "numberOperableBedsGeneralWardChildren",
        numberOperableBedsGeneralWardChildrenItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent>
        numberOperableBedsGeneralWardChildrenAnswerList =
            numberOperableBedsGeneralWardChildrenItem.getAnswer();
    assertEquals(1, numberOperableBedsGeneralWardChildrenAnswerList.size());
    final QuestionnaireResponseItemAnswerComponent numberOperableBedsGeneralWardChildrenAnswer =
        numberOperableBedsGeneralWardChildrenAnswerList.get(0);
    assertEquals(
        5, ((IntegerType) numberOperableBedsGeneralWardChildrenAnswer.getValue()).getValue());

    final QuestionnaireResponseItemComponent numberOccupiedBedsGeneralWardChildrenItem =
        items.get(3);
    assertEquals(
        "numberOccupiedBedsGeneralWardChildren",
        numberOccupiedBedsGeneralWardChildrenItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent>
        numberOccupiedBedsGeneralWardChildrenAnswerList =
            numberOccupiedBedsGeneralWardChildrenItem.getAnswer();
    assertEquals(1, numberOccupiedBedsGeneralWardChildrenAnswerList.size());
    final QuestionnaireResponseItemAnswerComponent numberOccupiedBedsGeneralWardChildrenAnswer =
        numberOccupiedBedsGeneralWardChildrenAnswerList.get(0);
    assertEquals(
        2, ((IntegerType) numberOccupiedBedsGeneralWardChildrenAnswer.getValue()).getValue());
  }

  private void checkAddress(Address addressToCheck, FacilityAddressInfo addressInfo) {
    assertTrue(addressToCheck.hasLine());
    final StringType addressLine = addressToCheck.getLine().get(0);
    assertEquals(
        addressInfo.getStreet() + " " + addressInfo.getHouseNumber(), addressLine.asStringValue());

    assertTrue(addressToCheck.hasCity());
    assertEquals(addressInfo.getCity(), addressToCheck.getCity());

    assertTrue(addressToCheck.hasCountry());
    assertEquals(addressInfo.getCountry(), addressToCheck.getCountry());

    assertTrue(addressToCheck.hasPostalCode());
    assertEquals(addressInfo.getZip(), addressToCheck.getPostalCode());
  }
}
