package de.gematik.demis.notificationgateway.domain.bedoccupancy.service;

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

import static de.gematik.demis.notificationgateway.common.constants.MessageConstants.CONTENT_NOT_ACCEPTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import de.gematik.demis.notificationgateway.common.dto.BedOccupancy;
import de.gematik.demis.notificationgateway.common.dto.BedOccupancyNotifierFacility;
import de.gematik.demis.notificationgateway.common.dto.BedOccupancyQuestion;
import de.gematik.demis.notificationgateway.common.dto.OkResponse;
import de.gematik.demis.notificationgateway.common.exceptions.HoneypotException;
import de.gematik.demis.notificationgateway.common.properties.RPSProperties;
import de.gematik.demis.notificationgateway.common.proxies.BundlePublisher;
import de.gematik.demis.notificationgateway.common.services.OkResponseService;
import de.gematik.demis.notificationgateway.common.utils.Token;
import de.gematik.demis.notificationgateway.domain.HeaderProperties;
import de.gematik.demis.notificationgateway.domain.bedoccupancy.fhir.ReportBundleCreationService;
import jakarta.security.auth.message.AuthException;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BedOccupancyServiceTest {

  @Mock private ReportBundleCreationService bundleCreationServiceMock;
  @Mock private BundlePublisher bundlePublisherMock;
  @Mock private OkResponseService okResponseServiceMock;
  @Mock private RPSProperties rpsPropertiesMock;
  @Mock private HeaderProperties headerPropertiesMock;
  @Mock private Token token;

  @InjectMocks private BedOccupancyService bedOccupancyService;

  private static BedOccupancy createBedOccupancy() {
    BedOccupancyQuestion bedOccupancyQuestion = new BedOccupancyQuestion();
    BedOccupancyNotifierFacility notifierFacility = new BedOccupancyNotifierFacility();
    notifierFacility.setOneTimeCode("");
    BedOccupancy bedOccupancy = new BedOccupancy();
    bedOccupancy.setNotifierFacility(notifierFacility);
    bedOccupancy.setBedOccupancyQuestion(bedOccupancyQuestion);
    return bedOccupancy;
  }

  @Test
  void shouldCreateBedOccupancyServiceAndUseHandleBedOccupancyMethod() throws AuthException {
    BedOccupancy bedOccupancy = createBedOccupancy();

    Bundle t = new Bundle();
    String url = "someUrl";
    Parameters result = new Parameters();
    OkResponse expectedResponse = new OkResponse();

    when(bundleCreationServiceMock.createReportBundle(bedOccupancy)).thenReturn(t);
    when(rpsPropertiesMock.bedOccupancyUrl()).thenReturn(url);
    when(headerPropertiesMock.getBedOccupancyProfile()).thenReturn("profileId");
    when(headerPropertiesMock.getBedOccupancyVersion()).thenReturn("profileVersion");
    when(bundlePublisherMock.postRequest(
            eq(t),
            eq(url),
            eq(RPSProperties.OPERATION_NAME),
            eq("profileId"),
            eq("profileVersion"),
            any()))
        .thenReturn(result);
    when(okResponseServiceMock.buildOkResponse(result)).thenReturn(expectedResponse);

    // use handleBedOccupancy method
    OkResponse okResponse = bedOccupancyService.handleBedOccupancy(bedOccupancy, token);

    assertThat(okResponse).isEqualTo(expectedResponse);
  }

  @Test
  void givenHoneyPotNotifierFacilityWhenHandleBedOccupancyThenError() {
    BedOccupancy bedOccupancy = createBedOccupancy();
    bedOccupancy.getNotifierFacility().setOneTimeCode("123");
    verifyHoneyPotDetection(bedOccupancy);
  }

  private void verifyHoneyPotDetection(BedOccupancy bedOccupancy) {
    final ThrowableAssert.ThrowingCallable throwingCallable =
        () -> this.bedOccupancyService.handleBedOccupancy(bedOccupancy, token);

    Assertions.assertThatThrownBy(throwingCallable)
        .isInstanceOf(HoneypotException.class)
        .hasMessage(CONTENT_NOT_ACCEPTED);
  }
}
