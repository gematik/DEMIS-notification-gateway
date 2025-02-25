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
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Organization;
import org.springframework.stereotype.Service;

@Service
public class Organizations implements ResourceFactory {

  @Override
  public FhirResource createFhirResource(
      DiseaseNotificationContext context, QuestionnaireResponseItem item) {
    QuestionnaireResponseItem organizationItem = item.getAnswer().getFirst().getItem().getFirst();
    Organization organization = createOrganization(organizationItem);
    context.bundle().addOrganization(organization);
    return createFhirResource(organization, item.getLinkId());
  }

  @Override
  public boolean test(QuestionnaireResponseItem item) {
    /*
     * Organization references don't have a specific linkId, so we have to check the answer subitems.
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
}
