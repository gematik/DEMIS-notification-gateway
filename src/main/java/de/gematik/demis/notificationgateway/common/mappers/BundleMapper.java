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

package de.gematik.demis.notificationgateway.common.mappers;

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.STRUCTURE_DEFINITION_ADDRESS_USE;
import static de.gematik.demis.notificationgateway.common.utils.DateUtils.createDate;

import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.NotifiedPersonDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.laboratory.LaboratoryReportDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.laboratory.NotificationLaboratoryDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.laboratory.PathogenDetectionDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.laboratory.SpecimenDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.AddressDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.HumanNameDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.OrganizationBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.PractitionerRoleBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.TelecomDataBuilder;
import de.gematik.demis.notificationgateway.common.dto.ContactPointInfo;
import de.gematik.demis.notificationgateway.common.dto.FacilityAddressInfo;
import de.gematik.demis.notificationgateway.common.dto.FacilityInfo;
import de.gematik.demis.notificationgateway.common.dto.MethodPathogenDTO;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPerson;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPersonAddressInfo;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPersonBasicInfo;
import de.gematik.demis.notificationgateway.common.dto.NotifierFacility;
import de.gematik.demis.notificationgateway.common.dto.PathogenDTO;
import de.gematik.demis.notificationgateway.common.utils.ConfiguredCodeSystems;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Specimen;
import org.jetbrains.annotations.NotNull;

public interface BundleMapper {
  default PractitionerRole createNotifierPractitionerRole(NotifierFacility notifierFacility) {

    final FacilityAddressInfo addressInfo = notifierFacility.getAddress();
    final FacilityInfo facilityInfo = notifierFacility.getFacilityInfo();

    List<ContactPoint> contactPoints =
        notifierFacility.getContacts().stream()
            .map(this::createContactPoint)
            .collect(Collectors.toList());

    OrganizationBuilder organizationBuilder =
        new OrganizationBuilder()
            .asNotifierFacility()
            .setDefaultData()
            .setTypeCode("othPrivatLab")
            .setTypeDisplay("Sonstige private Untersuchungsstelle")
            .setFacilityName(facilityInfo.getInstitutionName())
            .setAddress(createAddress(addressInfo))
            .setTelecomList(contactPoints);

    if (Boolean.TRUE.equals(facilityInfo.getExistsBsnr())) {
      organizationBuilder.setBsnrValue(facilityInfo.getBsnr());
    }

    Organization organization = organizationBuilder.build();

    PractitionerRoleBuilder practitionerRoleBuilder =
        new PractitionerRoleBuilder().asNotifierRole().withOrganization(organization);

    return practitionerRoleBuilder.build();
  }

  default Address createAddress(FacilityAddressInfo addressInfo) {
    if (addressInfo == null) {
      return null;
    }
    return new AddressDataBuilder()
        .setHouseNumber(addressInfo.getHouseNumber())
        .setStreet(addressInfo.getStreet())
        .setCity(addressInfo.getCity())
        .setPostalCode(addressInfo.getZip())
        .setCountry(addressInfo.getCountry())
        .buildAddress();
  }

  default PractitionerRole createSubmitterPractitionerRole(NotifierFacility notifierFacility) {
    final FacilityInfo facilityInfo = notifierFacility.getFacilityInfo();
    final FacilityAddressInfo addressInfo = notifierFacility.getAddress();

    List<ContactPoint> contactPoints =
        notifierFacility.getContacts().stream()
            .map(this::createContactPoint)
            .collect(Collectors.toList());
    final Address submitterAddress = createAddress(addressInfo);
    Organization submittingFacility =
        new OrganizationBuilder()
            .asSubmittingFacility()
            .setDefaultData()
            .setTypeCode("othPrivatLab")
            .setTypeDisplay("Sonstige private Untersuchungsstelle")
            .setFacilityName(facilityInfo.getInstitutionName())
            .setAddress(submitterAddress)
            .setTelecomList(contactPoints)
            .build();

    return new PractitionerRoleBuilder()
        .asSubmittingRole()
        .withOrganization(submittingFacility)
        .build();
  }

