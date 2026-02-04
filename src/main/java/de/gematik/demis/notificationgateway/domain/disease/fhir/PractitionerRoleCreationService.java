package de.gematik.demis.notificationgateway.domain.disease.fhir;

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

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.PROFILE_NOTIFIER_ROLE;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.PROFILE_SUBMITTING_ROLE;

import de.gematik.demis.notification.builder.demis.fhir.notification.utils.Utils;
import de.gematik.demis.notificationgateway.common.utils.ReferenceUtils;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.stereotype.Service;

@Service
public class PractitionerRoleCreationService {

  public PractitionerRole createSubmittingRole(Organization submittingFacility) {
    PractitionerRole practitionerRole = new PractitionerRole();
    practitionerRole.setId(Utils.generateUuidString());
    practitionerRole.setMeta(new Meta().addProfile(PROFILE_SUBMITTING_ROLE));
    if (submittingFacility != null) {
      practitionerRole.setOrganization(ReferenceUtils.createReference(submittingFacility));
    }
    return practitionerRole;
  }

  public PractitionerRole createNotifierRole(Organization notifierFacility) {
    PractitionerRole practitionerRole = new PractitionerRole();
    practitionerRole.setId(Utils.generateUuidString());
    practitionerRole.setMeta(new Meta().addProfile(PROFILE_NOTIFIER_ROLE));
    if (notifierFacility != null) {
      practitionerRole.setOrganization(new Reference(notifierFacility));
    }
    return practitionerRole;
  }
}
