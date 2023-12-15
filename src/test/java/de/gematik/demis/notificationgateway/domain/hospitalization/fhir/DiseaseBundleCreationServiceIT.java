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

package de.gematik.demis.notificationgateway.domain.hospitalization.fhir;

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.CODE_SYSTEM_ACT_CODE;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.CODE_SYSTEM_CONDITION_VERIFICATION_STATUS;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.CODE_SYSTEM_GEOGRAPHIC_REGION;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.CODE_SYSTEM_HOSPITALIZATION_SERVICE_TYPE;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.CODE_SYSTEM_INFECTION_ENVIRONMENT_SETTING;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.CODE_SYSTEM_MILITARY_AFFILIATION;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.CODE_SYSTEM_NOTIFICATION_DISEASE_CATEGORY;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.CODE_SYSTEM_NOTIFICATION_TYPE;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.CODE_SYSTEM_NULL_FLAVOR;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.CODE_SYSTEM_ORGANIZATION_ASSOCIATION;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.CODE_SYSTEM_SECTION_CODE;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.CODE_SYSTEM_VACCINE;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.CODE_SYSTEM_YES_OR_NO_ANSWER;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.COMMUNITY_REGISTER;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.NAMING_SYSTEM_NOTIFICATION_ID;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.PROFILE_DISEASE_INFORMATION_COMMON;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.PROFILE_DISEASE_INFORMATION_CVDD;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.PROFILE_HOSPITALIZATION;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.PROFILE_IMMUNIZATION_INFORMATION_CVDD;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.PROFILE_NOTIFICATION_DISEASE;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.QUESTIONAIRE_DISEASE_QUESTIONS_COMMON;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.QUESTIONAIRE_DISEASE_QUESTIONS_CVDD;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.STRUCTURE_DEFINITION_HOSPITALIZATION_NOTE;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.SYSTEM_LOINC;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.SYSTEM_SNOMED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import de.gematik.demis.notificationgateway.common.constants.FhirConstants;
import de.gematik.demis.notificationgateway.common.dto.FacilityAddressInfo;
import de.gematik.demis.notificationgateway.common.dto.Hospitalization;
import de.gematik.demis.notificationgateway.common.dto.InfectionProtectionFacilityInfo;
import de.gematik.demis.notificationgateway.common.dto.LabInfo;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPerson;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPersonAddressInfo;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPersonBasicInfo;
import de.gematik.demis.notificationgateway.common.exceptions.BadRequestException;
import de.gematik.demis.notificationgateway.common.utils.DateUtils;
import de.gematik.demis.notificationgateway.utils.FileUtils;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Composition.CompositionStatus;
import org.hl7.fhir.r4.model.Composition.SectionComponent;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Condition.ConditionEvidenceComponent;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Encounter.EncounterStatus;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.HumanName.NameUse;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Immunization.ImmunizationStatus;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Organization.OrganizationContactComponent;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseStatus;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DiseaseBundleCreationServiceIT {

  @Autowired private DiseaseBundleCreationService diseaseBundleCreationService;

  @Test
  void testCreateBundle() throws JsonProcessingException, BadRequestException {
    Hospitalization hospitalization =
        FileUtils.createHospitalization("portal/disease/notification_content_max.json");

    final Bundle bundle = diseaseBundleCreationService.createDiseaseBundle(hospitalization);
    assertNotNull(bundle);

    assertTrue(bundle.hasMeta());
    final Meta meta = bundle.getMeta();
    assertTrue(meta.hasLastUpdated());
    assertTrue(meta.hasProfile());
    assertEquals(
        LocalDate.now(),
        LocalDate.ofInstant(meta.getLastUpdated().toInstant(), ZoneId.systemDefault()));
    assertEquals(
        FhirConstants.PROFILE_NOTIFICATION_BUNDLE_DISEASE,
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
    assertEquals(16, entryList.size());

    checkEntries(entryList, hospitalization);
  }

  @Test
  void testCreateBundleWithMinimumInput() throws JsonProcessingException, BadRequestException {
    Hospitalization hospitalization =
        FileUtils.createHospitalization("portal/disease/notification_content_min.json");

    final Bundle bundle = diseaseBundleCreationService.createDiseaseBundle(hospitalization);
    assertNotNull(bundle);

    assertTrue(bundle.hasEntry());
    final List<BundleEntryComponent> entryList = bundle.getEntry();
    assertEquals(7, entryList.size());
  }

  @Test
  void testCreateBundleWithMinimumInputButInconsistentAdditionalData()
      throws JsonProcessingException, BadRequestException {
    Hospitalization hospitalization =
        FileUtils.createHospitalization(
            "portal/disease/notification_content_min_inconsistent.json");

    final Bundle bundle = diseaseBundleCreationService.createDiseaseBundle(hospitalization);
    assertNotNull(bundle);

    assertTrue(bundle.hasEntry());
    final List<BundleEntryComponent> entryList = bundle.getEntry();
    assertEquals(7, entryList.size());

    final BundleEntryComponent notificationDiseaseEntry = entryList.get(0);
    assertThat(notificationDiseaseEntry.getResource()).isInstanceOf(Composition.class);

    final BundleEntryComponent notifiedPersonEntry = entryList.get(1);
    assertThat(notifiedPersonEntry.getResource()).isInstanceOf(Patient.class);

    final BundleEntryComponent diseaseCVDDEntry = entryList.get(2);
    assertThat(diseaseCVDDEntry.getResource()).isInstanceOf(Condition.class);

    final BundleEntryComponent notifierFacilityEntry = entryList.get(3);
    assertThat(notifierFacilityEntry.getResource()).isInstanceOf(Organization.class);

    final BundleEntryComponent notifierRoleEntry = entryList.get(4);
    assertThat(notifierRoleEntry.getResource()).isInstanceOf(PractitionerRole.class);

    final BundleEntryComponent diseaseInformationCommonEntry = entryList.get(5);
    assertThat(diseaseInformationCommonEntry.getResource())
        .isInstanceOf(QuestionnaireResponse.class);

    final BundleEntryComponent diseaseInformationCVDDEntry = entryList.get(6);
    assertThat(diseaseInformationCVDDEntry.getResource()).isInstanceOf(QuestionnaireResponse.class);

    final QuestionnaireResponse diseaseInformationCommon =
        (QuestionnaireResponse) diseaseInformationCommonEntry.getResource();

    final List<QuestionnaireResponseItemComponent> items = diseaseInformationCommon.getItem();
    assertEquals(7, items.size());

    QuestionnaireResponseItemComponent labSpecimenItem = items.get(2);
    assertFalse(labSpecimenItem.getAnswer().get(0).hasItem());

    QuestionnaireResponseItemComponent hospitalizedItem = items.get(3);
    assertFalse(hospitalizedItem.getAnswer().get(0).hasItem());

    QuestionnaireResponseItemComponent infectProtectFacilityItem = items.get(4);
    assertFalse(infectProtectFacilityItem.getAnswer().get(0).hasItem());

    QuestionnaireResponseItemComponent placeExposureItem = items.get(5);
    assertFalse(placeExposureItem.getAnswer().get(0).hasItem());

    final QuestionnaireResponse diseaseInformationCVDD =
        (QuestionnaireResponse) diseaseInformationCVDDEntry.getResource();

    QuestionnaireResponseItemComponent infectionEnvironmentItem =
        diseaseInformationCVDD.getItem().get(1);
    assertFalse(infectionEnvironmentItem.getAnswer().get(0).hasItem());
  }

  @Test
  void testCreateBundleWithValidNullInputValues()
      throws JsonProcessingException, BadRequestException {
    Hospitalization hospitalization =
        FileUtils.createHospitalization(
            "portal/disease/notification_content_valid_null_values.json");

    final Bundle bundle = diseaseBundleCreationService.createDiseaseBundle(hospitalization);
    assertNotNull(bundle);

    assertTrue(bundle.hasEntry());
    final List<BundleEntryComponent> entryList = bundle.getEntry();
    assertEquals(10, entryList.size());
  }

  private void checkEntries(List<BundleEntryComponent> entryList, Hospitalization hospitalization) {
    final BundleEntryComponent notificationDiseaseEntry = entryList.get(0);
    assertTrue(notificationDiseaseEntry.hasFullUrl());

    final BundleEntryComponent notifiedPersonFacilityEntry = entryList.get(1);
    assertTrue(notifiedPersonFacilityEntry.hasFullUrl());
    final String notifiedPersonFacilityShortUrl =
        notifiedPersonFacilityEntry.getFullUrl().replace(FhirConstants.DEMIS_RKI_DE_FHIR, "");

    final BundleEntryComponent notifiedPersonEntry = entryList.get(2);
    assertTrue(notifiedPersonEntry.hasFullUrl());
    final String notifiedPersonShortUrl =
        notifiedPersonEntry.getFullUrl().replace(FhirConstants.DEMIS_RKI_DE_FHIR, "");

    final BundleEntryComponent diseaseCVDDEntry = entryList.get(3);
    assertTrue(diseaseCVDDEntry.hasFullUrl());
    final String diseaseCVDDShortUrl =
        diseaseCVDDEntry.getFullUrl().replace(FhirConstants.DEMIS_RKI_DE_FHIR, "");

    final BundleEntryComponent notifierFacilityEntry = entryList.get(4);
    assertTrue(notifierFacilityEntry.hasFullUrl());
    final String notifierFacilityShortUrl =
        notifierFacilityEntry.getFullUrl().replace(FhirConstants.DEMIS_RKI_DE_FHIR, "");

    final BundleEntryComponent notifierRoleEntry = entryList.get(5);
    assertTrue(notifierRoleEntry.hasFullUrl());
    final String notifierRoleShortUrl =
        notifierRoleEntry.getFullUrl().replace(FhirConstants.DEMIS_RKI_DE_FHIR, "");

    final BundleEntryComponent hospitalizationEncounterEntry = entryList.get(6);
    assertTrue(hospitalizationEncounterEntry.hasFullUrl());
    final String hospitalizationEncounterShortUrl =
        hospitalizationEncounterEntry.getFullUrl().replace(FhirConstants.DEMIS_RKI_DE_FHIR, "");

    final BundleEntryComponent intensiveCareEncounterEntry = entryList.get(7);
    assertTrue(intensiveCareEncounterEntry.hasFullUrl());
    final String intensiveCareEncounterShortUrl =
        intensiveCareEncounterEntry.getFullUrl().replace(FhirConstants.DEMIS_RKI_DE_FHIR, "");

    final BundleEntryComponent diseaseInformationCommonEntry = entryList.get(8);
    assertTrue(diseaseInformationCommonEntry.hasFullUrl());
    final String diseaseInformationCommonShortUrl =
        diseaseInformationCommonEntry.getFullUrl().replace(FhirConstants.DEMIS_RKI_DE_FHIR, "");

    final BundleEntryComponent labEntry = entryList.get(9);
    assertTrue(labEntry.hasFullUrl());
    final String labShortUrl = labEntry.getFullUrl().replace(FhirConstants.DEMIS_RKI_DE_FHIR, "");

    final BundleEntryComponent infectProtectFacilityEntry = entryList.get(10);
    assertTrue(infectProtectFacilityEntry.hasFullUrl());
    final String infectProtectFacilityShortUrl =
        infectProtectFacilityEntry.getFullUrl().replace(FhirConstants.DEMIS_RKI_DE_FHIR, "");

    final BundleEntryComponent diseaseInformationCVDDEntry = entryList.get(11);
    assertTrue(diseaseInformationCVDDEntry.hasFullUrl());
    final String diseaseInformationCVDDShortUrl =
        diseaseInformationCVDDEntry.getFullUrl().replace(FhirConstants.DEMIS_RKI_DE_FHIR, "");

    final BundleEntryComponent firstImmunizationEntry = entryList.get(12);
    assertTrue(firstImmunizationEntry.hasFullUrl());
    final String firstImmunizationShortUrl =
        firstImmunizationEntry.getFullUrl().replace(FhirConstants.DEMIS_RKI_DE_FHIR, "");

    final BundleEntryComponent secondImmunizationEntry = entryList.get(13);
    assertTrue(secondImmunizationEntry.hasFullUrl());
    final String secondImmunizationShortUrl =
        secondImmunizationEntry.getFullUrl().replace(FhirConstants.DEMIS_RKI_DE_FHIR, "");

    final BundleEntryComponent thirdImmunizationEntry = entryList.get(14);
    assertTrue(thirdImmunizationEntry.hasFullUrl());
    final String thirdImmunizationShortUrl =
        thirdImmunizationEntry.getFullUrl().replace(FhirConstants.DEMIS_RKI_DE_FHIR, "");

    final BundleEntryComponent furtherImmunizationEntry = entryList.get(15);
    assertTrue(furtherImmunizationEntry.hasFullUrl());
    final String furtherImmunizationShortUrl =
        furtherImmunizationEntry.getFullUrl().replace(FhirConstants.DEMIS_RKI_DE_FHIR, "");

    checkNotificationDisease(
        notificationDiseaseEntry,
        notifiedPersonShortUrl,
        notifierRoleShortUrl,
        diseaseCVDDShortUrl,
        diseaseInformationCommonShortUrl,
        diseaseInformationCVDDShortUrl);
    checkNotifiedPersonFacility(
        notifiedPersonFacilityEntry, hospitalization.getNotifierFacility().getAddress());
    checkNotifiedPerson(
        notifiedPersonEntry, hospitalization.getNotifiedPerson(), notifiedPersonFacilityShortUrl);
    checkDiseaseCVDD(diseaseCVDDEntry, notifiedPersonShortUrl);
    checkNotifierFacility(
        notifierFacilityEntry, hospitalization.getNotifierFacility().getAddress());
    checkNotifierRole(notifierRoleEntry, notifierFacilityShortUrl);
    checkHospitalizationEncounter(
        hospitalizationEncounterEntry, notifiedPersonShortUrl, notifiedPersonFacilityShortUrl);
    checkIntensiveCareEncounter(
        intensiveCareEncounterEntry, notifiedPersonShortUrl, notifiedPersonFacilityShortUrl);
    checkDiseaseInformationCommon(
        diseaseInformationCommonEntry,
        notifiedPersonShortUrl,
        hospitalizationEncounterShortUrl,
        intensiveCareEncounterShortUrl,
        labShortUrl,
        infectProtectFacilityShortUrl);
    checkLab(
        labEntry,
        hospitalization.getDisease().getDiseaseInfoCommon().getLabQuestion().getLabInfo());
    checkInfectProtectFacility(
        infectProtectFacilityEntry,
        hospitalization
            .getDisease()
            .getDiseaseInfoCommon()
            .getInfectionProtectionFacilityQuestion()
            .getInfectionProtectionFacilityInfo());
    checkDiseaseInformationCVDD(
        diseaseInformationCVDDEntry,
        notifiedPersonShortUrl,
        firstImmunizationShortUrl,
        secondImmunizationShortUrl,
        thirdImmunizationShortUrl,
        furtherImmunizationShortUrl);
    checkFirstImmunizationCVDD(firstImmunizationEntry, notifiedPersonShortUrl);
    checkSecondImmunizationCVDD(secondImmunizationEntry, notifiedPersonShortUrl);
    checkThirdImmunizationCVDD(thirdImmunizationEntry, notifiedPersonShortUrl);
    checkFurtherImmunizationCVDD(furtherImmunizationEntry, notifiedPersonShortUrl);
  }

  private void checkNotifiedPersonFacility(
      BundleEntryComponent notifiedPersonFacilityEntry,
      FacilityAddressInfo notifiedPersonFacilityAddressInfo) {
    assertTrue(notifiedPersonFacilityEntry.hasResource());
    final Organization notifiedPersonFacility =
        (Organization) notifiedPersonFacilityEntry.getResource();

    assertTrue(notifiedPersonFacility.hasId());

    assertTrue(notifiedPersonFacility.hasMeta());
    final Meta meta = notifiedPersonFacility.getMeta();
    assertTrue(meta.hasProfile());
    assertEquals(
        FhirConstants.PROFILE_NOTIFIED_PERSON_FACILITY, meta.getProfile().get(0).asStringValue());

    assertTrue(notifiedPersonFacility.hasIdentifier());
    final List<Identifier> identifiers = notifiedPersonFacility.getIdentifier();
    assertEquals(1, identifiers.size());
    final Identifier identifier = identifiers.get(0);
    assertEquals(FhirConstants.NAMING_SYSTEM_BSNR, identifier.getSystem());
    assertEquals("123456789", identifier.getValue());

    assertTrue(notifiedPersonFacility.hasType());
    final List<CodeableConcept> types = notifiedPersonFacility.getType();
    assertEquals(1, types.size());
    final List<Coding> typeCodings = types.get(0).getCoding();
    assertEquals(1, typeCodings.size());
    final Coding typeCoding = typeCodings.get(0);
    assertEquals(FhirConstants.CODE_SYSTEM_ORGANIZATION_TYPE, typeCoding.getSystem());
    assertEquals("hospital", typeCoding.getCode());
    assertEquals("Krankenhaus", typeCoding.getDisplay());

    assertTrue(notifiedPersonFacility.hasName());
    assertEquals("TEST Organisation", notifiedPersonFacility.getName());

    assertTrue(notifiedPersonFacility.hasTelecom());
    final List<ContactPoint> telecomList = notifiedPersonFacility.getTelecom();
    assertEquals(2, telecomList.size());
    final ContactPoint phone = telecomList.get(0);
    assertEquals("phone", phone.getSystem().toCode());
    assertEquals("01234567", phone.getValue());
    final ContactPoint email = telecomList.get(1);
    assertEquals("email", email.getSystem().toCode());
    assertEquals("anna@ansprechpartner.de", email.getValue());

    assertTrue(notifiedPersonFacility.hasAddress());
    final List<Address> addresses = notifiedPersonFacility.getAddress();
    assertEquals(1, addresses.size());
    final Address address = addresses.get(0);
    checkAddress(address, notifiedPersonFacilityAddressInfo);

    assertTrue(notifiedPersonFacility.hasContact());
    final List<OrganizationContactComponent> contactList = notifiedPersonFacility.getContact();
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

  private void checkNotificationDisease(
      BundleEntryComponent notificationDiseaseEntry,
      String notifiedPersonShortUrl,
      String notifierRoleShortUrl,
      String diseaseCVDDShortUrl,
      String diseaseInformationCommonShortUrl,
      String diseaseInformationCVDDShortUrl) {
    assertTrue(notificationDiseaseEntry.hasResource());
    final Composition notificationDisease = (Composition) notificationDiseaseEntry.getResource();
    assertTrue(notificationDisease.hasId());

    assertTrue(notificationDisease.hasMeta());
    final Meta meta = notificationDisease.getMeta();
    assertTrue(meta.hasProfile());
    assertEquals(PROFILE_NOTIFICATION_DISEASE, meta.getProfile().get(0).asStringValue());

    assertTrue(notificationDisease.hasIdentifier());
    final Identifier identifier = notificationDisease.getIdentifier();
    assertEquals(NAMING_SYSTEM_NOTIFICATION_ID, identifier.getSystem());

    assertEquals(CompositionStatus.FINAL, notificationDisease.getStatus());

    assertTrue(notificationDisease.hasType());
    final List<Coding> typeCodings = notificationDisease.getType().getCoding();
    assertEquals(1, typeCodings.size());
    final Coding typeCoding = typeCodings.get(0);
    assertEquals(SYSTEM_LOINC, typeCoding.getSystem());
    assertEquals("34782-3", typeCoding.getCode());
    assertEquals("Infectious disease Note", typeCoding.getDisplay());

    final List<CodeableConcept> categoryList = notificationDisease.getCategory();
    assertEquals(1, categoryList.size());
    final List<Coding> categoryCodings = categoryList.get(0).getCoding();
    assertEquals(1, categoryCodings.size());
    final Coding categoryCoding = categoryCodings.get(0);
    assertEquals(CODE_SYSTEM_NOTIFICATION_TYPE, categoryCoding.getSystem());
    assertEquals("6.1_2", categoryCoding.getCode());
    assertEquals("Meldung gemäß IfSG §6 Absatz 1, 2", categoryCoding.getDisplay());

    assertTrue(notificationDisease.hasSubject());
    final Reference subject = notificationDisease.getSubject();
    assertTrue(subject.hasReference());
    assertEquals(notifiedPersonShortUrl, subject.getReference());

    assertTrue(notificationDisease.hasDate());

    final List<Reference> authors = notificationDisease.getAuthor();
    assertEquals(1, authors.size());
    final Reference author = authors.get(0);
    assertTrue(author.hasReference());
    assertEquals(notifierRoleShortUrl, author.getReference());

    assertEquals("Meldung gemäß §6 Absatz 1, 2 IfSG", notificationDisease.getTitle());

    final List<SectionComponent> sections = notificationDisease.getSection();
    assertEquals(3, sections.size());

    final SectionComponent diagnosisSection = sections.get(0);
    assertEquals("Diagnose", diagnosisSection.getTitle());
    assertTrue(diagnosisSection.hasCode());
    final List<Coding> diagnosisCodeCodings = diagnosisSection.getCode().getCoding();
    assertEquals(1, diagnosisCodeCodings.size());
    final Coding diagnosisCodeCoding = diagnosisCodeCodings.get(0);
    assertEquals(CODE_SYSTEM_SECTION_CODE, diagnosisCodeCoding.getSystem());
    assertEquals("diagnosis", diagnosisCodeCoding.getCode());
    assertEquals("Diagnose", diagnosisCodeCoding.getDisplay());
    final List<Reference> diagnosisEntries = diagnosisSection.getEntry();
    assertEquals(1, diagnosisEntries.size());
    final Reference diagnosisEntry = diagnosisEntries.get(0);
    assertEquals(diseaseCVDDShortUrl, diagnosisEntry.getReference());

    final SectionComponent diseaseCommonSection = sections.get(1);
    assertEquals(
        "Meldetatbestandsübergreifende klinische und epidemiologische Angaben",
        diseaseCommonSection.getTitle());
    assertTrue(diseaseCommonSection.hasCode());
    final List<Coding> diseaseCommonCodeCodings = diseaseCommonSection.getCode().getCoding();
    assertEquals(1, diseaseCommonCodeCodings.size());
    final Coding diseaseCommonCodeCoding = diseaseCommonCodeCodings.get(0);
    assertEquals(CODE_SYSTEM_SECTION_CODE, diseaseCommonCodeCoding.getSystem());
    assertEquals("generalClinAndEpiInformation", diseaseCommonCodeCoding.getCode());
    assertEquals(
        "Meldetatbestandsübergreifende klinische und epidemiologische Angaben",
        diseaseCommonCodeCoding.getDisplay());
    final List<Reference> diseaseCommonEntries = diseaseCommonSection.getEntry();
    assertEquals(1, diseaseCommonEntries.size());
    final Reference diseaseCommonEntry = diseaseCommonEntries.get(0);
    assertEquals(diseaseInformationCommonShortUrl, diseaseCommonEntry.getReference());

    final SectionComponent diseaseCVDDSection = sections.get(2);
    assertEquals(
        "Meldetatbestandsspezifische klinische und epidemiologische Angaben",
        diseaseCVDDSection.getTitle());
    assertTrue(diseaseCVDDSection.hasCode());
    final List<Coding> diseaseCVDDCodeCodings = diseaseCVDDSection.getCode().getCoding();
    assertEquals(1, diseaseCVDDCodeCodings.size());
    final Coding diseaseCVDDCodeCoding = diseaseCVDDCodeCodings.get(0);
    assertEquals(CODE_SYSTEM_SECTION_CODE, diseaseCVDDCodeCoding.getSystem());
    assertEquals("specificClinAndEpiInformation", diseaseCVDDCodeCoding.getCode());
    assertEquals(
        "Meldetatbestandsspezifische klinische und epidemiologische Angaben",
        diseaseCVDDCodeCoding.getDisplay());
    final List<Reference> diseaseCVDDEntries = diseaseCVDDSection.getEntry();
    assertEquals(1, diseaseCVDDEntries.size());
    final Reference diseaseCVDDEntry = diseaseCVDDEntries.get(0);
    assertEquals(diseaseInformationCVDDShortUrl, diseaseCVDDEntry.getReference());
  }

  private void checkNotifiedPerson(
      BundleEntryComponent notifiedPersonEntry,
      NotifiedPerson notifiedPersonInfo,
      String notifiedPersonFacilityShortUrl) {
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

    assertTrue(notifiedPerson.hasAddress());
    final List<Address> addresses = notifiedPerson.getAddress();
    assertEquals(2, addresses.size());

    final Address primaryAddress = addresses.get(0);
    checkAddress(primaryAddress, notifiedPersonInfo.getPrimaryAddress());
    assertTrue(primaryAddress.hasExtension());
    final List<Extension> primaryAddressExtensions = primaryAddress.getExtension();
    assertEquals(1, primaryAddressExtensions.size());

    final Extension primaryAddressUseExtension = primaryAddressExtensions.get(0);
    assertEquals(
        FhirConstants.STRUCTURE_DEFINITION_ADDRESS_USE, primaryAddressUseExtension.getUrl());
    final Coding primaryAddressUseCode = (Coding) primaryAddressUseExtension.getValue();
    assertEquals(FhirConstants.CODE_SYSTEM_ADDRESS_USE, primaryAddressUseCode.getSystem());
    assertEquals("primary", primaryAddressUseCode.getCode());

    final Address currentFacilityAddress = addresses.get(1);
    assertTrue(currentFacilityAddress.hasExtension());
    final List<Extension> currentFacilityAddressExtensions = currentFacilityAddress.getExtension();
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
    assertEquals(
        notifiedPersonFacilityShortUrl,
        ((Reference) facilityAddressExtension.getValue()).getReference());
  }

  private void checkDiseaseCVDD(
      BundleEntryComponent diseaseCVDDEntry, String notifiedPersonShortUrl) {
    assertTrue(diseaseCVDDEntry.hasResource());
    final Condition diseaseCVDD = (Condition) diseaseCVDDEntry.getResource();

    assertTrue(diseaseCVDD.hasId());

    assertTrue(diseaseCVDD.hasMeta());
    final Meta meta = diseaseCVDD.getMeta();
    assertTrue(meta.hasProfile());
    assertEquals(FhirConstants.PROFILE_DISEASE_CVDD, meta.getProfile().get(0).asStringValue());

    assertTrue(diseaseCVDD.hasVerificationStatus());
    final List<Coding> verificationStatusCodings = diseaseCVDD.getVerificationStatus().getCoding();
    assertEquals(1, verificationStatusCodings.size());
    final Coding verificationStatusCoding = verificationStatusCodings.get(0);
    assertEquals(CODE_SYSTEM_CONDITION_VERIFICATION_STATUS, verificationStatusCoding.getSystem());
    assertEquals("confirmed", verificationStatusCoding.getCode());

    assertTrue(diseaseCVDD.hasCode());
    final List<Coding> codeCodings = diseaseCVDD.getCode().getCoding();
    assertEquals(1, codeCodings.size());
    final Coding codeCoding = codeCodings.get(0);
    assertEquals(CODE_SYSTEM_NOTIFICATION_DISEASE_CATEGORY, codeCoding.getSystem());
    assertEquals("cvdd", codeCoding.getCode());
    assertEquals("Coronavirus-Krankheit-2019 (COVID-19)", codeCoding.getDisplay());

    assertTrue(diseaseCVDD.hasSubject());
    final Reference subject = diseaseCVDD.getSubject();
    assertEquals(notifiedPersonShortUrl, subject.getReference());

    assertEquals("2022-01-01", diseaseCVDD.getOnsetDateTimeType().getValueAsString());
    assertEquals("2022-01-02", diseaseCVDD.getRecordedDateElement().getValueAsString());

    final List<ConditionEvidenceComponent> evidenceList = diseaseCVDD.getEvidence();
    assertEquals(18, evidenceList.size());
    Map<String, String> expected = new HashMap<>();
    expected.put("386661006", "Fever (finding)");
    expected.put("267102003", "Sore throat symptom (finding)");
    expected.put("49727002", "Cough (finding)");
    expected.put("233604007", "Pneumonia (disorder)");
    expected.put("275280004", "Sniffles (finding)");
    expected.put("67782005", "Acute respiratory distress syndrome (disorder)");
    expected.put("371820004", "Patient ventilated (finding)");
    expected.put("267036007", "Dyspnea (finding)");
    expected.put("62315008", "Diarrhea (finding)");
    expected.put("44169009", "Loss of sense of smell (finding)");
    expected.put("36955009", "Loss of taste (finding)");
    expected.put("271823003", "Tachypnea (finding)");
    expected.put("3424008", "Tachycardia (finding)");
    expected.put("213257006", "Generally unwell (finding)");
    expected.put("43724002", "Chill (finding)");
    expected.put("25064002", "Headache (finding)");
    expected.put("68962001", "Muscle pain (finding)");
    expected.put("840539006:47429007=404684003", "andere COVID-19-Symptome");
    for (ConditionEvidenceComponent evidence : evidenceList) {
      final List<CodeableConcept> code = evidence.getCode();
      assertEquals(1, code.size());
      final List<Coding> evidenceCodeCodings = code.get(0).getCoding();
      assertEquals(1, evidenceCodeCodings.size());
      final Coding evidenceCodeCoding = evidenceCodeCodings.get(0);
      assertEquals(SYSTEM_SNOMED, evidenceCodeCoding.getSystem());
      assertTrue(expected.containsKey(evidenceCodeCoding.getCode()));
      assertTrue(expected.containsValue(evidenceCodeCoding.getDisplay()));
      assertTrue(expected.remove(evidenceCodeCoding.getCode(), evidenceCodeCoding.getDisplay()));
    }

    assertTrue(expected.isEmpty());

    assertTrue(diseaseCVDD.hasNote());
    final List<Annotation> notes = diseaseCVDD.getNote();
    assertEquals(1, notes.size());
    assertEquals("Textueller Hinweis", notes.get(0).getText());
  }

  private void checkNotifierFacility(
      BundleEntryComponent notifierFacilityEntry,
      @NotNull @Valid FacilityAddressInfo notifierFacilityAddressInfo) {
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
    assertEquals("123456789", identifier.getValue());

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
    checkAddress(address, notifierFacilityAddressInfo);

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

  private void checkHospitalizationEncounter(
      BundleEntryComponent hospitalizationEncounterEntry,
      String notifiedPersonShortUrl,
      String notifiedPersonFacilityShortUrl) {
    final Encounter hospitalizationEncounter =
        checkEncounter(
            hospitalizationEncounterEntry, notifiedPersonShortUrl, notifiedPersonFacilityShortUrl);

    assertTrue(hospitalizationEncounter.hasPeriod());
    assertEquals(
        DateUtils.createDate(LocalDate.of(2022, 1, 5)),
        hospitalizationEncounter.getPeriod().getStart());

    assertTrue(hospitalizationEncounter.hasExtension());
    final List<Extension> extensions = hospitalizationEncounter.getExtension();
    assertEquals(1, extensions.size());
    final Extension extension = extensions.get(0);
    assertEquals(STRUCTURE_DEFINITION_HOSPITALIZATION_NOTE, extension.getUrl());
    assertEquals("wichtige Zusatzinformation", extension.getValue().toString());
  }

  private void checkIntensiveCareEncounter(
      BundleEntryComponent intensiveCareEncounterEntry,
      String notifiedPersonShortUrl,
      String notifiedPersonFacilityShortUrl) {
    final Encounter intensiveCareEncounter =
        checkEncounter(
            intensiveCareEncounterEntry, notifiedPersonShortUrl, notifiedPersonFacilityShortUrl);

    assertTrue(intensiveCareEncounter.hasServiceType());
    final List<Coding> serviceTypeCodings = intensiveCareEncounter.getServiceType().getCoding();
    assertEquals(1, serviceTypeCodings.size());
    final Coding serviceTypeCoding = serviceTypeCodings.get(0);
    assertEquals(CODE_SYSTEM_HOSPITALIZATION_SERVICE_TYPE, serviceTypeCoding.getSystem());
    assertEquals("3600", serviceTypeCoding.getCode());
    assertEquals("Intensivmedizin", serviceTypeCoding.getDisplay());

    assertTrue(intensiveCareEncounter.hasPeriod());
    assertEquals(
        DateUtils.createDate(LocalDate.of(2022, 1, 7)),
        intensiveCareEncounter.getPeriod().getStart());
  }

  private void checkDiseaseInformationCommon(
      BundleEntryComponent diseaseInformationCommonEntry,
      String notifiedPersonShortUrl,
      String hospitalizationEncounterShortUrl,
      String intensiveCareEncounterShortUrl,
      String labShortUrl,
      String infectProtectFacilityShortUrl) {
    assertTrue(diseaseInformationCommonEntry.hasResource());
    final QuestionnaireResponse diseaseInformationCommon =
        (QuestionnaireResponse) diseaseInformationCommonEntry.getResource();
    assertTrue(diseaseInformationCommon.hasId());

    assertTrue(diseaseInformationCommon.hasMeta());
    final Meta meta = diseaseInformationCommon.getMeta();
    assertTrue(meta.hasProfile());
    assertEquals(PROFILE_DISEASE_INFORMATION_COMMON, meta.getProfile().get(0).asStringValue());

    assertEquals(
        QUESTIONAIRE_DISEASE_QUESTIONS_COMMON, diseaseInformationCommon.getQuestionnaire());
    assertEquals(QuestionnaireResponseStatus.COMPLETED, diseaseInformationCommon.getStatus());

    assertTrue(diseaseInformationCommon.hasSubject());
    final Reference subject = diseaseInformationCommon.getSubject();
    assertTrue(subject.hasReference());
    assertEquals(notifiedPersonShortUrl, subject.getReference());

    final List<QuestionnaireResponseItemComponent> items = diseaseInformationCommon.getItem();
    assertEquals(8, items.size());

    checkDeathInformation(items.get(0));
    checkMilitaryaffiliation(items.get(1));
    checkLabSpecimenTaken(items.get(2), labShortUrl);
    checkHospitalizationInformation(
        items.get(3), hospitalizationEncounterShortUrl, intensiveCareEncounterShortUrl);
    checkInfectProtectFacilityInformation(items.get(4), infectProtectFacilityShortUrl);
    checkPlaceExposure(items.get(5));
    checkOrganDonation(items.get(6));
    checkAdditionalInformation(items.get(7));
  }

  private void checkLab(BundleEntryComponent labEntry, LabInfo labInfo) {
    assertTrue(labEntry.hasResource());
    final Organization lab = (Organization) labEntry.getResource();

    assertTrue(lab.hasId());

    assertTrue(lab.hasMeta());
    final Meta meta = lab.getMeta();
    assertTrue(meta.hasProfile());
    assertEquals(FhirConstants.PROFILE_ORGANIZATION, meta.getProfile().get(0).asStringValue());

    assertTrue(lab.hasName());
    assertEquals("Labor", lab.getName());

    assertTrue(lab.hasAddress());
    final List<Address> addresses = lab.getAddress();
    assertEquals(1, addresses.size());
    final Address address = addresses.get(0);
    checkAddress(address, labInfo.getAddress());

    assertFalse(lab.hasType());
  }

  private void checkInfectProtectFacility(
      BundleEntryComponent infectProtectFacilityEntry,
      InfectionProtectionFacilityInfo infectProtectFacilityInfo) {
    assertTrue(infectProtectFacilityEntry.hasResource());
    final Organization infectProtectFacility =
        (Organization) infectProtectFacilityEntry.getResource();

    assertTrue(infectProtectFacility.hasId());

    assertTrue(infectProtectFacility.hasMeta());
    final Meta meta = infectProtectFacility.getMeta();
    assertTrue(meta.hasProfile());
    assertEquals(FhirConstants.PROFILE_ORGANIZATION, meta.getProfile().get(0).asStringValue());

    assertTrue(infectProtectFacility.hasName());
    assertEquals("Einrichtungsname", infectProtectFacility.getName());

    assertTrue(infectProtectFacility.hasAddress());
    final List<Address> addresses = infectProtectFacility.getAddress();
    assertEquals(1, addresses.size());
    final Address address = addresses.get(0);
    checkAddress(address, infectProtectFacilityInfo.getAddress());

    assertFalse(infectProtectFacility.hasType());

    assertTrue(infectProtectFacility.hasTelecom());
    final List<ContactPoint> telecomList = infectProtectFacility.getTelecom();
    assertEquals(2, telecomList.size());
    final ContactPoint phone = telecomList.get(0);
    assertEquals("phone", phone.getSystem().toCode());
    assertEquals("0123456789", phone.getValue());
    final ContactPoint email = telecomList.get(1);
    assertEquals("email", email.getSystem().toCode());
    assertEquals("mail@einrichtung.de", email.getValue());
  }

  private void checkDiseaseInformationCVDD(
      BundleEntryComponent diseaseInformationCVDDEntry,
      String notifiedPersonShortUrl,
      String firstImmunizationShortUrl,
      String secondImmunizationShortUrl,
      String thirdImmunizationShortUrl,
      String furtherImmunizationShortUrl) {
    assertTrue(diseaseInformationCVDDEntry.hasResource());
    final QuestionnaireResponse diseaseInformationCVDD =
        (QuestionnaireResponse) diseaseInformationCVDDEntry.getResource();
    assertTrue(diseaseInformationCVDD.hasId());

    assertTrue(diseaseInformationCVDD.hasMeta());
    final Meta meta = diseaseInformationCVDD.getMeta();
    assertTrue(meta.hasProfile());
    assertEquals(PROFILE_DISEASE_INFORMATION_CVDD, meta.getProfile().get(0).asStringValue());

    assertEquals(QUESTIONAIRE_DISEASE_QUESTIONS_CVDD, diseaseInformationCVDD.getQuestionnaire());
    assertEquals(QuestionnaireResponseStatus.COMPLETED, diseaseInformationCVDD.getStatus());

    assertTrue(diseaseInformationCVDD.hasSubject());
    final Reference subject = diseaseInformationCVDD.getSubject();
    assertTrue(subject.hasReference());
    assertEquals(notifiedPersonShortUrl, subject.getReference());

    final List<QuestionnaireResponseItemComponent> items = diseaseInformationCVDD.getItem();
    assertEquals(3, items.size());

    checkInfectionSource(items.get(0));
    checkInfectionEnvironmentSetting(items.get(1));
    checkImmunization(
        items.get(2),
        firstImmunizationShortUrl,
        secondImmunizationShortUrl,
        thirdImmunizationShortUrl,
        furtherImmunizationShortUrl);
  }

  private void checkFirstImmunizationCVDD(
      BundleEntryComponent firstImmunizationEntry, String notifiedPersonShortUrl) {
    checkImmunizationCVDD(
        firstImmunizationEntry,
        notifiedPersonShortUrl,
        COMMUNITY_REGISTER,
        "EU/1/20/1528",
        "Comirnaty",
        "2021",
        TemporalPrecisionEnum.YEAR,
        "Zusatzinfo1");
  }

  private void checkSecondImmunizationCVDD(
      BundleEntryComponent secondImmunizationEntry, String notifiedPersonShortUrl) {
    checkImmunizationCVDD(
        secondImmunizationEntry,
        notifiedPersonShortUrl,
        COMMUNITY_REGISTER,
        "EU/1/20/1528",
        "Comirnaty",
        "2021-07",
        TemporalPrecisionEnum.MONTH,
        "Zusatzinfo2");
  }

  private void checkThirdImmunizationCVDD(
      BundleEntryComponent thirdImmunizationEntry, String notifiedPersonShortUrl) {
    checkImmunizationCVDD(
        thirdImmunizationEntry,
        notifiedPersonShortUrl,
        CODE_SYSTEM_NULL_FLAVOR,
        "ASKU",
        "asked but unknown",
        "2021-11-30",
        TemporalPrecisionEnum.DAY,
        "Zusatzinfo3");
  }

  private void checkFurtherImmunizationCVDD(
      BundleEntryComponent furtherImmunizationEntry, String notifiedPersonShortUrl) {
    checkImmunizationCVDD(
        furtherImmunizationEntry,
        notifiedPersonShortUrl,
        CODE_SYSTEM_VACCINE,
        "otherVaccine",
        "Anderer Impfstoff",
        "2021-12-25",
        TemporalPrecisionEnum.DAY,
        "Zusatzinfo4");
  }

  private void checkImmunizationCVDD(
      BundleEntryComponent firstImmunizationEntry,
      String notifiedPersonShortUrl,
      String expectedVaccineCodeSystem,
      String expectedVaccineCodeCode,
      String expectedVaccineCodeDisplay,
      String expectedDate,
      TemporalPrecisionEnum expectedPrecision,
      String expectedNote) {
    assertTrue(firstImmunizationEntry.hasResource());
    final Immunization immunization = (Immunization) firstImmunizationEntry.getResource();
    assertTrue(immunization.hasId());

    assertTrue(immunization.hasMeta());
    final Meta meta = immunization.getMeta();
    assertTrue(meta.hasProfile());
    assertEquals(PROFILE_IMMUNIZATION_INFORMATION_CVDD, meta.getProfile().get(0).asStringValue());

    assertEquals(ImmunizationStatus.COMPLETED, immunization.getStatus());

    assertTrue(immunization.hasVaccineCode());
    final List<Coding> vaccineCodeCodings = immunization.getVaccineCode().getCoding();
    assertEquals(1, vaccineCodeCodings.size());
    final Coding vaccineCodeCoding = vaccineCodeCodings.get(0);
    assertEquals(expectedVaccineCodeSystem, vaccineCodeCoding.getSystem());
    assertEquals(expectedVaccineCodeCode, vaccineCodeCoding.getCode());
    assertEquals(expectedVaccineCodeDisplay, vaccineCodeCoding.getDisplay());

    assertTrue(immunization.hasPatient());
    final Reference patient = immunization.getPatient();
    assertTrue(patient.hasReference());
    assertEquals(notifiedPersonShortUrl, patient.getReference());

    assertTrue(immunization.hasOccurrence());
    final DateTimeType occurrence = (DateTimeType) immunization.getOccurrence();
    assertEquals(expectedDate, occurrence.getValueAsString());
    assertEquals(expectedPrecision, occurrence.getPrecision());

    assertTrue(immunization.hasNote());
    final List<Annotation> notes = immunization.getNote();
    assertEquals(1, notes.size());
    assertEquals(expectedNote, notes.get(0).getText());
  }

  private void checkInfectionSource(QuestionnaireResponseItemComponent infectionSourceItem) {
    assertEquals("infectionSource", infectionSourceItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent> infectionSourceAnswerList =
        infectionSourceItem.getAnswer();
    assertEquals(1, infectionSourceAnswerList.size());
    final QuestionnaireResponseItemAnswerComponent infectionSourceAnswer =
        infectionSourceAnswerList.get(0);
    final Coding infectionSourceCoding = (Coding) infectionSourceAnswer.getValue();
    assertEquals(CODE_SYSTEM_YES_OR_NO_ANSWER, infectionSourceCoding.getSystem());
    assertEquals("yes", infectionSourceCoding.getCode());
    assertEquals("Ja", infectionSourceCoding.getDisplay());
  }

  private void checkInfectionEnvironmentSetting(
      QuestionnaireResponseItemComponent infectionEnvironmentItem) {
    assertEquals("infectionEnvironmentSetting", infectionEnvironmentItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent> infectionEnvironmentAnswerList =
        infectionEnvironmentItem.getAnswer();
    assertEquals(1, infectionEnvironmentAnswerList.size());
    final QuestionnaireResponseItemAnswerComponent infectionEnvironmentAnswer =
        infectionEnvironmentAnswerList.get(0);
    final Coding infectionEnvironmentCoding = (Coding) infectionEnvironmentAnswer.getValue();
    assertEquals(CODE_SYSTEM_YES_OR_NO_ANSWER, infectionEnvironmentCoding.getSystem());
    assertEquals("yes", infectionEnvironmentCoding.getCode());
    assertEquals("Ja", infectionEnvironmentCoding.getDisplay());

    assertTrue(infectionEnvironmentAnswer.hasItem());
    final List<QuestionnaireResponseItemComponent> infectionEnvironmentAnswerItems =
        infectionEnvironmentAnswer.getItem();
    assertEquals(1, infectionEnvironmentAnswerItems.size());

    final QuestionnaireResponseItemComponent infectionEnvironmentGroupItem =
        infectionEnvironmentAnswerItems.get(0);
    assertEquals("infectionEnvironmentSettingGroup", infectionEnvironmentGroupItem.getLinkId());
    assertTrue(infectionEnvironmentGroupItem.hasItem());
    final List<QuestionnaireResponseItemComponent> infectionEnvironmentGroupItemItems =
        infectionEnvironmentGroupItem.getItem();
    assertEquals(3, infectionEnvironmentGroupItemItems.size());

    final QuestionnaireResponseItemComponent kindItem = infectionEnvironmentGroupItemItems.get(0);
    assertEquals("infectionEnvironmentSettingKind", kindItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent> kindItemAnswers = kindItem.getAnswer();
    assertEquals(1, kindItemAnswers.size());
    final QuestionnaireResponseItemAnswerComponent kindItemAnswer = kindItemAnswers.get(0);
    final Coding kindCoding = (Coding) kindItemAnswer.getValue();
    assertEquals(CODE_SYSTEM_INFECTION_ENVIRONMENT_SETTING, kindCoding.getSystem());
    assertEquals("3", kindCoding.getCode());
    assertEquals("Gesundheitseinrichtung", kindCoding.getDisplay());

    final QuestionnaireResponseItemComponent beginItem = infectionEnvironmentGroupItemItems.get(1);
    assertEquals("infectionEnvironmentSettingBegin", beginItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent> beginItemAnswers = beginItem.getAnswer();
    assertEquals(1, beginItemAnswers.size());
    final QuestionnaireResponseItemAnswerComponent beginItemAnswer = beginItemAnswers.get(0);
    assertEquals(
        DateUtils.createDate(LocalDate.of(2021, 12, 28)),
        ((DateType) beginItemAnswer.getValue()).getValue());

    final QuestionnaireResponseItemComponent endItem = infectionEnvironmentGroupItemItems.get(2);
    assertEquals("infectionEnvironmentSettingEnd", endItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent> endItemAnswers = endItem.getAnswer();
    assertEquals(1, endItemAnswers.size());
    final QuestionnaireResponseItemAnswerComponent endItemAnswer = endItemAnswers.get(0);
    assertEquals(
        DateUtils.createDate(LocalDate.of(2021, 12, 30)),
        ((DateType) endItemAnswer.getValue()).getValue());
  }

  private void checkImmunization(
      QuestionnaireResponseItemComponent immunizationItem,
      String firstImmunizationShortUrl,
      String secondImmunizationShortUrl,
      String thirdImmunizationShortUrl,
      String furtherImmunizationShortUrl) {
    assertEquals("immunization", immunizationItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent> immunizationAnswerList =
        immunizationItem.getAnswer();
    assertEquals(1, immunizationAnswerList.size());
    final QuestionnaireResponseItemAnswerComponent immunizationAnswer =
        immunizationAnswerList.get(0);
    final Coding immunizationCoding = (Coding) immunizationAnswer.getValue();
    assertEquals(CODE_SYSTEM_YES_OR_NO_ANSWER, immunizationCoding.getSystem());
    assertEquals("yes", immunizationCoding.getCode());
    assertEquals("Ja", immunizationCoding.getDisplay());

    assertTrue(immunizationAnswer.hasItem());
    final List<QuestionnaireResponseItemComponent> immunizationRefItems =
        immunizationAnswer.getItem();
    assertEquals(4, immunizationRefItems.size());

    checkImmunizationRef(immunizationRefItems.get(0), firstImmunizationShortUrl);
    checkImmunizationRef(immunizationRefItems.get(1), secondImmunizationShortUrl);
    checkImmunizationRef(immunizationRefItems.get(2), thirdImmunizationShortUrl);
    checkImmunizationRef(immunizationRefItems.get(3), furtherImmunizationShortUrl);
  }

  private void checkImmunizationRef(
      QuestionnaireResponseItemComponent immunizationRefItem, String immunizationShortUrl) {
    assertEquals("immunizationRef", immunizationRefItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent> immunizationRefItemAnswers =
        immunizationRefItem.getAnswer();
    assertEquals(1, immunizationRefItemAnswers.size());
    final QuestionnaireResponseItemAnswerComponent immunizationRefItemAnswer =
        immunizationRefItemAnswers.get(0);
    assertEquals(
        immunizationShortUrl, ((Reference) immunizationRefItemAnswer.getValue()).getReference());
  }

  private void checkDeathInformation(QuestionnaireResponseItemComponent deadItem) {
    assertEquals("isDead", deadItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent> deadAnswerList = deadItem.getAnswer();
    assertEquals(1, deadAnswerList.size());
    final QuestionnaireResponseItemAnswerComponent deadAnswer = deadAnswerList.get(0);
    final Coding deadCoding = (Coding) deadAnswer.getValue();
    assertEquals(CODE_SYSTEM_YES_OR_NO_ANSWER, deadCoding.getSystem());
    assertEquals("yes", deadCoding.getCode());
    assertEquals("Ja", deadCoding.getDisplay());

    assertTrue(deadAnswer.hasItem());
    final List<QuestionnaireResponseItemComponent> deadAnswerItems = deadAnswer.getItem();
    assertEquals(1, deadAnswerItems.size());

    final QuestionnaireResponseItemComponent deathDateItem = deadAnswerItems.get(0);
    assertEquals("deathDate", deathDateItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent> deadDateAnswerList =
        deathDateItem.getAnswer();
    assertEquals(1, deadDateAnswerList.size());
    final QuestionnaireResponseItemAnswerComponent deathDateAnswer = deadDateAnswerList.get(0);
    assertEquals(
        DateUtils.createDate(LocalDate.of(2022, 1, 22)),
        ((DateType) deathDateAnswer.getValue()).getValue());
  }

  private void checkMilitaryaffiliation(QuestionnaireResponseItemComponent militaryItem) {
    assertEquals("militaryAffiliation", militaryItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent> militaryAnswerList =
        militaryItem.getAnswer();
    assertEquals(1, militaryAnswerList.size());
    final QuestionnaireResponseItemAnswerComponent militaryAnswer = militaryAnswerList.get(0);
    final Coding militaryCoding = (Coding) militaryAnswer.getValue();
    assertEquals(CODE_SYSTEM_MILITARY_AFFILIATION, militaryCoding.getSystem());
    assertEquals("memberOfBundeswehr", militaryCoding.getCode());
    assertEquals("Soldat/BW-Angehöriger", militaryCoding.getDisplay());
  }

  private void checkLabSpecimenTaken(
      QuestionnaireResponseItemComponent labSpecimenItem, String labShortUrl) {
    assertEquals("labSpecimenTaken", labSpecimenItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent> labSpecimenAnswerList =
        labSpecimenItem.getAnswer();
    assertEquals(1, labSpecimenAnswerList.size());
    final QuestionnaireResponseItemAnswerComponent labSpecimenAnswer = labSpecimenAnswerList.get(0);
    final Coding labSpecimenCoding = (Coding) labSpecimenAnswer.getValue();
    assertEquals(CODE_SYSTEM_YES_OR_NO_ANSWER, labSpecimenCoding.getSystem());
    assertEquals("yes", labSpecimenCoding.getCode());
    assertEquals("Ja", labSpecimenCoding.getDisplay());

    assertTrue(labSpecimenAnswer.hasItem());
    final List<QuestionnaireResponseItemComponent> labSpecimenAnswerItems =
        labSpecimenAnswer.getItem();
    assertEquals(1, labSpecimenAnswerItems.size());

    final QuestionnaireResponseItemComponent labItem = labSpecimenAnswerItems.get(0);
    assertEquals("labSpecimenLab", labItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent> labAnswerList = labItem.getAnswer();
    assertEquals(1, labAnswerList.size());
    final QuestionnaireResponseItemAnswerComponent labAnswer = labAnswerList.get(0);
    assertEquals(labShortUrl, ((Reference) labAnswer.getValue()).getReference());
  }

  private void checkHospitalizationInformation(
      QuestionnaireResponseItemComponent hospitalizedItem,
      String hospitalizationEncounterShortUrl,
      String intensiveCareEncounterShortUrl) {
    assertEquals("hospitalized", hospitalizedItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent> hospitalizedAnswerList =
        hospitalizedItem.getAnswer();
    assertEquals(1, hospitalizedAnswerList.size());
    final QuestionnaireResponseItemAnswerComponent hospitalizedAnswer =
        hospitalizedAnswerList.get(0);
    final Coding hospitalizedCoding = (Coding) hospitalizedAnswer.getValue();
    assertEquals(CODE_SYSTEM_YES_OR_NO_ANSWER, hospitalizedCoding.getSystem());
    assertEquals("yes", hospitalizedCoding.getCode());
    assertEquals("Ja", hospitalizedCoding.getDisplay());

    assertTrue(hospitalizedAnswer.hasItem());
    final List<QuestionnaireResponseItemComponent> hospitalizedAnswerItems =
        hospitalizedAnswer.getItem();
    assertEquals(2, hospitalizedAnswerItems.size());

    checkHospitalizedEncounter(hospitalizedAnswerItems.get(0), hospitalizationEncounterShortUrl);
    checkHospitalizedEncounter(hospitalizedAnswerItems.get(1), intensiveCareEncounterShortUrl);
  }

  private void checkHospitalizedEncounter(
      QuestionnaireResponseItemComponent hospitalizedGroupItem,
      String hospitalizationEncounterShortUrl) {
    assertEquals("hospitalizedGroup", hospitalizedGroupItem.getLinkId());
    assertTrue(hospitalizedGroupItem.hasItem());
    final List<QuestionnaireResponseItemComponent> hospitalizedGroupItemItems =
        hospitalizedGroupItem.getItem();
    assertEquals(1, hospitalizedGroupItemItems.size());
    final QuestionnaireResponseItemComponent hospitalizedEncounterItem =
        hospitalizedGroupItemItems.get(0);
    assertEquals("hospitalizedEncounter", hospitalizedEncounterItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent> hospitalizedEncounterItemAnswers =
        hospitalizedEncounterItem.getAnswer();
    assertEquals(1, hospitalizedEncounterItemAnswers.size());
    final QuestionnaireResponseItemAnswerComponent hospitalizedEncounterItemAnswer =
        hospitalizedEncounterItemAnswers.get(0);
    assertEquals(
        hospitalizationEncounterShortUrl,
        ((Reference) hospitalizedEncounterItemAnswer.getValue()).getReference());
  }

  private void checkInfectProtectFacilityInformation(
      QuestionnaireResponseItemComponent infectProtectFacilityItem,
      String infectProtectFacilityShortUrl) {
    assertEquals("infectProtectFacility", infectProtectFacilityItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent> infectProtectFacilityAnswerList =
        infectProtectFacilityItem.getAnswer();
    assertEquals(1, infectProtectFacilityAnswerList.size());
    final QuestionnaireResponseItemAnswerComponent infectProtectFacilityAnswer =
        infectProtectFacilityAnswerList.get(0);
    final Coding infactProtectFacilityCoding = (Coding) infectProtectFacilityAnswer.getValue();
    assertEquals(CODE_SYSTEM_YES_OR_NO_ANSWER, infactProtectFacilityCoding.getSystem());
    assertEquals("yes", infactProtectFacilityCoding.getCode());
    assertEquals("Ja", infactProtectFacilityCoding.getDisplay());

    assertTrue(infectProtectFacilityAnswer.hasItem());
    final List<QuestionnaireResponseItemComponent> infectProtectFacilityAnswerItems =
        infectProtectFacilityAnswer.getItem();
    assertEquals(1, infectProtectFacilityAnswerItems.size());

    final QuestionnaireResponseItemComponent infectProtectFacilityGroupItem =
        infectProtectFacilityAnswerItems.get(0);
    assertEquals("infectProtectFacilityGroup", infectProtectFacilityGroupItem.getLinkId());
    assertTrue(infectProtectFacilityGroupItem.hasItem());
    final List<QuestionnaireResponseItemComponent> infectProtectFacilityGroupItemItems =
        infectProtectFacilityGroupItem.getItem();
    assertEquals(4, infectProtectFacilityGroupItemItems.size());

    final QuestionnaireResponseItemComponent startDateItem =
        infectProtectFacilityGroupItemItems.get(0);
    assertEquals("infectProtectFacilityBegin", startDateItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent> startDateAnswers =
        startDateItem.getAnswer();
    assertEquals(1, startDateAnswers.size());
    final QuestionnaireResponseItemAnswerComponent startDateAnswer = startDateAnswers.get(0);
    assertEquals(
        DateUtils.createDate(LocalDate.of(2021, 12, 1)),
        ((DateType) startDateAnswer.getValue()).getValue());

    final QuestionnaireResponseItemComponent endDateItem =
        infectProtectFacilityGroupItemItems.get(1);
    assertEquals("infectProtectFacilityEnd", endDateItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent> endDateAnswers = endDateItem.getAnswer();
    assertEquals(1, endDateAnswers.size());
    final QuestionnaireResponseItemAnswerComponent endDateAnswer = endDateAnswers.get(0);
    assertEquals(
        DateUtils.createDate(LocalDate.of(2022, 1, 5)),
        ((DateType) endDateAnswer.getValue()).getValue());

    final QuestionnaireResponseItemComponent roleItem = infectProtectFacilityGroupItemItems.get(2);
    assertEquals("infectProtectFacilityRole", roleItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent> roleItemAnswers = roleItem.getAnswer();
    assertEquals(1, roleItemAnswers.size());
    final QuestionnaireResponseItemAnswerComponent roleItemAnswer = roleItemAnswers.get(0);
    final Coding roleCoding = (Coding) roleItemAnswer.getValue();
    assertEquals(CODE_SYSTEM_ORGANIZATION_ASSOCIATION, roleCoding.getSystem());
    assertEquals("employment", roleCoding.getCode());
    assertEquals("Tätigkeit", roleCoding.getDisplay());

    final QuestionnaireResponseItemComponent organizationItem =
        infectProtectFacilityGroupItemItems.get(3);
    assertEquals("infectProtectFacilityOrganization", organizationItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent> organizationItemAnswers =
        organizationItem.getAnswer();
    assertEquals(1, organizationItemAnswers.size());
    final QuestionnaireResponseItemAnswerComponent organizationItemAnswer =
        organizationItemAnswers.get(0);
    assertEquals(
        infectProtectFacilityShortUrl,
        ((Reference) organizationItemAnswer.getValue()).getReference());
  }

  private void checkPlaceExposure(QuestionnaireResponseItemComponent placeExposureItem) {
    assertEquals("placeExposure", placeExposureItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent> placeExposureAnswerList =
        placeExposureItem.getAnswer();
    assertEquals(1, placeExposureAnswerList.size());
    final QuestionnaireResponseItemAnswerComponent placeExposureAnswer =
        placeExposureAnswerList.get(0);
    final Coding placeExposureCoding = (Coding) placeExposureAnswer.getValue();
    assertEquals(CODE_SYSTEM_YES_OR_NO_ANSWER, placeExposureCoding.getSystem());
    assertEquals("yes", placeExposureCoding.getCode());
    assertEquals("Ja", placeExposureCoding.getDisplay());

    assertTrue(placeExposureAnswer.hasItem());
    final List<QuestionnaireResponseItemComponent> placeExposureAnswerItems =
        placeExposureAnswer.getItem();
    assertEquals(1, placeExposureAnswerItems.size());

    final QuestionnaireResponseItemComponent placeExposureGroupItem =
        placeExposureAnswerItems.get(0);
    assertEquals("placeExposureGroup", placeExposureGroupItem.getLinkId());
    assertTrue(placeExposureGroupItem.hasItem());
    final List<QuestionnaireResponseItemComponent> placeExposureGroupItemItems =
        placeExposureGroupItem.getItem();
    assertEquals(4, placeExposureGroupItemItems.size());

    final QuestionnaireResponseItemComponent startDateItem = placeExposureGroupItemItems.get(0);
    assertEquals("placeExposureBegin", startDateItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent> startDateAnswers =
        startDateItem.getAnswer();
    assertEquals(1, startDateAnswers.size());
    final QuestionnaireResponseItemAnswerComponent startDateAnswer = startDateAnswers.get(0);
    assertEquals(
        DateUtils.createDate(LocalDate.of(2021, 12, 20)),
        ((DateType) startDateAnswer.getValue()).getValue());

    final QuestionnaireResponseItemComponent endDateItem = placeExposureGroupItemItems.get(1);
    assertEquals("placeExposureEnd", endDateItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent> endDateAnswers = endDateItem.getAnswer();
    assertEquals(1, endDateAnswers.size());
    final QuestionnaireResponseItemAnswerComponent endDateAnswer = endDateAnswers.get(0);
    assertEquals(
        DateUtils.createDate(LocalDate.of(2021, 12, 28)),
        ((DateType) endDateAnswer.getValue()).getValue());

    final QuestionnaireResponseItemComponent regionItem = placeExposureGroupItemItems.get(2);
    assertEquals("placeExposureRegion", regionItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent> regionItemAnswers = regionItem.getAnswer();
    assertEquals(1, regionItemAnswers.size());
    final QuestionnaireResponseItemAnswerComponent regionItemAnswer = regionItemAnswers.get(0);
    final Coding regionCoding = (Coding) regionItemAnswer.getValue();
    assertEquals(CODE_SYSTEM_GEOGRAPHIC_REGION, regionCoding.getSystem());
    assertEquals("21000316", regionCoding.getCode());
    assertEquals("Libyen", regionCoding.getDisplay());

    final QuestionnaireResponseItemComponent hintItem = placeExposureGroupItemItems.get(3);
    assertEquals("placeExposureHint", hintItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent> hintItemAnswers = hintItem.getAnswer();
    assertEquals(1, hintItemAnswers.size());
    final QuestionnaireResponseItemAnswerComponent hintItemAnswer = hintItemAnswers.get(0);
    assertEquals("Anmerkung", ((StringType) hintItemAnswer.getValue()).asStringValue());
  }

  private void checkOrganDonation(QuestionnaireResponseItemComponent organDonationItem) {
    assertEquals("organDonation", organDonationItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent> organDonationAnswerList =
        organDonationItem.getAnswer();
    assertEquals(1, organDonationAnswerList.size());
    final QuestionnaireResponseItemAnswerComponent organDonationAnswer =
        organDonationAnswerList.get(0);
    final Coding organDonationCoding = (Coding) organDonationAnswer.getValue();
    assertEquals(CODE_SYSTEM_YES_OR_NO_ANSWER, organDonationCoding.getSystem());
    assertEquals("yes", organDonationCoding.getCode());
    assertEquals("Ja", organDonationCoding.getDisplay());
  }

  private void checkAdditionalInformation(
      QuestionnaireResponseItemComponent additionalInformationItem) {
    assertEquals("additionalInformation", additionalInformationItem.getLinkId());
    final List<QuestionnaireResponseItemAnswerComponent> additionalInformationAnswerList =
        additionalInformationItem.getAnswer();
    assertEquals(1, additionalInformationAnswerList.size());
    final QuestionnaireResponseItemAnswerComponent additionalInformationAnswer =
        additionalInformationAnswerList.get(0);
    assertEquals(
        "Zusatzinformationen zu den meldetatbestandsübergreifenden klinischen und epidemiologischen Angaben",
        ((StringType) additionalInformationAnswer.getValue()).asStringValue());
  }

  private Encounter checkEncounter(
      BundleEntryComponent encounterEntry,
      String notifiedPersonShortUrl,
      String notifiedPersonFacilityShortUrl) {
    assertTrue(encounterEntry.hasResource());
    final Encounter encounter = (Encounter) encounterEntry.getResource();
    assertTrue(encounter.hasId());

    assertTrue(encounter.hasMeta());
    final Meta meta = encounter.getMeta();
    assertTrue(meta.hasProfile());
    assertEquals(PROFILE_HOSPITALIZATION, meta.getProfile().get(0).asStringValue());

    assertEquals(EncounterStatus.INPROGRESS, encounter.getStatus());

    assertTrue(encounter.hasClass_());
    final Coding classCoding = encounter.getClass_();
    assertEquals(CODE_SYSTEM_ACT_CODE, classCoding.getSystem());
    assertEquals("IMP", classCoding.getCode());
    assertEquals("inpatient encounter", classCoding.getDisplay());

    assertTrue(encounter.hasSubject());
    final Reference subject = encounter.getSubject();
    assertTrue(subject.hasReference());
    assertEquals(notifiedPersonShortUrl, subject.getReference());

    assertTrue(encounter.hasServiceProvider());
    final Reference serviceProvider = encounter.getServiceProvider();
    assertTrue(serviceProvider.hasReference());
    assertEquals(notifiedPersonFacilityShortUrl, serviceProvider.getReference());

    return encounter;
  }

  private void checkAddress(Address addressToCheck, FacilityAddressInfo addressInfo) {
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

  private void checkAddress(Address addressToCheck, NotifiedPersonAddressInfo addressInfo) {
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
