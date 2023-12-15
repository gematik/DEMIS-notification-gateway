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

package de.gematik.demis.notificationgateway.domain.bedoccupancy.service;

import static de.gematik.demis.notificationgateway.common.enums.SupportedRealm.HOSPITAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.demis.notificationgateway.common.dto.BedOccupancy;
import de.gematik.demis.notificationgateway.common.dto.BedOccupancyNotifierFacility;
import de.gematik.demis.notificationgateway.common.dto.BedOccupancyQuestion;
import de.gematik.demis.notificationgateway.common.dto.OkResponse;
import de.gematik.demis.notificationgateway.common.properties.RPSProperties;
import de.gematik.demis.notificationgateway.common.proxies.BundlePublisher;
import de.gematik.demis.notificationgateway.common.services.OkResponseService;
import de.gematik.demis.notificationgateway.domain.HeaderProperties;
import de.gematik.demis.notificationgateway.domain.bedoccupancy.fhir.ReportBundleCreationService;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.Test;

class BedOccupancyServiceTest {

  private ReportBundleCreationService bundleCreationServiceMock =
      mock(ReportBundleCreationService.class);
  private BundlePublisher bundlePublisherMock = mock(BundlePublisher.class);
  private OkResponseService okResponseServiceMock = mock(OkResponseService.class);
  private RPSProperties rpsPropertiesMock = mock(RPSProperties.class);
  private HeaderProperties headerPropertiesMock = mock(HeaderProperties.class);

  @Test
  void shouldCreateBedOccupancyServiceAndUseHandleBedOccupancyMethod() {
    BedOccupancyQuestion bedOccupancyQuestion = new BedOccupancyQuestion();
    bedOccupancyQuestion.setOneTimeCode("");
    BedOccupancyNotifierFacility notifierFacility = new BedOccupancyNotifierFacility();
    notifierFacility.setOneTimeCode("");
    BedOccupancy bedOccupancy = new BedOccupancy();
    bedOccupancy.setNotifierFacility(notifierFacility);
    bedOccupancy.setBedOccupancyQuestion(bedOccupancyQuestion);
    Bundle t = new Bundle();
    String url = "someUrl";
    String someRemoteAddress = "someRemoteAddress";
    Parameters result = new Parameters();
    OkResponse expectedResponse = new OkResponse();

    when(bundleCreationServiceMock.createReportBundle(bedOccupancy)).thenReturn(t);
    when(rpsPropertiesMock.bedOccupancyUrl()).thenReturn(url);
    when(headerPropertiesMock.getBedOccupancyProfile()).thenReturn("profileId");
    when(headerPropertiesMock.getBedOccupancyVersion()).thenReturn("profileVersion");
    when(bundlePublisherMock.postRequest(
            t,
            HOSPITAL,
            url,
            RPSProperties.OPERATION_NAME,
            someRemoteAddress,
            "profileId",
            "profileVersion"))
        .thenReturn(result);
    when(okResponseServiceMock.buildOkResponse(result)).thenReturn(expectedResponse);

    // create BedOccupancyService with Mocks
    BedOccupancyService bedOccupancyService =
        new BedOccupancyService(
            bundleCreationServiceMock,
            bundlePublisherMock,
            okResponseServiceMock,
            rpsPropertiesMock,
            headerPropertiesMock);

    // use handleBedOccupancy method
    OkResponse okResponse = bedOccupancyService.handleBedOccupancy(bedOccupancy, someRemoteAddress);

    assertThat(okResponse).isEqualTo(expectedResponse);
  }
}
