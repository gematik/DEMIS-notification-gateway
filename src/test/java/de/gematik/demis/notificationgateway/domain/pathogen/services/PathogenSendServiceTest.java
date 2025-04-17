package de.gematik.demis.notificationgateway.domain.pathogen.services;

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

import static de.gematik.demis.notificationgateway.common.constants.MessageConstants.CONTENT_NOT_ACCEPTED;
import static de.gematik.demis.notificationgateway.utils.FileUtils.loadJsonFromFile;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.demis.notificationgateway.BaseTestUtils;
import de.gematik.demis.notificationgateway.common.dto.OkResponse;
import de.gematik.demis.notificationgateway.common.dto.PathogenTest;
import de.gematik.demis.notificationgateway.common.exceptions.HoneypotException;
import de.gematik.demis.notificationgateway.common.properties.NESProperties;
import de.gematik.demis.notificationgateway.common.proxies.BundlePublisher;
import de.gematik.demis.notificationgateway.common.services.OkResponseService;
import de.gematik.demis.notificationgateway.common.utils.Token;
import de.gematik.demis.notificationgateway.domain.HeaderProperties;
import de.gematik.demis.notificationgateway.domain.pathogen.fhir.PathogenBundleCreationService;
import java.io.IOException;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PathogenSendServiceTest implements BaseTestUtils {

  private final OkResponseService okResponseService = new OkResponseService();
  private final PathogenBundleCreationService mapper = new PathogenBundleCreationService();
  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @Mock private BundlePublisher bundlePublisher;
  @Mock private NESProperties nesProperties;
  @Mock private HeaderProperties headerPropertiesMock;
  @Mock private Token token;
  private PathogenSendService service;
  private String jsonContent;
  private static RandomStringUtils random = RandomStringUtils.secure();

  @BeforeEach
  void createService() {
    service =
        new PathogenSendService(
            bundlePublisher, okResponseService, mapper, nesProperties, headerPropertiesMock);
  }

  @BeforeEach
  void createJsonContent() {
    jsonContent = loadJsonFromFile("/portal/pathogen/pathogen-test.json");
    assert jsonContent != null;
  }

  private void mockPostResponse() throws Exception {
    when(headerPropertiesMock.getLaboratoryNotificationProfile()).thenReturn("core");
    when(headerPropertiesMock.getLaboratoryNotificationVersion()).thenReturn("1.0.0");
    when(bundlePublisher.postRequest(any(), any(), any(), eq("core"), eq("1.0.0"), any()))
        .thenReturn(createJsonOkParameters("nes/nes_response_OK.json"));
    when(nesProperties.laboratoryUrl()).thenReturn(random.nextAlphabetic(10));
  }

  @Test
  void givenValidNotificationWhenSendThenOkResponse() throws Exception {
    mockPostResponse();
    PathogenTest pathogenTest = objectMapper.readValue(jsonContent, PathogenTest.class);

    final OkResponse response = service.send(pathogenTest, token);

    Assertions.assertThat(response)
        .isNotNull()
        .extracting(OkResponse::getStatus)
        .isEqualTo("All OK");
  }

  @Test
  void givenHoneyPotNotifierFacilityWhenSendThenError() throws IOException {
    PathogenTest pathogenTest = objectMapper.readValue(jsonContent, PathogenTest.class);
    pathogenTest.getNotifierFacility().oneTimeCode(random.nextAlphabetic(5));
    verifyHoneyPotDetection(pathogenTest);
  }

  private void verifyHoneyPotDetection(PathogenTest pathogenTest) {
    final ThrowableAssert.ThrowingCallable throwingCallable =
        () -> service.send(pathogenTest, token);
    Assertions.assertThatThrownBy(throwingCallable)
        .isInstanceOf(HoneypotException.class)
        .hasMessage(CONTENT_NOT_ACCEPTED);
  }
}