  default DiagnosticReport createDiagnosticReport(
      PathogenDTO pathogenDTO, Patient patient, List<Observation> observationList) {

    LaboratoryReportDataBuilder laboratoryReportDataBuilder =
        new LaboratoryReportDataBuilder()
            .setDefaultData()
            .setStatus(
                DiagnosticReport.DiagnosticReportStatus.fromCode(
                    pathogenDTO.getDiagnostic().getReportStatus().getValue()))
            .setCodeCode(pathogenDTO.getCodeDisplay().getCode())
            .setCodeDisplay(pathogenDTO.getCodeDisplay().getDisplay())
            .setIssued(Date.from(Instant.now()))
            .setProfileUrlHelper(pathogenDTO.getCodeDisplay().getCode())
            .setNotifiedPerson(patient)
            .setConclusionCodeStatusToNotDetected()
            .setConclusion(pathogenDTO.getDiagnostic().getInterpretation())
            .setPathogenDetections(observationList);

    for (Observation obs : observationList) {
      for (var interpretation : obs.getInterpretation()) {
        for (var code : interpretation.getCoding()) {
          if (("POS").equals(code.getCode())) {
            laboratoryReportDataBuilder.setConclusionCodeStatusToDetected();
            return laboratoryReportDataBuilder.build();
          }
        }
      }
    }

    return laboratoryReportDataBuilder.build();
  }

  default Composition createComposition(
      Patient patient,
      PractitionerRole practitionerRole,
      DiagnosticReport diagnosticReport,
      PathogenDTO pathogenDTO) {
    Composition.CompositionStatus compositionStatus =
        Composition.CompositionStatus.fromCode(
            pathogenDTO.getDiagnostic().getReportStatus().getValue());
    String notificationLaboratorySectionCompomentyCode = "11502-2";
    String notificationLaboratorySectionCompomentyDisplay = "Laboratory report";

    NotificationLaboratoryDataBuilder builder =
        new NotificationLaboratoryDataBuilder()
            .setDefault()
            .setCompositionStatus(compositionStatus)
            .setTitle("Erregernachweismeldung")
            .setSectionComponentCode(notificationLaboratorySectionCompomentyCode)
            .setSectionComponentDisplay(notificationLaboratorySectionCompomentyDisplay)
            .setNotifiedPerson(patient)
            .setNotifierRole(practitionerRole)
            .setLaboratoryReport(diagnosticReport);

    if (pathogenDTO.getDiagnostic().getInitialNotificationId() != null) {
      builder.setRelatesToNotificationId(pathogenDTO.getDiagnostic().getInitialNotificationId());
    }

    return builder.build();
  }

  default Specimen createSpecimen(
      PathogenDTO pathogenDTO, Patient patient, PractitionerRole submittingRole) {

    final String code = pathogenDTO.getCodeDisplay().getCode();
    SpecimenDataBuilder specimenDataBuilder =
        new SpecimenDataBuilder()
            .setDefaultData()
            //            .setSpecimenStatus(Specimen.SpecimenStatus.AVAILABLE)
            .setProfileUrlHelper(code)
            .setReceivedTime(createDate(pathogenDTO.getDiagnostic().getReceivedDate()))
            .setTypeCode(pathogenDTO.getDiagnostic().getMaterial().getCode())
            .setTypeDisplay(pathogenDTO.getDiagnostic().getMaterial().getDisplay())
            .setNotifiedPerson(patient)
            .setSubmittingRole(submittingRole);

    if (pathogenDTO.getDiagnostic().getExtractionDate() != null) {
      specimenDataBuilder.setCollectedDate(
          createDate(pathogenDTO.getDiagnostic().getExtractionDate()));
    }
    return specimenDataBuilder.build();
  }

  default List<Observation> createObservation(
      PathogenDTO pathogenDTO, Patient patient, Specimen specimen) {
    List<Observation> collect = new ArrayList<>();

    var firstMethodPathogenDTO = pathogenDTO.getDiagnostic().getMethodPathogenList().get(0);

    // Observation 1
    String pathogenCode = pathogenDTO.getCodeDisplay().getCode();
    collect.add(
        createSingleObservation(
            firstMethodPathogenDTO,
            pathogenDTO.getDiagnostic().getPathogen().getCode(),
            pathogenDTO.getDiagnostic().getPathogen().getDisplay(),
            patient,
            specimen,
            pathogenCode));

    // Observation 2 - only if analyt is not null
    if (firstMethodPathogenDTO.getAnalyt() != null) {
      collect.add(
          createSingleObservation(
              firstMethodPathogenDTO,
              firstMethodPathogenDTO.getAnalyt().getCode(),
              firstMethodPathogenDTO.getAnalyt().getDisplay(),
              patient,
              specimen,
              pathogenCode));
    }

    // Observations for all other diagnostik input
    var liste = pathogenDTO.getDiagnostic().getMethodPathogenList();
    for (int i = 1; i < liste.size(); i++) {
      var pdto = liste.get(i);
      String code;
      String display;
      if (pdto.getAnalyt() != null) {
        code = pdto.getAnalyt().getCode();
        display = pdto.getAnalyt().getDisplay();
      } else {
        code = pathogenDTO.getDiagnostic().getPathogen().getCode();
        display = pathogenDTO.getDiagnostic().getPathogen().getDisplay();
      }
      collect.add(createSingleObservation(pdto, code, display, patient, specimen, pathogenCode));
    }
    return collect;
  }

