package de.gematik.demis.notificationgateway.domain.pathogen.creator;

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

import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.OrganizationBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.PractitionerRoleBuilder;
import de.gematik.demis.notificationgateway.common.creator.ContactPointCreator;
import de.gematik.demis.notificationgateway.common.dto.FacilityAddressInfo;
import de.gematik.demis.notificationgateway.common.dto.FacilityInfo;
import de.gematik.demis.notificationgateway.common.dto.NotifierFacility;
import de.gematik.demis.notificationgateway.common.dto.PractitionerInfo;
import de.gematik.demis.notificationgateway.common.dto.SubmitterFacility;
import de.gematik.demis.notificationgateway.common.dto.SubmittingFacilityInfo;
import java.util.List;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.PractitionerRole;

/**
 * Utility class for creating FHIR {@link PractitionerRole} objects.
 *
 * <p>This class provides methods to build {@link PractitionerRole} instances for notifier and
 * submitter facilities.
 */
public class PractitionerOrganizationCreator {

  /** Constant representing the type code for private laboratories. */
  public static final String OTH_PRIVAT_LAB = "othPrivatLab";

  /** Constant representing the display name for private laboratories. */
  public static final String SONSTIGE_PRIVATE_UNTERSUCHUNGSSTELLE =
      "Sonstige private Untersuchungsstelle";

  /**
   * Private constructor to prevent instantiation of this utility class.
   *
   * <p>This class is designed as a utility class and should not be instantiated.
   */
  private PractitionerOrganizationCreator() {}

  /**
   * Creates a FHIR {@link PractitionerRole} object for a notifier facility.
   *
   * @param notifierFacility The {@link NotifierFacility} object containing details about the
   *     notifier facility.
   * @return A {@link PractitionerRole} object populated with the provided notifier facility data.
   */
  public static PractitionerRole createNotifierPractitionerRole(NotifierFacility notifierFacility) {

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
            .setTypeCode(OTH_PRIVAT_LAB)
            .setTypeDisplay(SONSTIGE_PRIVATE_UNTERSUCHUNGSSTELLE)
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

  /**
   * Creates a FHIR {@link PractitionerRole} object for a submitter facility.
   *
   * @param submitterFacility The {@link SubmitterFacility} object containing details about the
   *     submitter facility.
   * @param isNotifiedPersonFacility A boolean indicating whether the submitter facility is also a
   *     notified person facility.
   * @return A {@link PractitionerRole} object populated with the provided submitter facility data.
   */
  public static PractitionerRole createSubmitterPractitionerRole(
      SubmitterFacility submitterFacility,
      boolean isNotifiedPersonFacility,
      final boolean isOthPrivatLabSubmitterAssignmentDisabled) {
    final SubmittingFacilityInfo facilityInfo = submitterFacility.getFacilityInfo();
    final FacilityAddressInfo addressInfo = submitterFacility.getAddress();

    final List<ContactPoint> contactPoints =
        submitterFacility.getContacts().stream()
            .map(ContactPointCreator::createContactPoint)
            .toList();
    final Address submitterAddress = createAddress(addressInfo);
    final HumanName contactName = createHumanName(submitterFacility.getContact());
    final OrganizationBuilder organizationBuilder =
        new OrganizationBuilder()
            .asSubmittingFacility()
            .setSubmitterDetails(contactName, facilityInfo.getDepartmentName())
            .setDefaults()
            .setFacilityName(facilityInfo.getInstitutionName())
            .setAddress(submitterAddress)
            .setTelecomList(contactPoints);

    if (!isOthPrivatLabSubmitterAssignmentDisabled) {
      organizationBuilder
          .setTypeCode(OTH_PRIVAT_LAB)
          .setTypeDisplay(SONSTIGE_PRIVATE_UNTERSUCHUNGSSTELLE);
    }
    if (isNotifiedPersonFacility) {
      organizationBuilder.addNotifiedPersonFacilityProfile();
    }

    final Organization submittingFacility = organizationBuilder.build();

    return new PractitionerRoleBuilder()
        .asSubmittingRole()
        .withOrganization(submittingFacility)
        .build();
  }
}
