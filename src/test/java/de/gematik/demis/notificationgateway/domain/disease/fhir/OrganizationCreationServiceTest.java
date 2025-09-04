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
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.CODE_SYSTEM_ORGANIZATION_TYPE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.gematik.demis.notificationgateway.FeatureFlags;
import de.gematik.demis.notificationgateway.common.dto.FacilityInfo;
import de.gematik.demis.notificationgateway.common.dto.NotifierFacility;
import de.gematik.demis.notificationgateway.common.dto.QuickTest;
import de.gematik.demis.notificationgateway.common.services.fhir.FhirObjectCreationService;
import de.gematik.demis.notificationgateway.utils.FileUtils;
import java.util.List;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Organization;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationCreationServiceTest {

  @Mock private FhirObjectCreationService fhirObjectCreationServiceMock;

  @Test
  void createNotifierFacilitySuccessfullyRegression() throws JsonProcessingException {
    final QuickTest quickTest =
        FileUtils.createQuickTest("portal/laboratory/notification_content_min.json");

    final NotifierFacility notifierFacility = quickTest.getNotifierFacility();
    final FacilityInfo facilityInfo = notifierFacility.getFacilityInfo();
    final String orgaTypeCode = "mySpecialCodeValue";
    facilityInfo.setOrganizationType(orgaTypeCode);

    final FeatureFlags featureFlags = mock(FeatureFlags.class);
    when(featureFlags.isNotifications73()).thenReturn(false);

    final OrganizationCreationService creationService =
        new OrganizationCreationService(fhirObjectCreationServiceMock, featureFlags);

    final Organization result = creationService.createNotifierFacility(notifierFacility);

    assertOrgaType(orgaTypeCode, null, result);
  }

  @Test
  void testNotifierFacilityIsHospitalRegression() throws JsonProcessingException {
    final QuickTest quickTest =
        FileUtils.createQuickTest("portal/laboratory/notification_content_min.json");

    final NotifierFacility notifierFacilityContent = quickTest.getNotifierFacility();

    final FeatureFlags featureFlags = mock(FeatureFlags.class);
    when(featureFlags.isNotifications73()).thenReturn(false);

    final OrganizationCreationService creationService =
        new OrganizationCreationService(fhirObjectCreationServiceMock, featureFlags);

    final Organization notifierFacility =
        creationService.createHospitalNotifierFacility(notifierFacilityContent);

    assertOrgaType("hospital", "Krankenhaus", notifierFacility);
  }

  @Test
  void createNotifierFacilitySuccessfully() throws JsonProcessingException {
    final QuickTest quickTest =
        FileUtils.createQuickTest("portal/laboratory/notification_content_min.json");

    final NotifierFacility notifierFacility = quickTest.getNotifierFacility();
    final FacilityInfo facilityInfo = notifierFacility.getFacilityInfo();
    final String orgaTypeCode = "mySpecialCodeValue";
    facilityInfo.setOrganizationType(orgaTypeCode);

    final FeatureFlags featureFlags = mock(FeatureFlags.class);
    when(featureFlags.isNotifications73()).thenReturn(true);

    final OrganizationCreationService creationServiceActiveFlag =
        new OrganizationCreationService(fhirObjectCreationServiceMock, featureFlags);

    final Organization result = creationServiceActiveFlag.createNotifierFacility(notifierFacility);

    assertOrgaType(orgaTypeCode, null, result);
  }

  @Test
  void testNotifierFacilityIsHospital() throws JsonProcessingException {
    final QuickTest quickTest =
        FileUtils.createQuickTest("portal/laboratory/notification_content_min.json");

    final NotifierFacility notifierFacilityContent = quickTest.getNotifierFacility();

    final FeatureFlags featureFlags = mock(FeatureFlags.class);
    when(featureFlags.isNotifications73()).thenReturn(true);

    final OrganizationCreationService creationServiceActiveFlag =
        new OrganizationCreationService(fhirObjectCreationServiceMock, featureFlags);

    final Organization notifierFacility =
        creationServiceActiveFlag.createHospitalNotifierFacility(notifierFacilityContent);

    assertOrgaType("hospital", "Krankenhaus", notifierFacility);
  }

  private void assertOrgaType(
      final String expectedCode, final String expectedDisplay, final Organization actual) {
    final List<CodeableConcept> types = actual.getType();
    assertEquals(1, types.size());
    final List<Coding> typeCodings = types.getFirst().getCoding();
    assertEquals(1, typeCodings.size());
    final Coding typeCoding = typeCodings.getFirst();
    assertEquals(CODE_SYSTEM_ORGANIZATION_TYPE, typeCoding.getSystem());
    assertEquals(expectedCode, typeCoding.getCode());
    assertEquals(expectedDisplay, typeCoding.getDisplay());
  }
}
