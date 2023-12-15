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

package de.gematik.demis.notificationgateway.integration.builder;

import com.github.javafaker.Faker;
import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import de.gematik.demis.notificationgateway.common.dto.*;
import de.gematik.demis.notificationgateway.integration.abilities.HospitalizationAbility;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor
public class HospitalizationNotificationBuilder {

  Faker faker;
  FakeValuesService fakeValuesService;

  public static HospitalizationNotificationBuilder build() {
    return new HospitalizationNotificationBuilder();
  }

  public HospitalizationAbility createPositiveHospitalizationNotification() {
    faker = new Faker(new Locale("de"));
    fakeValuesService = new FakeValuesService(new Locale("de"), new RandomService());

    Notification notification = new Notification();

    Hospitalization hospitalization = new Hospitalization();
    hospitalization.setPathogen("cvdp");

    NotifierFacility notifierFacility = new NotifierFacility();

    notifierFacility.setFacilityInfo(fillFacilityInfo());
    notifierFacility.setAddress(fillFacilityAddressInfoStd());
    notifierFacility.setContact(fillPractitionerInfo());

    List<ContactPointInfo> contactList = new ArrayList<>();
    contactList.add(fillContactPointInfoPhone());
    contactList.add(fillContactPointInfoEmail());
    notifierFacility.setContacts(contactList);
    hospitalization.setNotifierFacility(notifierFacility);

    NotifiedPerson notifiedPerson = new NotifiedPerson();
    notifiedPerson.setInfo(fillNotifiedPersonBasicInfoStd());
    notifiedPerson.setPrimaryAddress(fillNotifiedPersonAddressInfoStd());
    hospitalization.setNotifiedPerson(notifiedPerson);

    Disease disease = new Disease();

    disease.setDiseaseInfoCommon(fillDiseaseInfoCommonStd());

    DiseaseInfoCVDD infoCVDD = new DiseaseInfoCVDD();
    infoCVDD.setContactToInfected(DiseaseInfoCVDD.ContactToInfectedEnum.NO);

    infoCVDD.setInfectionEnvironmentQuestion(fillInfectionEnvironmentQuestion());

    VaccinationQuestion vaccinationQuestion = new VaccinationQuestion();
    vaccinationQuestion.setImmunizationStatus(VaccinationQuestion.ImmunizationStatusEnum.YES);

    List<VaccinationInfo> vaccinationList = new ArrayList<>();
    vaccinationList.add(fillVaccinationInfoStd());
    vaccinationQuestion.setVaccinations(vaccinationList);
    infoCVDD.setVaccinationQuestion(vaccinationQuestion);

    disease.setDiseaseInfoCVDD(infoCVDD);

    hospitalization.setDisease(disease);
    notification.setHospitalization(hospitalization);
    //        log.info(hospitalization.toString());
    return new HospitalizationAbility(hospitalization);
  }

  private DiseaseInfoCommon fillDiseaseInfoCommonStd() {
    DiseaseInfoCommon diseaseInfoCommon = new DiseaseInfoCommon();
    diseaseInfoCommon.additionalInfo(faker.lorem().characters(16, 500));
    diseaseInfoCommon.setOrganDonor(DiseaseInfoCommon.OrganDonorEnum.UNKNOWN);

    DeathQuestion deathQuestion = new DeathQuestion();
    deathQuestion.setDead(DeathQuestion.DeadEnum.NO);
    diseaseInfoCommon.setDeathQuestion(deathQuestion);

    diseaseInfoCommon.setExposurePlaceQuestion(fillExposurePlaceQuestionStd());
    diseaseInfoCommon.setHospitalizationQuestion(fillHospitalizationQuestionStd());

    InfectionProtectionFacilityQuestion infectionProtectionFacilityQuestion =
        new InfectionProtectionFacilityQuestion();

    infectionProtectionFacilityQuestion.setInfectionProtectionFacilityInfo(
        fillInfectionProtectionFacilityInfoStd());
    infectionProtectionFacilityQuestion.setInfectionProtectionFacilityExisting(
        InfectionProtectionFacilityQuestion.InfectionProtectionFacilityExistingEnum.INDETERMINATE);

    diseaseInfoCommon.setInfectionProtectionFacilityQuestion(infectionProtectionFacilityQuestion);
    return diseaseInfoCommon;
  }

