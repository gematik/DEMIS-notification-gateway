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
import de.gematik.demis.notification.builder.demis.fhir.notification.utils.Utils;
import de.gematik.demis.notificationgateway.common.dto.AddressType;
import de.gematik.demis.notificationgateway.common.dto.ContactPointInfo;
import de.gematik.demis.notificationgateway.common.dto.FacilityAddressInfo;
import de.gematik.demis.notificationgateway.common.dto.FacilityInfo;
import de.gematik.demis.notificationgateway.common.dto.MethodPathogenDTO;
import de.gematik.demis.notificationgateway.common.dto.NotificationLaboratoryCategory;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPerson;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPersonAddressInfo;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPersonBasicInfo;
import de.gematik.demis.notificationgateway.common.dto.NotifierFacility;
import de.gematik.demis.notificationgateway.common.dto.PathogenDTO;
import de.gematik.demis.notificationgateway.common.dto.PractitionerInfo;
import de.gematik.demis.notificationgateway.common.dto.ResistanceDTO;
import de.gematik.demis.notificationgateway.common.dto.ResistanceGeneDTO;
import de.gematik.demis.notificationgateway.common.dto.SubmitterFacility;
import de.gematik.demis.notificationgateway.common.dto.SubmittingFacilityInfo;
import de.gematik.demis.notificationgateway.common.utils.ConfiguredCodeSystems;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
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

public interface BundleMapper {
  default PractitionerRole createNotifierPractitionerRole(NotifierFacility notifierFacility) {

    final FacilityAddressInfo addressInfo = notifierFacility.getAddress();
    final FacilityInfo facilityInfo = notifierFacility.getFacilityInfo();
    final PractitionerInfo practitionerInfo = notifierFacility.getContact();

    List<ContactPoint> contactPoints =
        notifierFacility.getContacts().stream().map(this::createContactPoint).toList();

    final HumanName contactName = createHumanName(practitionerInfo);

    OrganizationBuilder organizationBuilder =
        new OrganizationBuilder()
            .asNotifierFacility()
            .setDefaults()
            .setTypeCode("othPrivatLab")
            .setTypeDisplay("Sonstige private Untersuchungsstelle")
            .setFacilityName(facilityInfo.getInstitutionName())
            .setAddress(createAddress(addressInfo))
            .setTelecomList(contactPoints)
            .addContact(new Organization.OrganizationContactComponent().setName(contactName));

    if (Boolean.TRUE.equals(facilityInfo.getExistsBsnr())) {
      organizationBuilder.setBsnrValue(facilityInfo.getBsnr());
    }

    Organization organization = organizationBuilder.build();

    PractitionerRoleBuilder practitionerRoleBuilder =
        new PractitionerRoleBuilder().asNotifierRole().withOrganization(organization);

    return practitionerRoleBuilder.build();
  }

  private HumanName createHumanName(final PractitionerInfo practitionerInfo) {
    final HumanNameDataBuilder humanNameDataBuilder =
        new HumanNameDataBuilder()
            .setFamilyName(practitionerInfo.getLastname())
            .addGivenName(practitionerInfo.getFirstname())
            .addPrefix(practitionerInfo.getPrefix());
    findSalutation(practitionerInfo).ifPresent(humanNameDataBuilder::setSalutation);
    return humanNameDataBuilder.build();
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
        .build();
  }

  default PractitionerRole createSubmitterPractitionerRole(SubmitterFacility submitterFacility) {
    return createSubmitterPractitionerRole(submitterFacility, false);
  }

