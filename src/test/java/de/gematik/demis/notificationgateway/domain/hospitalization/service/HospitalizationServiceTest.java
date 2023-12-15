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

package de.gematik.demis.notificationgateway.domain.hospitalization.service;

import static de.gematik.demis.notificationgateway.common.enums.SupportedRealm.LAB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.demis.notificationgateway.common.dto.ConditionInfo;
import de.gematik.demis.notificationgateway.common.dto.Disease;
import de.gematik.demis.notificationgateway.common.dto.DiseaseInfoCVDD;
import de.gematik.demis.notificationgateway.common.dto.DiseaseInfoCommon;
import de.gematik.demis.notificationgateway.common.dto.Hospitalization;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPerson;
import de.gematik.demis.notificationgateway.common.dto.NotifierFacility;
import de.gematik.demis.notificationgateway.common.dto.OkResponse;
import de.gematik.demis.notificationgateway.common.exceptions.BadRequestException;
import de.gematik.demis.notificationgateway.common.properties.NESProperties;
import de.gematik.demis.notificationgateway.common.proxies.BundlePublisher;
import de.gematik.demis.notificationgateway.common.services.OkResponseService;
import de.gematik.demis.notificationgateway.domain.HeaderProperties;
import de.gematik.demis.notificationgateway.domain.hospitalization.fhir.DiseaseBundleCreationService;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.Test;

class HospitalizationServiceTest {

  private final DiseaseBundleCreationService diseaseBundleCreationServiceMock =
      mock(DiseaseBundleCreationService.class);
  private final BundlePublisher bundlePublisherMock = mock(BundlePublisher.class);
  private final OkResponseService okResponseServiceMock = mock(OkResponseService.class);
  private final NESProperties nesPropertiesMock = mock(NESProperties.class);
  private final HeaderProperties headerPropertiesMock = mock(HeaderProperties.class);

  @Test
  void shouldCreateBedOccupancyServiceAndUseHandleBedOccupancyMethod() throws BadRequestException {
    NotifierFacility notifierFacility = new NotifierFacility();
    notifierFacility.setOneTimeCode("");
    Hospitalization hospitalization = new Hospitalization();
    hospitalization.setNotifierFacility(notifierFacility);
    Disease disease = new Disease();
    disease.setDiseaseInfoCommon(new DiseaseInfoCommon());
    disease.setDiseaseInfoCVDD(new DiseaseInfoCVDD());
    disease.setConditionInfo(new ConditionInfo());
    hospitalization.setDisease(disease);
    hospitalization.setNotifiedPerson(new NotifiedPerson());
    Bundle t = new Bundle();
    String url = "someUrl";
    String someRemoteAddress = "someRemoteAddress";
    Parameters result = new Parameters();
    OkResponse expectedResponse = new OkResponse();

    when(diseaseBundleCreationServiceMock.createDiseaseBundle(hospitalization)).thenReturn(t);
    when(nesPropertiesMock.hospitalizationUrl()).thenReturn(url);
    when(headerPropertiesMock.getDiseaseNotificationProfile()).thenReturn("profileId");
    when(headerPropertiesMock.getDiseaseNotificationVersion()).thenReturn("profileVersion");
    when(bundlePublisherMock.postRequest(
            t,
            LAB,
            url,
            NESProperties.OPERATION_NAME,
            someRemoteAddress,
            "profileId",
            "profileVersion"))
        .thenReturn(result);
    when(okResponseServiceMock.buildOkResponse(result)).thenReturn(expectedResponse);

    // create BedOccupancyService with Mocks
    HospitalizationService hospitalizationService =
        new HospitalizationService(
            diseaseBundleCreationServiceMock,
            bundlePublisherMock,
            okResponseServiceMock,
            nesPropertiesMock,
            headerPropertiesMock);

    // use handleBedOccupancy method
    OkResponse okResponse =
        hospitalizationService.sendNotification(hospitalization, someRemoteAddress);

    assertThat(okResponse).isEqualTo(expectedResponse);
  }
}
