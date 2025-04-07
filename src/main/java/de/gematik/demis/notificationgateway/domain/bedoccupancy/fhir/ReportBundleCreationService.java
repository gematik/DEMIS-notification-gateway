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

import de.gematik.demis.notification.builder.demis.fhir.notification.builder.NotifierDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.reports.ReportBedOccupancyDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.reports.ReportBundleDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.reports.StatisticInformationBedOccupancyDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.AddressDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.HumanNameDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.HumanNameDataBuilder.Salutation;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.TelecomDataBuilder;
import de.gematik.demis.notificationgateway.common.dto.BedOccupancy;
import de.gematik.demis.notificationgateway.common.dto.BedOccupancyNotifierFacility;
import de.gematik.demis.notificationgateway.common.dto.BedOccupancyQuestion;
import de.gematik.demis.notificationgateway.common.dto.ContactPointInfo;
import de.gematik.demis.notificationgateway.common.dto.ContactPointInfo.UsageEnum;
import de.gematik.demis.notificationgateway.common.dto.FacilityAddressInfo;
import de.gematik.demis.notificationgateway.common.dto.OccupiedBeds;
import de.gematik.demis.notificationgateway.common.dto.OperableBeds;
import de.gematik.demis.notificationgateway.common.dto.PractitionerInfo;
import de.gematik.demis.notificationgateway.common.dto.PractitionerInfo.SalutationEnum;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointUse;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.springframework.stereotype.Service;

@Service
public class ReportBundleCreationService {

  private static QuestionnaireResponse createQuestionnaireResponse(BedOccupancy bedOccupancy) {
    final BedOccupancyQuestion bedOccupancyQuestion = bedOccupancy.getBedOccupancyQuestion();
    final OccupiedBeds occupiedBeds = bedOccupancyQuestion.getOccupiedBeds();
    final var builder = new StatisticInformationBedOccupancyDataBuilder();
    builder.setDefaults();
    builder.setNumberOccupiedBedsGeneralWardAdultsValue(occupiedBeds.getAdultsNumberOfBeds());
    builder.setNumberOccupiedBedsGeneralWardChildrenValue(occupiedBeds.getChildrenNumberOfBeds());
    setOperableBeds(bedOccupancyQuestion, builder);
    return builder.build();
  }

  private static void setOperableBeds(
      BedOccupancyQuestion bedOccupancyQuestion,
      StatisticInformationBedOccupancyDataBuilder builder) {
    final OperableBeds operableBeds = bedOccupancyQuestion.getOperableBeds();
    if (operableBeds != null) {
      final Integer adultsNumberOfOperableBeds = operableBeds.getAdultsNumberOfBeds();
      final Integer childrenNumberOfOperableBeds = operableBeds.getChildrenNumberOfBeds();
      if (adultsNumberOfOperableBeds != null) {
        builder.setNumberOperableBedsGeneralWardAdultsValue(adultsNumberOfOperableBeds);
      }
      if (childrenNumberOfOperableBeds != null) {
        builder.setNumberOperableBedsGeneralWardChildrenValue(childrenNumberOfOperableBeds);
      }
    }
  }

  private static PractitionerRole createNotifierRole(BedOccupancy bedOccupancy) {
    final BedOccupancyNotifierFacility notifierFacility = bedOccupancy.getNotifierFacility();
    final Address notifierFacilityAddress = createNotifierFacilityAddress(notifierFacility);
    final List<ContactPoint> notifierFacilityContacts =
        createNotifierFacilityContactPoints(notifierFacility);
    final HumanName notifierFacilityContact = createNotifierFacilityContact(notifierFacility);
    return new NotifierDataBuilder()
        .setNotifierFacilityName(notifierFacility.getFacilityInfo().getInstitutionName())
        .setNotifierAddress(notifierFacilityAddress)
        .setNotifierTelecomList(notifierFacilityContacts)
        .addNotifierFacilityContact(notifierFacilityContact)
        .buildNotifierDataForGateway();
  }

  private static HumanName createNotifierFacilityContact(
      BedOccupancyNotifierFacility notifierFacility) {
    final PractitionerInfo contact = notifierFacility.getContact();
    final HumanNameDataBuilder humanNameDataBuilder = new HumanNameDataBuilder();

    final SalutationEnum salutation = contact.getSalutation();
    if (salutation != null) {
      switch (salutation) {
        case MR:
          humanNameDataBuilder.setSalutation(Salutation.MR);
          break;
        case MRS:
          humanNameDataBuilder.setSalutation(Salutation.MRS);
          break;
        default:
          throw new IllegalArgumentException("unknown salutation type: " + salutation);
      }
    }
    final String prefixInfo = contact.getPrefix();
    if (prefixInfo != null) {
      for (String prefix : prefixInfo.split("\\s+")) {
        humanNameDataBuilder.addPrefix(prefix);
      }
    }
    for (String firstName : contact.getFirstname().split("\\s+")) {
      humanNameDataBuilder.addGivenName(firstName);
    }
    humanNameDataBuilder.setFamilyName(contact.getLastname());
    return humanNameDataBuilder.build();
  }

  @NotNull
  private static List<ContactPoint> createNotifierFacilityContactPoints(
      BedOccupancyNotifierFacility notifierFacility) {
    List<ContactPoint> contactPoints = new ArrayList<>();
    for (ContactPointInfo contact : notifierFacility.getContacts()) {
      TelecomDataBuilder telecomDataBuilder =
          new TelecomDataBuilder()
              .setSystem(ContactPointSystem.fromCode(contact.getContactType().getValue()))
              .setValue(contact.getValue());

      final UsageEnum usage = contact.getUsage();
      if (usage != null) {
        telecomDataBuilder.setUse(ContactPointUse.fromCode(usage.getValue()));
      }

      contactPoints.add(telecomDataBuilder.build());
    }
    return contactPoints;
  }

  private static Address createNotifierFacilityAddress(
      BedOccupancyNotifierFacility notifierFacility) {
    final FacilityAddressInfo address = notifierFacility.getAddress();
    final AddressDataBuilder addressDataBuilder =
        new AddressDataBuilder()
            .setStreet(address.getStreet())
            .setHouseNumber(address.getHouseNumber())
            .setAdditionalInfo(address.getAdditionalInfo())
            .setPostalCode(address.getZip())
            .setCity(address.getCity())
            .setCountry(address.getCountry());
    return addressDataBuilder.build();
  }

  private static Composition createComposition(
      String locationID, PractitionerRole notifierRole, QuestionnaireResponse bedOccupancy) {
    ReportBedOccupancyDataBuilder composition = new ReportBedOccupancyDataBuilder();
    composition.setDefaults();
    composition.setNotifierRole(notifierRole);
    composition.setSubjectAsInekStandortId(locationID);
    composition.setStatisticInformationBedOccupancy(bedOccupancy);
    return composition.build();
  }

  public Bundle createReportBundle(BedOccupancy bedOccupancy) {
    final PractitionerRole notifierRole = createNotifierRole(bedOccupancy);
    final QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse(bedOccupancy);
    final Composition reportBedOccupancy =
        createComposition(
            bedOccupancy.getNotifierFacility().getLocationID(),
            notifierRole,
            questionnaireResponse);
    ReportBundleDataBuilder bundle = new ReportBundleDataBuilder();
    bundle.setDefaults().setLastUpdated(new Date());
    return bundle
        .setReportBedOccupancy(reportBedOccupancy)
        .setNotifierRole(notifierRole)
        .setStatisticInformationBedOccupancy(questionnaireResponse)
        .build();
  }
}