  default PractitionerRole createSubmitterPractitionerRole(
      SubmitterFacility submitterFacility, boolean isNotifiedPersonFacility) {
    final SubmittingFacilityInfo facilityInfo = submitterFacility.getFacilityInfo();
    final FacilityAddressInfo addressInfo = submitterFacility.getAddress();

    List<ContactPoint> contactPoints =
        submitterFacility.getContacts().stream().map(this::createContactPoint).toList();
    final Address submitterAddress = createAddress(addressInfo);
    final HumanName contactName = createHumanName(submitterFacility.getContact());
    final OrganizationBuilder organizationBuilder =
        new OrganizationBuilder()
            .asSubmittingFacility()
            .setSubmitterDetails(contactName, facilityInfo.getDepartmentName())
            .setDefaults()
            .setTypeCode("othPrivatLab")
            .setTypeDisplay("Sonstige private Untersuchungsstelle")
            .setFacilityName(facilityInfo.getInstitutionName())
            .setAddress(submitterAddress)
            .setTelecomList(contactPoints);
    if (isNotifiedPersonFacility) {
      organizationBuilder.addNotifiedPersonFacilityProfile();
    }

    Organization submittingFacility = organizationBuilder.build();

    return new PractitionerRoleBuilder()
        .asSubmittingRole()
        .withOrganization(submittingFacility)
        .build();
  }

  default DiagnosticReport createDiagnosticReport(
      PathogenDTO pathogenDTO,
      Patient patient,
      List<Observation> observationList,
      boolean useNewNotificationPreparationCode,
      NotificationLaboratoryCategory notificationCategory) {

    String value;
    String conclusionCode;
    String laboratoryOrderId;
    if (useNewNotificationPreparationCode) {
      value = notificationCategory.getReportStatus().getValue();
      conclusionCode = notificationCategory.getInterpretation();
      laboratoryOrderId = notificationCategory.getLaboratoryOrderId();
    } else {
      value = pathogenDTO.getDiagnostic().getReportStatus().getValue();
      conclusionCode = pathogenDTO.getDiagnostic().getInterpretation();
      laboratoryOrderId = pathogenDTO.getDiagnostic().getLaboratoryOrderId();
    }

    LaboratoryReportDataBuilder laboratoryReportDataBuilder =
        new LaboratoryReportDataBuilder()
            .setDefaultData()
            .setStatus(DiagnosticReport.DiagnosticReportStatus.fromCode(value))
            .setCodeCode(pathogenDTO.getCodeDisplay().getCode())
            .setCodeDisplay(pathogenDTO.getCodeDisplay().getDisplay())
            .setIssued(Utils.getCurrentDate())
            .setProfileUrlHelper(pathogenDTO.getCodeDisplay().getCode())
            .setNotifiedPerson(patient)
            .setConclusionCodeStatusToNotDetected()
            .setConclusion(conclusionCode)
            .setPathogenDetections(observationList);

    if (laboratoryOrderId != null && !laboratoryOrderId.isEmpty()) {
      laboratoryReportDataBuilder.addBasedOn(laboratoryOrderId);
    }

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
      PathogenDTO pathogenDTO,
      boolean useNewNotificationPreparationCode,
      NotificationLaboratoryCategory notificationCategory) {

    Composition.CompositionStatus compositionStatus;
    if (useNewNotificationPreparationCode) {
      compositionStatus =
          Composition.CompositionStatus.fromCode(notificationCategory.getReportStatus().getValue());
    } else {
      compositionStatus =
          Composition.CompositionStatus.fromCode(
              pathogenDTO.getDiagnostic().getReportStatus().getValue());
    }
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

    String initialNotificationId;
    if (useNewNotificationPreparationCode) {
      initialNotificationId = notificationCategory.getInitialNotificationId();
    } else {
      initialNotificationId = pathogenDTO.getDiagnostic().getInitialNotificationId();
    }
    if (initialNotificationId != null) {
      builder.setRelatesToNotificationId(initialNotificationId);
    }

    return builder.build();
  }

