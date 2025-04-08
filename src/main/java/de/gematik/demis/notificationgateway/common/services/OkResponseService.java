package de.gematik.demis.notificationgateway.common.services;

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
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */

import de.gematik.demis.notificationgateway.common.dto.OkResponse;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OkResponseService {

  private static final String HAPI_FHIR_BASE_URL = "https://demis.rki.de/fhir";

  public OkResponse buildOkResponse(Parameters result) {
    return addOperationOutcomeInformation(new OkResponse(), result);
  }

  public OkResponse addOperationOutcomeInformation(OkResponse response, Parameters result) {
    var operationOutcomeResource = findResource(result, "operationOutcome");
    var bundleResource = findResource(result, "bundle");

    if (operationOutcomeResource.isPresent()) {
      response.setStatus(readStatus(operationOutcomeResource));
    }

    if (bundleResource.isPresent()) {
      var bundle = ((Bundle) bundleResource.get());
      // we can do so, because its defined first element must be the composition
      var compositionResource = ((Composition) bundle.getEntry().get(0).getResource());
      var binaryReference = findBinaryReferenceInComposition(compositionResource);
      var demisOrganisationReference =
          findDEMISOrganisationReferenceInComposition(compositionResource);

      var demisOrgaResource =
          findBundleEntry(bundle, HAPI_FHIR_BASE_URL + "/" + demisOrganisationReference);
      var receiptResource = findBundleEntry(bundle, HAPI_FHIR_BASE_URL + "/" + binaryReference);

      response.setTimestamp(readTimeStamp(compositionResource));
      response.setNotificationId(readId(compositionResource));
      response.setTitle(compositionResource.getTitle());

      updateAuthorInformation(response, demisOrgaResource);
      updateReceiptInformation(response, receiptResource);
    }

    return response;
  }

  private String readId(Composition composition) {

    Composition.CompositionRelatesToComponent relatesToFirstRep =
        composition.getRelatesToFirstRep();
    if (relatesToFirstRep != null) {
      Reference targetReference = relatesToFirstRep.getTargetReference();
      if (targetReference != null) {
        Identifier identifier = targetReference.getIdentifier();
        if (identifier != null && identifier.hasValue()) {
          return identifier.getValue();
        }
      }
    }

    // fallback
    log.error("no notification id was found");
    return "";
  }

  private OkResponse updateReceiptInformation(
      OkResponse response, Optional<Bundle.BundleEntryComponent> receiptResource) {
    if (receiptResource.isPresent()) {
      var receipt = ((Binary) receiptResource.get().getResource());

      response.setContentType(receipt.getContentType());
      response.setContent(receipt.getData());
    }

    return response;
  }

  private OkResponse updateAuthorInformation(
      OkResponse response, Optional<Bundle.BundleEntryComponent> demisOrganisation) {
    if (demisOrganisation.isPresent()) {
      var organisation = (Organization) demisOrganisation.get().getResource();
      response.setAuthorName(organisation.getName());
      response.setAuthorEmail(readEmailAddress(organisation));
    }
    return response;
  }

  private String readEmailAddress(Organization organization) {
    if (null == organization) {
      return "";
    }

    var emailOptional =
        organization.getContact().stream()
            .filter(Organization.OrganizationContactComponent::hasTelecom)
            .map(organizationContactComponent -> organizationContactComponent.getTelecom().get(0))
            .filter(contact -> "Email".equals(contact.getSystem().getDisplay()))
            .map(ContactPoint::getValue)
            .findFirst();

    return emailOptional.orElse("");
  }

  private String readTimeStamp(Composition composition) {
    return DateFormatUtils.format(composition.getDate(), "dd.MM.yyyy HH:mm:ss");
  }

  private String readStatus(Optional<Resource> resource) {
    if (resource.isEmpty()) {
      return "No status found";
    }

    var operationOutcome = (OperationOutcome) resource.get();

    if (!operationOutcome.getIssue().isEmpty()) {
      var issue = operationOutcome.getIssue().get(0);
      return issue.getDetails().getText();
    }

    return "";
  }

  private String findBinaryReferenceInComposition(Composition composition) {
    var optSection =
        composition.getSection().stream()
            .filter(
                sectionComponent ->
                    sectionComponent.hasTitle()
                        && "PDF Quittung".equals(sectionComponent.getTitle()))
            .findFirst();

    if (optSection.isPresent()) {
      return getPdfQuittung(optSection.get());
    }
    return "";
  }

  private String getPdfQuittung(Composition.SectionComponent section) {
    var optSection =
        section.getEntry().stream().map(Optional::ofNullable).findFirst().orElse(Optional.empty());

    if (optSection.isPresent()) {
      return optSection.get().getReference();
    }

    return "";
  }

  private String findDEMISOrganisationReferenceInComposition(Composition composition) {
    var result = composition.getAuthor().stream().filter(Objects::nonNull).findFirst();

    if (result.isPresent()) {
      return result.get().getReference();
    }

    return "";
  }

  private Optional<Resource> findResource(Parameters response, String resourceName) {
    if (Objects.isNull(resourceName) || Objects.isNull(response)) {
      return Optional.empty();
    }

    return response.getParameter().stream()
        .filter(parameters -> resourceName.equals(parameters.getName()) && parameters.hasResource())
        .map(Parameters.ParametersParameterComponent::getResource)
        .findFirst();
  }

  private Optional<Bundle.BundleEntryComponent> findBundleEntry(
      Bundle bundle, String searchFullUrl) {
    return bundle.getEntry().stream()
        .filter(entryComponent -> searchFullUrl.equals(entryComponent.getFullUrl()))
        .findFirst();
  }
}
