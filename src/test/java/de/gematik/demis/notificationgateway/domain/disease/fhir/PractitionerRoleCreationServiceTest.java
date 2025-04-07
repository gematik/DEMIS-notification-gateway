package de.gematik.demis.notificationgateway.domain.disease.fhir;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.demis.notificationgateway.common.constants.FhirConstants;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Test;

class PractitionerRoleCreationServiceTest {

  private final PractitionerRoleCreationService creationService =
      new PractitionerRoleCreationService();

  @Test
  void testSubmittingRoleContainsGeneralFixedData() {
    final PractitionerRole role = creationService.createSubmittingRole(null);

    assertTrue(role.hasId());
    assertTrue(role.hasMeta());
    final Meta meta = role.getMeta();
    assertTrue(meta.hasProfile());
    assertEquals(
        FhirConstants.PROFILE_SUBMITTING_ROLE, meta.getProfile().getFirst().asStringValue());
  }

  @Test
  void testNotifierRoleContainsGeneralFixedData() {
    final PractitionerRole role = creationService.createNotifierRole(null);

    assertTrue(role.hasId());
    assertTrue(role.hasMeta());
    final Meta meta = role.getMeta();
    assertTrue(meta.hasProfile());
    assertEquals(FhirConstants.PROFILE_NOTIFIER_ROLE, meta.getProfile().getFirst().asStringValue());
  }

  @Test
  void testSubmittingRoleContainsOrganizationIdInReference() {
    Organization facility = new Organization();
    final String facilityId = UUID.randomUUID().toString();
    facility.setId(facilityId);

    final PractitionerRole role = creationService.createSubmittingRole(facility);

    assertTrue(role.hasOrganization());
    final Reference organization = role.getOrganization();
    assertTrue(organization.hasReference());
    Assertions.assertThat(organization.getReference()).contains(facilityId);
  }

  @Test
  void testNotifierRoleContainsOrganizationIdInReference() {
    Organization facility = new Organization();
    final String facilityId = UUID.randomUUID().toString();
    facility.setId(facilityId);

    final PractitionerRole role = creationService.createNotifierRole(facility);

    assertTrue(role.hasOrganization());
  }
}