  default Specimen createSpecimen(
      PathogenDTO pathogenDTO, Patient patient, PractitionerRole submittingRole) {

    final String code = pathogenDTO.getCodeDisplay().getCode();
    SpecimenDataBuilder specimenDataBuilder =
        new SpecimenDataBuilder()
            .setDefaultData()
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
      PathogenDTO pathogenDTO,
      Patient patient,
      Specimen specimen,
      boolean useNewNotificationPreparationCode,
      boolean multipleSpecimen,
      NotificationLaboratoryCategory notificationCategory) {
    var firstMethodPathogenDTO = pathogenDTO.getDiagnostic().getMethodPathogenList();

    return createObservation(
        pathogenDTO,
        firstMethodPathogenDTO,
        patient,
        specimen,
        useNewNotificationPreparationCode,
        multipleSpecimen,
        notificationCategory);
  }

  default List<Observation> createObservation(
      PathogenDTO pathogenDTO,
      List<MethodPathogenDTO> methodPathogenDTOList,
      Patient patient,
      Specimen specimen,
      boolean useNewNotificationPreparationCode,
      boolean multipleSpecimen,
      NotificationLaboratoryCategory notificationCategory) {
    List<Observation> collect = new ArrayList<>();
    String pathogenCode;
    String pathogenDisplay;
    if (useNewNotificationPreparationCode) {
      pathogenCode = notificationCategory.getPathogen().getCode();
      pathogenDisplay = notificationCategory.getPathogen().getDisplay();
    } else {
      pathogenCode = pathogenDTO.getDiagnostic().getPathogen().getCode();
      pathogenDisplay = pathogenDTO.getDiagnostic().getPathogen().getDisplay();
    }
    // Observation 1
    String pathogenShortCode = pathogenDTO.getCodeDisplay().getCode();
    MethodPathogenDTO firstMethodPathogenDTO = methodPathogenDTOList.getFirst();
    collect.add(
        createSingleObservation(
            firstMethodPathogenDTO,
            pathogenCode,
            pathogenDisplay,
            patient,
            specimen,
            pathogenShortCode));

    // Observation 2 - only if analyt is not null
    if (firstMethodPathogenDTO.getAnalyt() != null) {
      collect.add(
          createSingleObservation(
              firstMethodPathogenDTO,
              firstMethodPathogenDTO.getAnalyt().getCode(),
              firstMethodPathogenDTO.getAnalyt().getDisplay(),
              patient,
              specimen,
              pathogenShortCode));
    }

    // Observations for all other diagnostik input
    for (int i = 1; i < methodPathogenDTOList.size(); i++) {
      var pdto = methodPathogenDTOList.get(i);
      String code;
      String display;
      if (pdto.getAnalyt() != null) {
        code = pdto.getAnalyt().getCode();
        display = pdto.getAnalyt().getDisplay();
      } else {
        code = pathogenCode;
        display = pathogenDisplay;
      }
      collect.add(
          createSingleObservation(pdto, code, display, patient, specimen, pathogenShortCode));
    }

    // TODO remove this hole block when removing feature.flag.multiple_specimen_enabled
    if (!multipleSpecimen) {
      // Observations for resistance genes
      createObservationsForResistanceGenes(
          pathogenDTO.getDiagnostic().getResistanceGeneList(),
          patient,
          specimen,
          collect,
          pathogenShortCode);
      createObservationsForResistances(
          pathogenDTO.getDiagnostic().getResistanceList(),
          patient,
          specimen,
          collect,
          pathogenShortCode);
    }

    return collect;
  }

