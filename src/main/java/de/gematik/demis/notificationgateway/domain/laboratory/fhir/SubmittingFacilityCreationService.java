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

package de.gematik.demis.notificationgateway.domain.laboratory.fhir;

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.PROFILE_SUBMITTING_FACILITY;

import de.gematik.demis.notificationgateway.common.dto.ContactPointInfo;
import de.gematik.demis.notificationgateway.common.dto.NotifierFacility;
import de.gematik.demis.notificationgateway.common.services.fhir.FhirObjectCreationService;
import de.gematik.demis.notificationgateway.common.utils.ConfiguredCodeSystems;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Organization;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class SubmittingFacilityCreationService {

  private final FhirObjectCreationService fhirObjectCreationService;

  public Organization createSubmittingFacility(NotifierFacility facility) {
    final Organization submittingFacility = new Organization();
    submittingFacility.setId(UUID.randomUUID().toString());
    submittingFacility.setMeta(new Meta().addProfile(PROFILE_SUBMITTING_FACILITY));

    addOrganizationType(submittingFacility);
    addAddress(submittingFacility, facility);
    submittingFacility.setName(facility.getFacilityInfo().getInstitutionName());
    addContacts(submittingFacility, facility.getContacts());

    return submittingFacility;
  }

  private void addContacts(Organization notifierFacility, List<ContactPointInfo> contacts) {
    for (ContactPointInfo contact : contacts) {
      ContactPoint contactPoint = fhirObjectCreationService.createContactPoint(contact);
      notifierFacility.addTelecom(contactPoint);
    }
  }

  private void addAddress(
      Organization submittingFacility, NotifierFacility notifierFacilityContent) {
    final Address fhirAddress =
        fhirObjectCreationService.createAddress(notifierFacilityContent.getAddress());
    submittingFacility.addAddress(fhirAddress);
  }

  private void addOrganizationType(Organization submittingFacility) {
    submittingFacility
        .addType()
        .addCoding(
            ConfiguredCodeSystems.getInstance().getOrganizationTypeCoding("physicianOffice"));
  }
}