  private FacilityInfo fillFacilityInfo() {
    FacilityInfo facilityInfo = new FacilityInfo();
    facilityInfo.setExistsBsnr(true);
    facilityInfo.setBsnr(faker.numerify("#########"));
    facilityInfo.setInstitutionName(faker.company().name());
    return facilityInfo;
  }

  private PractitionerInfo fillPractitionerInfo() {
    PractitionerInfo practitionerInfo = new PractitionerInfo();
    practitionerInfo.setFirstname(faker.name().firstName());
    practitionerInfo.setLastname(faker.name().lastName());
    practitionerInfo.setPrefix(faker.name().prefix());
    return practitionerInfo;
  }

  private ContactPointInfo fillContactPointInfoEmail() {
    String email1 = fakeValuesService.bothify("?????????##@gmail.com");
    //        ContactPointInfo contactPointInfo = new
    // ContactPointInfo(ContactPointInfo.ContactTypeEnum.EMAIL, ContactPointInfo.UsageEnum.HOME,
    // email1);
    //        return contactPointInfo;
    return null;
  }

  private ContactPointInfo fillContactPointInfoPhone() {
    //        ContactPointInfo contactPointInfo = new
    // ContactPointInfo(ContactPointInfo.ContactTypeEnum.PHONE, ContactPointInfo.UsageEnum.WORK,
    // faker.phoneNumber().phoneNumber());
    //        return contactPointInfo;
    return null;
  }

  private NotifiedPersonBasicInfo fillNotifiedPersonBasicInfoStd() {
    NotifiedPersonBasicInfo personBasicInfo = new NotifiedPersonBasicInfo();
    personBasicInfo.setFirstname(faker.name().firstName());
    personBasicInfo.setLastname(faker.name().lastName());
    personBasicInfo.setBirthDate(parseDate4Json(faker.date().birthday(1, 102)));
    personBasicInfo.gender(NotifiedPersonBasicInfo.GenderEnum.MALE);

    return personBasicInfo;
  }

  private InfectionEnvironmentQuestion fillInfectionEnvironmentQuestion() {
    InfectionEnvironmentQuestion infectionEnvironmentQuestion = new InfectionEnvironmentQuestion();
    infectionEnvironmentQuestion.setInfectionEnvironmentExisting(
        InfectionEnvironmentQuestion.InfectionEnvironmentExistingEnum.NO);

    InfectionEnvironmentInfo infectionEnvironmentInfo = new InfectionEnvironmentInfo();
    infectionEnvironmentInfo.setInfectionEnvironmentEndDate(
        parseDate4Json(faker.date().past(40, TimeUnit.DAYS)));
    infectionEnvironmentInfo.setInfectionEnvironmentKind(faker.lorem().characters(50, 100));

    infectionEnvironmentInfo.setInfectionEnvironmentEndDate(
        parseDate4Json(faker.date().past(40, TimeUnit.DAYS)));

    infectionEnvironmentQuestion.setInfectionEnvironmentInfo(infectionEnvironmentInfo);

    return infectionEnvironmentQuestion;
  }

  private ExposurePlaceQuestion fillExposurePlaceQuestionStd() {
    ExposurePlaceQuestion exposurePlaceQuestion = new ExposurePlaceQuestion();
    ExposurePlaceInfo exposurePlaceInfo = new ExposurePlaceInfo();
    exposurePlaceInfo.setExposureStartDate(parseDate4Json(faker.date().past(4, TimeUnit.DAYS)));
    exposurePlaceInfo.setExposureEndDate(parseDate4Json(faker.date().past(14, TimeUnit.DAYS)));
    exposurePlaceQuestion.setExposurePlaceInfo(exposurePlaceInfo);
    return exposurePlaceQuestion;
  }

