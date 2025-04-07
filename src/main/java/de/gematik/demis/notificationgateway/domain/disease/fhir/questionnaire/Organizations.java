package de.gematik.demis.notificationgateway.domain.disease.fhir.questionnaire;

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

import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.AddressDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.OrganizationBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.TelecomDataBuilder;
import de.gematik.demis.notificationgateway.common.dto.CodeDisplay;
import de.gematik.demis.notificationgateway.common.dto.QuestionnaireResponseAnswer;
import de.gematik.demis.notificationgateway.common.dto.QuestionnaireResponseItem;
import de.gematik.demis.notificationgateway.domain.disease.fhir.DiseaseNotificationContext;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Type;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class Organizations implements ResourceFactory {

  private static final String CHECKBOX_LINK_ID_COPY_CONTACT = "copyNotifierContact";
  private static final String CHECKBOX_LINK_ID_COPY_CURRENT_ADDRESS =
      "copyNotifiedPersonCurrentAddress";
  private static final String EXTENSION_NOTIFIED_PERSON_FACILITY_ADDRESS =
      "https://demis.rki.de/fhir/StructureDefinition/FacilityAddressNotifiedPerson";
  private static final String EXTENSION_CURRENT_ADDRESS =
      "https://demis.rki.de/fhir/StructureDefinition/AddressUse";

  private final boolean featureFlagCopyCheckboxes;

  Organizations(@Value("${feature.flag.hosp_copy_checkboxes}") boolean featureFlagCopyCheckboxes) {
    this.featureFlagCopyCheckboxes = featureFlagCopyCheckboxes;
  }

  @Override
  public FhirResource createFhirResource(
      DiseaseNotificationContext context, QuestionnaireResponseItem item) {
    QuestionnaireResponseItem organizationItem = item.getAnswer().getFirst().getItem().getFirst();
    Organization organization = getReferencedOrganization(context, organizationItem);
    if (organization == null) {
      organization = createOrganization(context, organizationItem);
    }
    syncContacts(context, organizationItem, organization);
    return createFhirResource(organization, item.getLinkId());
  }

  private Organization createOrganization(
      DiseaseNotificationContext context, QuestionnaireResponseItem organizationItem) {
    final Organization organization = createOrganization(organizationItem);
    context.bundle().addOrganization(organization);
    return organization;
  }

  private Organization getReferencedOrganization(
      DiseaseNotificationContext context, QuestionnaireResponseItem organizationItem) {
    Organization organization = null;
    if (shouldCopyNotifiedPersonCurrentAddress(organizationItem)) {
      log.debug("Organization definition is referencing current address of notified person");
      organization = getNotifiedPersonCurrentAddressOrganization(context);
      if (organization == null) {
        log.warn(
            "Notified person current address organization not found. Unable to create reference.");
      }
    }
    return organization;
  }

  private boolean shouldCopyNotifiedPersonCurrentAddress(QuestionnaireResponseItem organization) {
    return featureFlagCopyCheckboxes
        && findSubItemAnswer(organization, CHECKBOX_LINK_ID_COPY_CURRENT_ADDRESS)
            .map(QuestionnaireResponseAnswer::getValueBoolean)
            .orElse(false);
  }

  private Organization getNotifiedPersonCurrentAddressOrganization(
      DiseaseNotificationContext context) {
    return context.notifiedPerson().getAddress().stream()
        .filter(address -> address.hasExtension(EXTENSION_CURRENT_ADDRESS))
        .filter(address -> address.hasExtension(EXTENSION_NOTIFIED_PERSON_FACILITY_ADDRESS))
        .findFirst()
        .map(this::getNotifiedPersonFacilityOrganization)
        .orElse(null);
  }

  private Organization getNotifiedPersonFacilityOrganization(Address address) {
    final Type value =
        address.getExtensionByUrl(EXTENSION_NOTIFIED_PERSON_FACILITY_ADDRESS).getValue();
    if (value instanceof Reference reference) {
      return (Organization) reference.getResource();
    }
    return null;
  }

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

  private Organization createOrganization(QuestionnaireResponseItem item) {
    final var organization = new OrganizationBuilder();
    organization.setDefaults();
    setFacilityName(item, organization);
    setAddress(item, organization);
    setContact(item, organization);
    setTelecom(item, organization);
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

  private void setContact(QuestionnaireResponseItem item, OrganizationBuilder organization) {
    findSubItem(item, "contact").ifPresent(contact -> setContactItem(contact, organization));
  }

  private void setContactItem(QuestionnaireResponseItem contact, OrganizationBuilder organization) {
    findSubItem(contact, "name").ifPresent(name -> setContactItemName(name, organization));
  }

  private void setContactItemName(
      QuestionnaireResponseItem name, OrganizationBuilder organization) {
    final String prefix = getSubItemAnswerTextOrNull(name, "prefix");
    final String given = getSubItemAnswerTextOrNull(name, "given");
    final String family = getSubItemAnswerTextOrNull(name, "family");
    if ((prefix != null) || (given != null) || (family != null)) {
      organization.addContact(prefix, given, family);
    }
  }

  private void setTelecom(QuestionnaireResponseItem item, OrganizationBuilder organization) {
    findSubItem(item, "telecom").ifPresent(telecom -> setTelecomItem(telecom, organization));
  }

  private void setTelecomItem(QuestionnaireResponseItem telecom, OrganizationBuilder organization) {
    final String phone = getSubItemAnswerTextOrNull(telecom, "phone");
    if (phone != null) {
      organization.addTelecom(new TelecomDataBuilder().setPhone(phone).build());
    }
    final String email = getSubItemAnswerTextOrNull(telecom, "email");
    if (email != null) {
      organization.addTelecom(new TelecomDataBuilder().setEmail(email).build());
    }
  }

  private String getSubItemAnswerTextOrNull(QuestionnaireResponseItem item, String linkId) {
    return findSubItemAnswer(item, linkId)
        .map(QuestionnaireResponseAnswer::getValueString)
        .map(StringUtils::trimToNull)
        .orElse(null);
  }

  private void syncContacts(
      DiseaseNotificationContext context,
      QuestionnaireResponseItem organizationItem,
      Organization organization) {
    if (shouldCopyNotifierContacts(organizationItem)) {
      log.debug("Copying contacts from notifier to organization");
      final Organization notifierOrganization = getNotifierOrganization(context);
      if (notifierOrganization == null) {
        log.warn("Notifier organization not found. Unable to copy contacts.");
      } else if (organization != notifierOrganization) {
        notifierOrganization.getContact().forEach(organization::addContact);
      }
    }
  }

  private boolean shouldCopyNotifierContacts(QuestionnaireResponseItem organizationItem) {
    if (this.featureFlagCopyCheckboxes) {
      final Optional<QuestionnaireResponseItem> contact = findSubItem(organizationItem, "contact");
      if (contact.isPresent()) {
        return findSubItemAnswer(contact.get(), CHECKBOX_LINK_ID_COPY_CONTACT)
            .map(QuestionnaireResponseAnswer::getValueBoolean)
            .orElse(false);
      }
    }
    return false;
  }

  private Organization getNotifierOrganization(DiseaseNotificationContext context) {
    final PractitionerRole notifier = context.bundle().getNotifierRole();
    if (notifier != null) {
      final Reference reference = notifier.getOrganization();
      if (reference != null) {
        return (Organization) reference.getResource();
      }
    }
    return null;
  }
}
