package de.gematik.demis.notificationgateway.domain.bedoccupancy.fhir;

/*-
 * #%L
 * DEMIS Notification-Gateway
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
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
 * For additional notes and disclaimer from gematik and in case of changes by gematik,
 * find details in the "Readme" file.
 * #L%
 */

import static de.gematik.demis.notificationgateway.common.creator.HumanNameCreator.createHumanName;
import static de.gematik.demis.notificationgateway.domain.pathogen.creator.AddressCreator.createAddress;

import de.gematik.demis.notification.builder.demis.fhir.notification.builder.reports.ReportBedOccupancyDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.reports.ReportBundleDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.reports.StatisticInformationBedOccupancyDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.OrganizationBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.PractitionerRoleBuilder;
import de.gematik.demis.notificationgateway.common.creator.ContactPointCreator;
import de.gematik.demis.notificationgateway.common.dto.BedOccupancy;
import de.gematik.demis.notificationgateway.common.dto.BedOccupancyNotifierFacility;
import de.gematik.demis.notificationgateway.common.dto.BedOccupancyQuestion;
import de.gematik.demis.notificationgateway.common.dto.FacilityAddressInfo;
import de.gematik.demis.notificationgateway.common.dto.FacilityInfo;
import de.gematik.demis.notificationgateway.common.dto.OccupiedBeds;
import de.gematik.demis.notificationgateway.common.dto.OperableBeds;
import de.gematik.demis.notificationgateway.common.dto.PractitionerInfo;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Organization;
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

  private static PractitionerRole createNotifierRole(
      BedOccupancyNotifierFacility notifierFacility) {
    final FacilityAddressInfo addressInfo = notifierFacility.getAddress();
    final FacilityInfo facilityInfo = notifierFacility.getFacilityInfo();
    final PractitionerInfo practitionerInfo = notifierFacility.getContact();

    final List<ContactPoint> contactPoints =
        notifierFacility.getContacts().stream()
            .map(ContactPointCreator::createContactPoint)
            .toList();

    final HumanName contactName = createHumanName(practitionerInfo);

    final OrganizationBuilder organizationBuilder =
        new OrganizationBuilder()
            .asNotifierFacility()
            .setDefaults()
            .setTypeCode("hospital")
            .setTypeDisplay("Krankenhaus")
            .setFacilityName(facilityInfo.getInstitutionName())
            .setAddress(createAddress(addressInfo))
            .setTelecomList(contactPoints)
            .addContact(new Organization.OrganizationContactComponent().setName(contactName));

    if (Boolean.TRUE.equals(facilityInfo.getExistsBsnr())) {
      organizationBuilder.setBsnrValue(facilityInfo.getBsnr());
    }

    final Organization organization = organizationBuilder.build();

    final PractitionerRoleBuilder practitionerRoleBuilder =
        new PractitionerRoleBuilder().asNotifierRole().withOrganization(organization);

    return practitionerRoleBuilder.build();
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
    final PractitionerRole notifierRole = createNotifierRole(bedOccupancy.getNotifierFacility());
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
