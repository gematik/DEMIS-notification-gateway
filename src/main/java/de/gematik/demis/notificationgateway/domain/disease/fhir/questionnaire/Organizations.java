package de.gematik.demis.notificationgateway.domain.disease.fhir.questionnaire;

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

import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.AddressDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.HumanNameDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.OrganizationBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.TelecomDataBuilder;
import de.gematik.demis.notificationgateway.FeatureFlags;
import de.gematik.demis.notificationgateway.common.dto.CodeDisplay;
import de.gematik.demis.notificationgateway.common.dto.QuestionnaireResponseAnswer;
import de.gematik.demis.notificationgateway.common.dto.QuestionnaireResponseItem;
import de.gematik.demis.notificationgateway.domain.disease.fhir.DiseaseNotificationContext;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Type;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class Organizations implements ResourceFactory {

  private static final String CHECKBOX_LINK_ID_COPY_CONTACT = "copyNotifierContact";
  private static final String CHECKBOX_LINK_ID_COPY_CURRENT_ADDRESS =
      "copyNotifiedPersonCurrentAddress";
  private static final String EXTENSION_NOTIFIED_PERSON_FACILITY_ADDRESS =
      "https://demis.rki.de/fhir/StructureDefinition/FacilityAddressNotifiedPerson";
  private static final String ADDRESS_USE_EXTENSION =
      "https://demis.rki.de/fhir/StructureDefinition/AddressUse";
  private static final String ADDRESS_USE_SYSTEM =
      "https://demis.rki.de/fhir/CodeSystem/addressUse";
  private static final String ADDRESS_USE_CODE_CURRENT = "current";

  private final FeatureFlags featureFlags;

  @Override
  public boolean test(QuestionnaireResponseItem item) {
    /*
     * References of organizations don't have a specific linkId, so we have to check the answer subitems.
     * This code assumes that the first answer subitem is the reference to the organization
     * and that there are no other answer subitems.
     */
    List<QuestionnaireResponseAnswer> answers = item.getAnswer();
    if ((answers != null) && !answers.isEmpty()) {
      List<QuestionnaireResponseItem> answerSubitems = answers.getFirst().getItem();
      if ((answerSubitems != null) && !answerSubitems.isEmpty()) {
        return "Organization".equals(answerSubitems.getFirst().getLinkId());
      }
    }
    return false;
  }

  @Override
  public FhirResource createFhirResource(
      DiseaseNotificationContext context, QuestionnaireResponseItem item) {
    QuestionnaireResponseItem organizationItem = item.getAnswer().getFirst().getItem().getFirst();
    Organization organization = creatHospitalizationOrganization(context, organizationItem);
    setContactAndTelecom(context, organizationItem, organization);
    return createFhirResource(organization, item.getLinkId());
  }

  /**
   * Create organization without mentioning contact and telecom details.
   *
   * @param context context
   * @param organizationItem form input
   * @return organization
   */
  private Organization creatHospitalizationOrganization(
      DiseaseNotificationContext context, QuestionnaireResponseItem organizationItem) {
    Organization organization = copyOrganizationIfRequested(context, organizationItem);
    if (organization == null) {
      organization = creatHospitalizationOrganization(organizationItem);
    }
    context.bundleBuilder().addEncounterOrganization(organization);
    return organization;
  }

  @Nullable
  private Organization copyOrganizationIfRequested(
      DiseaseNotificationContext context, QuestionnaireResponseItem organizationItem) {
    Organization organization = null;
    if (shouldCopyNotifiedPersonCurrentAddress(organizationItem)) {
      log.debug("Organization definition is referencing current address of notified person");
      final Organization source = getNotifiedPersonCurrentAddressOrganization(context);
      if (source == null) {
        log.warn(
            "Notified person current address organization not found. Unable to create reference.");
      } else {
        organization =
            new OrganizationBuilder()
                .setDefaults()
                .setFacilityName(source.getName())
                .setAddress(source.getAddressFirstRep())
                .build();
      }
    }
    return organization;
  }

  private boolean shouldCopyNotifiedPersonCurrentAddress(QuestionnaireResponseItem organization) {
    return findSubItemAnswer(organization, CHECKBOX_LINK_ID_COPY_CURRENT_ADDRESS)
        .map(QuestionnaireResponseAnswer::getValueBoolean)
        .orElse(false);
  }

  @Nullable
  private Organization getNotifiedPersonCurrentAddressOrganization(
      DiseaseNotificationContext context) {
    return context.notifiedPerson().getAddress().stream()
        .filter(this::isCurrentAddress)
        .filter(address -> address.hasExtension(EXTENSION_NOTIFIED_PERSON_FACILITY_ADDRESS))
        .findFirst()
        .map(this::getNotifiedPersonFacilityOrganization)
        .orElse(null);
  }

  private boolean isCurrentAddress(Address address) {
    final Extension extension = address.getExtensionByUrl(ADDRESS_USE_EXTENSION);
    boolean currentAddress = false;
    if (extension != null) {
      final Type value = extension.getValue();
      if (value instanceof Coding coding) {
        currentAddress =
            ADDRESS_USE_SYSTEM.equals(coding.getSystem())
                && ADDRESS_USE_CODE_CURRENT.equals(coding.getCode());
      } else {
        log.warn(
            "Unexpected type for address use extension value. Expected: Coding Actual: {}",
            ((value != null) ? value.getClass() : null));
      }
    }
    return currentAddress;
  }

  @Nullable
  private Organization getNotifiedPersonFacilityOrganization(Address address) {
    final Type value =
        address.getExtensionByUrl(EXTENSION_NOTIFIED_PERSON_FACILITY_ADDRESS).getValue();
    if (value instanceof Reference reference) {
      return (Organization) reference.getResource();
    }
    return null;
  }

  private Organization creatHospitalizationOrganization(QuestionnaireResponseItem item) {
    final var organization = new OrganizationBuilder();
    organization.setDefaults();
    setFacilityName(item, organization);
    setAddress(item, organization);
    return organization.build();
  }

  private void setFacilityName(QuestionnaireResponseItem item, OrganizationBuilder organization) {
    findSubItemAnswer(item, "name")
        .ifPresent(name -> organization.setFacilityName(name.getValueString()));
  }

  private void setAddress(QuestionnaireResponseItem item, OrganizationBuilder organization) {
    findSubItem(item, "address").ifPresent(address -> setAddressItem(address, organization));
  }

  private void setAddressItem(QuestionnaireResponseItem item, OrganizationBuilder organization) {
    final String street = getSubItemAnswerTextOrNull(item, "street");
    final String houseNumber = getSubItemAnswerTextOrNull(item, "houseNumber");
    final String postalCode = getSubItemAnswerTextOrNull(item, "postalCode");
    final String city = getSubItemAnswerTextOrNull(item, "city");
    final String country = getCountry(item);
    if ((street != null) || (postalCode != null) || (city != null) || (country != null)) {
      organization.setAddress(
          new AddressDataBuilder()
              .setStreet(street)
              .setHouseNumber(houseNumber)
              .setPostalCode(postalCode)
              .setCity(city)
              .setCountry(country)
              .build());
    }
  }

  @Nullable
  private String getCountry(QuestionnaireResponseItem item) {
    Optional<QuestionnaireResponseAnswer> countryAnswer = findSubItemAnswer(item, "country");
    if (countryAnswer.isPresent()) {
      QuestionnaireResponseAnswer answer = countryAnswer.get();
      CodeDisplay valueCoding = answer.getValueCoding();
      if (valueCoding != null) {
        return valueCoding.getCode();
      }
      return StringUtils.trimToNull(answer.getValueString());
    }
    return null;
  }

  private void setContactAndTelecom(
      DiseaseNotificationContext context,
      QuestionnaireResponseItem input,
      Organization organization) {
    if (shouldCopyNotifierContacts(input)) {
      copyNotifierContacts(context, organization);
    } else {
      addContact(input, organization);
      addPhoneAndMail(input, organization);
    }
  }

  private void copyNotifierContacts(DiseaseNotificationContext context, Organization target) {
    log.debug("Copying contacts from notifier to organization");
    final Organization source = getNotifierOrganization(context);
    if (source == null) {
      log.warn("Notifier organization not found. Unable to copy contacts.");
    } else {
      target.setContact(source.getContact());
      final List<ContactPoint> telecoms = source.getTelecom();
      addFirstPhone(target, telecoms);
      addFirstMail(target, telecoms);
    }
  }

  private boolean isPhone(ContactPoint contact) {
    return contact.getSystem() == ContactPoint.ContactPointSystem.PHONE;
  }

  private boolean isMail(ContactPoint contact) {
    return contact.getSystem() == ContactPoint.ContactPointSystem.EMAIL;
  }

  private void addFirstPhone(Organization target, List<ContactPoint> telecoms) {
    telecoms.stream().filter(this::isPhone).findFirst().ifPresent(target::addTelecom);
  }

  private void addFirstMail(Organization target, List<ContactPoint> telecoms) {
    telecoms.stream().filter(this::isMail).findFirst().ifPresent(target::addTelecom);
  }

  private void addContact(QuestionnaireResponseItem item, Organization organization) {
    final Optional<QuestionnaireResponseItem> contact = findSubItem(item, "contact");
    if (contact.isPresent()) {
      final Optional<QuestionnaireResponseItem> name = findSubItem(contact.get(), "name");
      if (name.isPresent()) {
        addContactOfNameItem(name.get(), organization);
      }
    }
  }

  private void addContactOfNameItem(QuestionnaireResponseItem name, Organization organization) {
    final String prefix = getSubItemAnswerTextOrNull(name, "prefix");
    final String given = getSubItemAnswerTextOrNull(name, "given");
    final String family = getSubItemAnswerTextOrNull(name, "family");
    if ((prefix != null) || (given != null) || (family != null)) {
      final var contact = new Organization.OrganizationContactComponent();
      final HumanName humanName = HumanNameDataBuilder.with(prefix, given, family);
      contact.setName(humanName);
      organization.addContact(contact);
    }
  }

  private void addPhoneAndMail(QuestionnaireResponseItem item, Organization organization) {
    findSubItem(item, "telecom")
        .ifPresent(telecom -> addPhoneAndMailOfTelecomItem(telecom, organization));
  }

  private void addPhoneAndMailOfTelecomItem(
      QuestionnaireResponseItem telecom, Organization organization) {
    final String phone = getSubItemAnswerTextOrNull(telecom, "phone");
    if (phone != null) {
      organization.addTelecom(new TelecomDataBuilder().setPhone(phone).build());
    }
    final String email = getSubItemAnswerTextOrNull(telecom, "email");
    if (email != null) {
      organization.addTelecom(new TelecomDataBuilder().setEmail(email).build());
    }
  }

  @Nullable
  private String getSubItemAnswerTextOrNull(QuestionnaireResponseItem item, String linkId) {
    return findSubItemAnswer(item, linkId)
        .map(QuestionnaireResponseAnswer::getValueString)
        .map(StringUtils::trimToNull)
        .orElse(null);
  }

  private boolean shouldCopyNotifierContacts(QuestionnaireResponseItem organizationItem) {
    final Optional<QuestionnaireResponseItem> contact = findSubItem(organizationItem, "contact");
    if (contact.isPresent()) {
      return findSubItemAnswer(contact.get(), CHECKBOX_LINK_ID_COPY_CONTACT)
          .map(QuestionnaireResponseAnswer::getValueBoolean)
          .orElse(false);
    }
    return false;
  }

  @Nullable
  private Organization getNotifierOrganization(DiseaseNotificationContext context) {
    final PractitionerRole notifier = context.bundleBuilder().getNotifierRole();
    if (notifier != null) {
      final Reference reference = notifier.getOrganization();
      if (reference != null) {
        return (Organization) reference.getResource();
      }
    }
    return null;
  }
}