  default void createObservationsForResistanceGenes(
      List<ResistanceGeneDTO> resistanceGenes,
      Patient patient,
      Specimen specimen,
      List<Observation> observationList,
      String pathogenCode) {
    if (resistanceGenes == null || resistanceGenes.isEmpty()) {
      return;
    }
    for (var resistanceGene : resistanceGenes) {
      String code = resistanceGene.getResistanceGene().getCode();
      String display = resistanceGene.getResistanceGene().getDisplay();

      String interpretation = "";
      String valueCode = "";
      String valueDisplay = "";

      var interpretationEnum = resistanceGene.getResistanceGeneResult();
      switch (interpretationEnum) {
        case DETECTED -> {
          interpretation = "R";
          valueCode = "260373001";
        }
        case NOT_DETECTED -> {
          interpretation = "S";
          valueCode = "260415000";
        }
        case INDETERMINATE -> {
          interpretation = "IND";
          valueCode = "82334004";
        }
      }

      observationList.add(
          new PathogenDetectionDataBuilder()
              .setDefaultData()
              .setMethodCode("116148004")
              .setMethodDisplay("Molecular genetic procedure (procedure)")
              .setInterpretationCode(interpretation)
              .setValue(
                  new CodeableConcept(
                      new Coding("http://snomed.info/sct", valueCode, valueDisplay)))
              .setNotifiedPerson(patient)
              .setSpecimen(specimen)
              .setProfileUrlHelper(pathogenCode)
              .setObservationCodeCode(code)
              .setObservationCodeDisplay(display)
              .setStatus(Observation.ObservationStatus.FINAL)
              .build());
    }
  }

