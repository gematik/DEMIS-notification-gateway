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

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.CODE_SYSTEM_NULL_FLAVOR;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.CODE_SYSTEM_VACCINE;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.COMMUNITY_REGISTER;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.PROFILE_IMMUNIZATION_INFORMATION_CVDD;
import static de.gematik.demis.notificationgateway.common.constants.MessageConstants.MISSING_VACCINATION_DATE_OF;
import static de.gematik.demis.notificationgateway.common.enums.VaccineCoding.INDETERMINATE_CODING;
import static de.gematik.demis.notificationgateway.common.enums.VaccineCoding.UNKNOWN_CODING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import de.gematik.demis.notificationgateway.common.dto.Hospitalization;
import de.gematik.demis.notificationgateway.common.dto.VaccinationInfo;
import de.gematik.demis.notificationgateway.common.dto.VaccinationQuestion;
import de.gematik.demis.notificationgateway.common.enums.VaccineCoding;
import de.gematik.demis.notificationgateway.common.exceptions.BadRequestException;
import de.gematik.demis.notificationgateway.common.utils.ReferenceUtils;
import de.gematik.demis.notificationgateway.utils.FileUtils;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Immunization.ImmunizationStatus;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ImmunizationCreatorTest {

  private static final Reference NOTIFIED_PERSON =
      ReferenceUtils.createReference(new Patient().setId("123"));
  private final ImmunizationCreator creationService = new ImmunizationCreator();

  @Test
  void testAllSupportedVaccines() throws JsonProcessingException, BadRequestException {
    Hospitalization hospitalization =
        FileUtils.createHospitalization("portal/disease/notification_all_supported_vaccines.json");
    final List<VaccinationInfo> vaccinations =
        hospitalization
            .getDisease()
            .getDiseaseInfoCVDD()
            .getVaccinationQuestion()
            .getVaccinations();

    final List<Immunization> immunizations = creationService.create(vaccinations, NOTIFIED_PERSON);

    final List<String> displays =
        Arrays.stream(VaccineCoding.values())
            .map(VaccineCoding::getCoding)
            .map(Coding::getDisplay)
            .collect(Collectors.toList());
    assertThat(immunizations)
        .isNotEmpty()
        .hasSize(9)
        .extracting("vaccineCode")
        .isNotNull()
        .flatExtracting("coding")
        .isNotEmpty()
        .hasSize(9)
        .extracting("display")
        .asList()
        .containsExactlyInAnyOrderElementsOf(displays);
  }

  @Test
  void testImmunizationContainsGeneralFixedDataFromQuickTest()
      throws JsonProcessingException, BadRequestException {
    Hospitalization hospitalization =
        FileUtils.createHospitalization("portal/disease/notification_content_max.json");
    final List<VaccinationInfo> vaccinations =
        hospitalization
            .getDisease()
            .getDiseaseInfoCVDD()
            .getVaccinationQuestion()
            .getVaccinations();

    final List<Immunization> immunizations = creationService.create(vaccinations, NOTIFIED_PERSON);

    final Predicate<Immunization> profilePredicate =
        immunization ->
            immunization
                .getMeta()
                .getProfile()
                .get(0)
                .asStringValue()
                .equals(PROFILE_IMMUNIZATION_INFORMATION_CVDD);

    final Predicate<Immunization> statusPredicate =
        immunization -> immunization.getStatus().equals(ImmunizationStatus.COMPLETED);

    final Predicate<Immunization> yyyyPredicate =
        immunization -> immunization.getOccurrenceDateTimeType().asStringValue().equals("2021");
    final Predicate<Immunization> mmYYPredicate =
        immunization -> immunization.getOccurrenceDateTimeType().asStringValue().equals("2021-07");
    final Predicate<Immunization> ddMMYYPredicate =
        immunization ->
            immunization.getOccurrenceDateTimeType().asStringValue().equals("2021-11-30");

    assertThat(immunizations)
        .isNotEmpty()
        .hasSize(4)
        .allMatch(Immunization::hasPatient)
        .allMatch(immunization -> immunization.getPatient().hasReference())
        .allMatch(Immunization::hasMeta)
        .allMatch(immunization -> immunization.getMeta().hasProfile())
        .allMatch(profilePredicate)
        .allMatch(Immunization::hasVaccineCode)
        .allMatch(Immunization::hasOccurrence)
        .anyMatch(yyyyPredicate)
        .anyMatch(mmYYPredicate)
        .anyMatch(ddMMYYPredicate)
        .allMatch(Immunization::hasStatus)
        .allMatch(statusPredicate)
        .allMatch(Immunization::hasNote)
        .anySatisfy(
            immunization ->
                assertThat(immunization.getVaccineCode().getCoding())
                    .isNotEmpty()
                    .hasSize(1)
                    .element(0)
                    .hasFieldOrPropertyWithValue("system", COMMUNITY_REGISTER)
                    .hasFieldOrPropertyWithValue("code", "EU/1/20/1528")
                    .hasFieldOrPropertyWithValue("display", "Comirnaty"))
        .anySatisfy(
            immunization ->
                assertThat(immunization.getNote())
                    .isNotEmpty()
                    .hasSize(1)
                    .element(0)
                    .hasFieldOrPropertyWithValue("text", "Zusatzinfo1"));
  }

  @Test
  void givenUnknownVaccinationWhenCreateThenUnknownImmunization()
      throws JsonProcessingException, BadRequestException {

    final String inputFile =
        "portal/disease/immunization/notification_content_vaccine_unknown.json";
    Hospitalization hospitalization = FileUtils.createHospitalization(inputFile);
    final List<VaccinationInfo> vaccinations =
        hospitalization
            .getDisease()
            .getDiseaseInfoCVDD()
            .getVaccinationQuestion()
            .getVaccinations();

    final List<Immunization> immunizations = creationService.create(vaccinations, NOTIFIED_PERSON);

    final Predicate<Immunization> ddMMYYPredicate =
        immunization ->
            immunization.getOccurrenceDateTimeType().asStringValue().equals("2021-05-28");
    final Predicate<Immunization> statusPredicate =
        immunization -> immunization.getStatus().equals(ImmunizationStatus.COMPLETED);

    assertThat(immunizations)
        .isNotEmpty()
        .hasSize(1)
        .allMatch(Immunization::hasPatient)
        .allMatch(immunization -> immunization.getPatient().hasReference())
        .allMatch(Immunization::hasMeta)
        .allMatch(immunization -> immunization.getMeta().hasProfile())
        .allMatch(Immunization::hasVaccineCode)
        .allMatch(Immunization::hasOccurrence)
        .allMatch(ddMMYYPredicate)
        .allMatch(Immunization::hasStatus)
        .allMatch(statusPredicate)
        .allMatch(Immunization::hasNote)
        .anySatisfy(
            immunization ->
                assertThat(immunization.getVaccineCode().getCoding())
                    .isNotEmpty()
                    .hasSize(1)
                    .element(0)
                    .hasFieldOrPropertyWithValue("system", CODE_SYSTEM_NULL_FLAVOR)
                    .hasFieldOrPropertyWithValue("code", "NASK")
                    .hasFieldOrPropertyWithValue("display", "not asked"))
        .anySatisfy(
            immunization ->
                assertThat(immunization.getNote())
                    .isNotEmpty()
                    .hasSize(1)
                    .element(0)
                    .hasFieldOrPropertyWithValue("text", "Zusatzinfo1"));
  }

  @Test
  void givenBlankVaccinationsWhenCreateThenNoException() throws Exception {
    List<Immunization> immunizations = creationService.create(List.of(), NOTIFIED_PERSON);
    assertThat(immunizations).isEmpty();
  }

  @Test
  void givenNotCommunityRegisterVaccineWithoutDateWhenCreateThenVaccineIgnored() throws Exception {
    final String inputFile =
        "portal/disease/notification-not-community-register-vaccination-without-date.json";
    Hospitalization hospitalization = FileUtils.createHospitalization(inputFile);
    final VaccinationQuestion vaccinationQuestion =
        hospitalization.getDisease().getDiseaseInfoCVDD().getVaccinationQuestion();
    final List<VaccinationInfo> vaccinations = vaccinationQuestion.getVaccinations();

    final List<Immunization> immunizations = creationService.create(vaccinations, NOTIFIED_PERSON);

    assertThat(vaccinationQuestion.getImmunizationStatus())
        .isEqualByComparingTo(VaccinationQuestion.ImmunizationStatusEnum.YES);
    assertThat(immunizations).isEmpty();
  }

  @Test
  void givenMissingVaccinationsWhenCreateThenNoError() throws Exception {
    String inputFile = "portal/disease/notification-missing-vaccinations-use-case.json";
    Hospitalization hospitalization = FileUtils.createHospitalization(inputFile);
    final VaccinationQuestion vaccinationQuestion =
        hospitalization.getDisease().getDiseaseInfoCVDD().getVaccinationQuestion();
    final List<VaccinationInfo> vaccinations = vaccinationQuestion.getVaccinations();

    final List<Immunization> immunizations = creationService.create(vaccinations, NOTIFIED_PERSON);

    assertThat(vaccinationQuestion.getImmunizationStatus())
        .isEqualByComparingTo(VaccinationQuestion.ImmunizationStatusEnum.YES);
    assertThat(immunizations).isEmpty();
  }

  @Test
  void givenNotCommunityRegisterVaccineWithDateWhenCreateThenVaccineNotIgnored() throws Exception {
    String inputFile =
        "portal/disease/notification-not-community-register-vaccination-with-date.json";
    Hospitalization hospitalization = FileUtils.createHospitalization(inputFile);
    final VaccinationQuestion vaccinationQuestion =
        hospitalization.getDisease().getDiseaseInfoCVDD().getVaccinationQuestion();
    final List<VaccinationInfo> vaccinations = vaccinationQuestion.getVaccinations();

    final List<Immunization> immunizations = creationService.create(vaccinations, NOTIFIED_PERSON);

    assertThat(vaccinationQuestion.getImmunizationStatus())
        .isEqualByComparingTo(VaccinationQuestion.ImmunizationStatusEnum.YES);
    assertThat(immunizations)
        .isNotEmpty()
        .hasSize(2)
        .extracting(Immunization::getVaccineCode)
        .flatExtracting(CodeableConcept::getCoding)
        .isNotEmpty()
        .hasSize(2)
        .extracting("display")
        .containsExactlyInAnyOrder(
            UNKNOWN_CODING.getCoding().getDisplay(), INDETERMINATE_CODING.getCoding().getDisplay());
  }

  @ParameterizedTest
  @MethodSource("provideVaccinationDates")
  void testPartialDate(
      String inputFile, String expectedDate, TemporalPrecisionEnum expectedPrecision)
      throws JsonProcessingException, BadRequestException {
    Hospitalization hospitalization = FileUtils.createHospitalization(inputFile);
    final List<VaccinationInfo> vaccinations =
        hospitalization
            .getDisease()
            .getDiseaseInfoCVDD()
            .getVaccinationQuestion()
            .getVaccinations();

    final List<Immunization> immunizations = creationService.create(vaccinations, NOTIFIED_PERSON);

    assertThat(immunizations)
        .isNotEmpty()
        .hasSize(1)
        .element(0)
        .extracting("occurrence")
        .isNotNull()
        .hasFieldOrPropertyWithValue("myPrecision", expectedPrecision)
        .hasFieldOrPropertyWithValue("myStringValue", expectedDate);
  }

  @ParameterizedTest
  @MethodSource("provideVaccineValues")
  void testVaccines(
      String inputFile, String expectedSystem, String expectedCode, String expectedDisplay)
      throws JsonProcessingException, BadRequestException {
    Hospitalization hospitalization = FileUtils.createHospitalization(inputFile);
    final List<VaccinationInfo> vaccinations =
        hospitalization
            .getDisease()
            .getDiseaseInfoCVDD()
            .getVaccinationQuestion()
            .getVaccinations();

    final List<Immunization> immunizations = creationService.create(vaccinations, NOTIFIED_PERSON);

    assertThat(immunizations)
        .isNotEmpty()
        .hasSize(1)
        .extracting("vaccineCode")
        .isNotNull()
        .flatExtracting("coding")
        .isNotEmpty()
        .hasSize(1)
        .element(0)
        .hasFieldOrPropertyWithValue("system", expectedSystem)
        .hasFieldOrPropertyWithValue("code", expectedCode)
        .hasFieldOrPropertyWithValue("display", expectedDisplay);
  }

  @ParameterizedTest
  @MethodSource("provideInvalidVaccinationDates")
  void testThrowsExceptionWhenDateIsInvalid(
      VaccinationInfo vaccination, String expectedErrorMessage) {

    assertThatThrownBy(() -> creationService.create(List.of(vaccination), NOTIFIED_PERSON))
        .isInstanceOf(BadRequestException.class)
        .hasMessage(expectedErrorMessage);
  }

  @SuppressWarnings("unused")
  private static Stream<Arguments> provideInvalidVaccinationDates() {
    final VaccinationInfo vaccinationEmptyDate = new VaccinationInfo();
    vaccinationEmptyDate.setVaccine(VaccinationInfo.VaccineEnum.COMIRNATY);
    vaccinationEmptyDate.setVaccinationDate("");

    final VaccinationInfo vaccinationMissingDate = new VaccinationInfo();
    vaccinationMissingDate.setVaccine(VaccinationInfo.VaccineEnum.COMIRNATY);

    final VaccinationInfo vaccinationWrongDateLength = new VaccinationInfo();
    vaccinationWrongDateLength.setVaccine(VaccinationInfo.VaccineEnum.COMIRNATY);
    vaccinationWrongDateLength.setVaccinationDate("2022-03-31T12:00:46.384Z");

    final VaccinationInfo vaccinationWrongDate = new VaccinationInfo();
    vaccinationWrongDate.setVaccine(VaccinationInfo.VaccineEnum.COMIRNATY);
    vaccinationWrongDate.setVaccinationDate("wrong-d");

    final VaccinationInfo vaccinationFutureDate = new VaccinationInfo();
    vaccinationFutureDate.setVaccine(VaccinationInfo.VaccineEnum.COMIRNATY);
    final LocalDate tomorrow = LocalDate.now().plusDays(1);
    vaccinationFutureDate.setVaccinationDate(tomorrow.toString());

    return Stream.of(
        Arguments.of(
            vaccinationMissingDate,
            MISSING_VACCINATION_DATE_OF + vaccinationMissingDate.getVaccine()),
        Arguments.of(
            vaccinationEmptyDate, MISSING_VACCINATION_DATE_OF + vaccinationEmptyDate.getVaccine()),
        Arguments.of(vaccinationWrongDateLength, "invalid date length"),
        Arguments.of(vaccinationFutureDate, "vaccination date must not be in the future"),
        Arguments.of(
            vaccinationWrongDate,
            "invalid vaccination date: " + vaccinationWrongDate.getVaccinationDate()));
  }

  @SuppressWarnings("unused")
  private static Stream<Arguments> provideVaccineValues() {
    return Stream.of(
        Arguments.of(
            "portal/disease/immunization/notification_content_vaccine_moderna.json",
            COMMUNITY_REGISTER,
            "EU/1/20/1507",
            "Spikevax (COVID-19 Vaccine Moderna)"),
        Arguments.of(
            "portal/disease/immunization/notification_content_vaccine_janssen.json",
            COMMUNITY_REGISTER,
            "EU/1/20/1525",
            "COVID-19 Vaccine Janssen"),
        Arguments.of(
            "portal/disease/immunization/notification_content_vaccine_comirnaty.json",
            COMMUNITY_REGISTER,
            "EU/1/20/1528",
            "Comirnaty"),
        Arguments.of(
            "portal/disease/immunization/notification_content_vaccine_vaxzevria.json",
            COMMUNITY_REGISTER,
            "EU/1/21/1529",
            "Vaxzevria (COVID-19 Vaccine AstraZeneca)"),
        Arguments.of(
            "portal/disease/immunization/notification_content_vaccine_nuvaxovid.json",
            COMMUNITY_REGISTER,
            "EU/1/21/1618",
            "Nuvaxovid (NVX-CoV2373)"),
        Arguments.of(
            "portal/disease/immunization/notification_content_vaccine_valneva.json",
            COMMUNITY_REGISTER,
            "EU/1/21/1624",
            "COVID-19 Vaccine (inactivated, adjuvanted) Valneva"),
        Arguments.of(
            "portal/disease/immunization/notification_content_vaccine_other.json",
            CODE_SYSTEM_VACCINE,
            "otherVaccine",
            "Anderer Impfstoff"),
        Arguments.of(
            "portal/disease/immunization/notification_content_vaccine_indeterminate.json",
            CODE_SYSTEM_NULL_FLAVOR,
            "ASKU",
            "asked but unknown"),
        Arguments.of(
            "portal/disease/immunization/notification_content_vaccine_unknown.json",
            CODE_SYSTEM_NULL_FLAVOR,
            "NASK",
            "not asked"));
  }

  @SuppressWarnings("unused")
  private static Stream<Arguments> provideVaccinationDates() {
    return Stream.of(
        Arguments.of(
            "portal/disease/immunization/notification_content_complete_date.json",
            "2021-05-28",
            TemporalPrecisionEnum.DAY),
        Arguments.of(
            "portal/disease/immunization/notification_content_partial_date_year_month.json",
            "2021-05",
            TemporalPrecisionEnum.MONTH),
        Arguments.of(
            "portal/disease/immunization/notification_content_partial_date_year.json",
            "2021",
            TemporalPrecisionEnum.YEAR));
  }
}