  private HospitalizationQuestion fillHospitalizationQuestionStd() {
    HospitalizationQuestion hospitalizationQuestion = new HospitalizationQuestion();
    hospitalizationQuestion.setHospitalized(HospitalizationQuestion.HospitalizedEnum.NO);

    HospitalizationEncounterInfo hospitalizationEncounterInfo = new HospitalizationEncounterInfo();
    hospitalizationEncounterInfo.setHospitalizationStartDate(
        parseDate4Json(faker.date().past(40, TimeUnit.DAYS)));
    hospitalizationEncounterInfo.setIntensiveCareStartDate(
        parseDate4Json(faker.date().past(14, TimeUnit.DAYS)));
    hospitalizationEncounterInfo.setIntensiveCare(false);
    hospitalizationEncounterInfo.setAdditionalInfo(faker.lorem().characters(100, 500));
    hospitalizationQuestion.setHospitalizedEncounterInfo(hospitalizationEncounterInfo);
    return hospitalizationQuestion;
  }

  private InfectionProtectionFacilityInfo fillInfectionProtectionFacilityInfoStd() {
    InfectionProtectionFacilityInfo infectionProtectionFacilityInfo =
        new InfectionProtectionFacilityInfo();
    infectionProtectionFacilityInfo.setAddress(fillNotifiedPersonAddressInfoStd());
    infectionProtectionFacilityInfo.setFacilityName(faker.company().name());

    ContactsInfo contactsInfo = new ContactsInfo();
    String email2 = fakeValuesService.bothify("?????????##@gmail.com");
    //        contactsInfo.setEmail(new ContactPointInfo(ContactPointInfo.ContactTypeEnum.EMAIL,
    // ContactPointInfo.UsageEnum.HOME, email2));
    infectionProtectionFacilityInfo.setContactsInfo(contactsInfo);
    infectionProtectionFacilityInfo.setStartDate(
        parseDate4Json(faker.date().past(40, TimeUnit.DAYS)));
    infectionProtectionFacilityInfo.setEndDate(
        parseDate4Json(faker.date().past(40, TimeUnit.DAYS)));
    infectionProtectionFacilityInfo.setRole(InfectionProtectionFacilityInfo.RoleEnum.ACCOMMODATION);
    infectionProtectionFacilityInfo.setFacilityName(faker.company().name());
    return infectionProtectionFacilityInfo;
  }

  private VaccinationInfo fillVaccinationInfoStd() {
    VaccinationInfo vaccinationInfo = new VaccinationInfo();
    vaccinationInfo.setVaccinationDate(
        parseDate4Json(faker.date().past(40, TimeUnit.DAYS)).toString());
    vaccinationInfo.additionalInfo(faker.lorem().sentence(20));
    vaccinationInfo.setVaccine(VaccinationInfo.VaccineEnum.MODERNA);
    return vaccinationInfo;
  }

  private NotifiedPersonAddressInfo fillNotifiedPersonAddressInfoStd() {

    NotifiedPersonAddressInfo addressInfo = new NotifiedPersonAddressInfo();
    addressInfo.addressType(AddressType.PRIMARY);
    addressInfo.setCity(faker.address().cityName());
    addressInfo.setCountry(faker.country().name());
    addressInfo.setStreet(faker.address().streetName());
    addressInfo.setHouseNumber(faker.address().buildingNumber());
    addressInfo.setZip(faker.address().zipCode());
    return addressInfo;
  }

  private FacilityAddressInfo fillFacilityAddressInfoStd() {

    FacilityAddressInfo addressInfo = new FacilityAddressInfo();
    addressInfo.addressType(AddressType.PRIMARY);
    addressInfo.setCity(faker.address().cityName());
    addressInfo.setCountry(faker.country().name());
    addressInfo.setStreet(faker.address().streetName());
    addressInfo.setHouseNumber(faker.address().buildingNumber());
    addressInfo.setZip(faker.address().zipCode());
    return addressInfo;
  }

  private void test() {
    //        AddressInfo addressInfo = AddressInfo.builder().build()
  }

  private LocalDate parseDate4Json(Date date) {
    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
  }
}