  default void createObservationsForResistances(
      List<ResistanceDTO> resistances,
      Patient patient,
      Specimen specimen,
      List<Observation> observationList,
      String pathogenCode) {
    if (resistances == null || resistances.isEmpty()) {
      return;
    }
    for (var resistance : resistances) {
      String code = resistance.getResistance().getCode();
      String display = resistance.getResistance().getDisplay();
      String interpretation = "";
      String valueCode = "";
      String valueDisplay = "";

      var interpretationEnum = resistance.getResistanceResult();
      switch (interpretationEnum) {
        case RESISTANT -> {
          interpretation = "R";
          valueCode = "30714006";
        }
        case SUSCEPTIBLE_WITH_INCREASED_EXPOSURE -> {
          interpretation = "I";
          valueCode = "1255965005";
        }
        case INTERMEDIATE -> {
          interpretation = "I";
          valueCode = "11896004";
        }
        case SUSCEPTIBLE -> {
          interpretation = "S";
          valueCode = "131196009";
        }
        case INDETERMINATE -> {
          interpretation = "IND";
          valueCode = "82334004";
        }
      }

      observationList.add(
          new PathogenDetectionDataBuilder()
              .setDefaultData()
              .setMethodCode("14788002")
              .setMethodDisplay("Antimicrobial susceptibility test (procedure)")
              .setInterpretationCode(interpretation)
              .setValue(
                  new CodeableConcept(
                      new Coding("http://snomed.info/sct", valueCode, valueDisplay)))
              .setNotifiedPerson(patient)
              .setSpecimen(specimen)
              .setProfileUrlHelper(pathogenCode)
              .setObservationCodeCode(code)
              .setObservationCodeDisplay(display)
              .setStatus(Observation.ObservationStatus.FINAL)
              .build());
    }
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

  default Patient createPatient(
      final NotifiedPerson notifiedPerson, final List<Address> manualAddresses) {
    final NotifiedPersonDataBuilder result = new NotifiedPersonDataBuilder();
    result.setId(Utils.generateUuidString());

    final NotifiedPersonBasicInfo personInfo = notifiedPerson.getInfo();
    result.setBirthdate(createDate(personInfo.getBirthDate()));

    final HumanName humanName =
        new HumanNameDataBuilder()
            .addGivenName(personInfo.getFirstname())
            .setFamilyName(personInfo.getLastname())
            .build();
    result.setHumanName(humanName);

    final Enumerations.AdministrativeGender gender =
        Enumerations.AdministrativeGender.valueOf(personInfo.getGender().getValue());
    result.setGender(gender);

    manualAddresses.stream().filter(Objects::nonNull).forEach(result::addAddress);

    // add contacts
    notifiedPerson.getContacts().stream().map(this::createContactPoint).forEach(result::addTelecom);

    return result.build();
  }

  default Patient createPatient(NotifiedPerson notifiedPerson) {
    // Some of the addresses might be null, so we can't use List.of()
    final List<Address> automaticAddresses =
        Stream.of(
                createAddress(notifiedPerson.getCurrentAddress()),
                createAddress(notifiedPerson.getPrimaryAddress()),
                createAddress(notifiedPerson.getOrdinaryAddress()))
            .filter(Objects::nonNull)
            .toList();
    return createPatient(notifiedPerson, automaticAddresses);
  }

  default ContactPoint createContactPoint(ContactPointInfo contactPointInfo) {
    final ContactPointInfo.UsageEnum usage = contactPointInfo.getUsage();
    return new TelecomDataBuilder()
        .setSystem(
            ContactPoint.ContactPointSystem.fromCode(contactPointInfo.getContactType().getValue()))
        .setValue(contactPointInfo.getValue())
        .setUse(usage == null ? null : ContactPoint.ContactPointUse.fromCode(usage.getValue()))
        .build();
  }

  default Address createAddress(NotifiedPersonAddressInfo personAddress) {
    if (personAddress == null) {
      return null;
    }

    final Address address;
    final AddressDataBuilder addressDataBuilder =
        new AddressDataBuilder()
            .setHouseNumber(personAddress.getHouseNumber())
            .setStreet(personAddress.getStreet())
            .setPostalCode(personAddress.getZip())
            .setCity(personAddress.getCity())
            .setCountry(personAddress.getCountry());

    switch (personAddress.getAddressType()) {
      case SUBMITTING_FACILITY:
      case PRIMARY_AS_CURRENT:
      case CURRENT:
      case PRIMARY:
      case ORDINARY:
        addressDataBuilder.setAdditionalInfo(personAddress.getAdditionalInfo());
        break;
      case OTHER_FACILITY:
        // NO-OP we don't want additional info for other facility, this is where we store the
        // name of the facility...
        break;
      default:
        throw new IllegalStateException("Unexpected value: " + personAddress.getAddressType());
    }
    address = addressDataBuilder.build();
    final Coding addressUse = getAddressUseCoding(personAddress.getAddressType());
    if (addressUse != null) {
      address.addExtension().setUrl(STRUCTURE_DEFINITION_ADDRESS_USE).setValue(addressUse);
    }

    return address;
  }

  default Address createAddressWithoutAddressUse(NotifiedPersonAddressInfo personAddress) {
    final Address address = createAddress(personAddress);
    address.getExtension().clear();
    return address;
  }

  /** Create an address referencing the given organization */
  default Address createAddress(
      final NotifiedPersonAddressInfo addressInfo, final Organization organization) {
    final Address result =
        new AddressDataBuilder().withOrganizationReferenceExtension(organization).build();
    final Coding addressUse = getAddressUseCoding(addressInfo.getAddressType());
    if (addressUse != null) {
      result.addExtension().setUrl(STRUCTURE_DEFINITION_ADDRESS_USE).setValue(addressUse);
    }

    return result;
  }

  /** Translate front-end address types to the internal code system */
  private static Coding getAddressUseCoding(final AddressType addressType) {
    AddressType.fromValue(addressType.getValue());
    final String referencedCodingValue =
        switch (addressType) {
          case SUBMITTING_FACILITY, OTHER_FACILITY, PRIMARY_AS_CURRENT -> "current";
          default -> addressType.getValue();
        };

    return ConfiguredCodeSystems.getInstance().getAddressUseCoding(referencedCodingValue);
  }

  private Optional<HumanNameDataBuilder.Salutation> findSalutation(PractitionerInfo contact) {
    final PractitionerInfo.SalutationEnum salutation = contact.getSalutation();
    if (salutation != null) {
      switch (salutation) {
        case MR:
          return Optional.of(HumanNameDataBuilder.Salutation.MR);
        case MRS:
          return Optional.of(HumanNameDataBuilder.Salutation.MRS);
        default:
          throw new IllegalArgumentException("unknown salutation type: " + salutation);
      }
    }
    return Optional.empty();
  }
}