  private Observation createSingleObservation(
      MethodPathogenDTO methodPathogenDTO,
      String valueCode,
      String valueDisplay,
      Patient patient,
      Specimen specimen,
      String pathogenCode) {
    return new PathogenDetectionDataBuilder()
        .setDefaultData()
        .setInterpretationCode(methodPathogenDTO.getResult().getValue())
        .setMethodCode(methodPathogenDTO.getMethod().getCode())
        .setMethodDisplay(methodPathogenDTO.getMethod().getDisplay())
        .setInterpretationCode(methodPathogenDTO.getResult().getValue())
        .setValue(
            new CodeableConcept(new Coding("http://snomed.info/sct", valueCode, valueDisplay)))
        .setObservationCodeCode("41852-5")
        .setObservationCodeDisplay("Microorganism or agent identified in Specimen")
        .setStatus(Observation.ObservationStatus.FINAL)
        .setNotifiedPerson(patient)
        .setSpecimen(specimen)
        .setProfileUrlHelper(pathogenCode)
        .build();
  }

  @NotNull
  default Patient createPatient(NotifiedPerson notifiedPerson) {
    final NotifiedPersonBasicInfo personInfo = notifiedPerson.getInfo();
    final HumanName humanName =
        new HumanNameDataBuilder()
            .addGivenName(personInfo.getFirstname())
            .setFamilyName(personInfo.getLastname())
            .buildHumanName();
    final Enumerations.AdministrativeGender gender =
        Enumerations.AdministrativeGender.valueOf(personInfo.getGender().getValue());
    final Patient patient =
        new NotifiedPersonDataBuilder()
            .setId(UUID.randomUUID().toString())
            .setHumanName(humanName)
            .setGender(gender)
            .setBirthdate(createDate(personInfo.getBirthDate()))
            .addAddress(createAddress(notifiedPerson.getCurrentAddress()))
            .addAddress(createAddress(notifiedPerson.getPrimaryAddress()))
            .addAddress(createAddress(notifiedPerson.getOrdinaryAddress()))
            .buildNotifiedPerson();
    // add contacts
    notifiedPerson.getContacts().stream()
        .map(this::createContactPoint)
        .forEach(patient::addTelecom);

    return patient;
  }

  default ContactPoint createContactPoint(ContactPointInfo contactPointInfo) {
    final ContactPointInfo.UsageEnum usage = contactPointInfo.getUsage();
    return new TelecomDataBuilder()
        .setSystem(
            ContactPoint.ContactPointSystem.fromCode(contactPointInfo.getContactType().getValue()))
        .setValue(contactPointInfo.getValue())
        .setUse(usage == null ? null : ContactPoint.ContactPointUse.fromCode(usage.getValue()))
        .buildContactPoint();
  }

  default Address createAddress(NotifiedPersonAddressInfo personAddress) {
    if (personAddress == null) {
      return null;
    }
    final Address address =
        new AddressDataBuilder()
            .setHouseNumber(personAddress.getHouseNumber())
            .setStreet(personAddress.getStreet())
            .setPostalCode(personAddress.getZip())
            .setCity(personAddress.getCity())
            .setCountry(personAddress.getCountry())
            .setAdditionalInfo(personAddress.getAdditionalInfo())
            .buildAddress();
    Coding addressUse =
        ConfiguredCodeSystems.getInstance()
            .getAddressUseCoding(personAddress.getAddressType().getValue());
    if (addressUse != null) {
      address.addExtension().setUrl(STRUCTURE_DEFINITION_ADDRESS_USE).setValue(addressUse);
    }
    return address;
  }
}
