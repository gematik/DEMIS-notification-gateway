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

package de.gematik.demis.notificationgateway.domain.pathogen.services;

import static de.gematik.demis.notificationgateway.common.constants.MessageConstants.CONTENT_NOT_ACCEPTED;
import static de.gematik.demis.notificationgateway.utils.FileUtils.loadJsonFromFile;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.demis.notificationgateway.BaseTestUtils;
import de.gematik.demis.notificationgateway.common.dto.OkResponse;
import de.gematik.demis.notificationgateway.common.dto.PathogenTest;
import de.gematik.demis.notificationgateway.common.exceptions.HoneypotException;
import de.gematik.demis.notificationgateway.common.properties.NESProperties;
import de.gematik.demis.notificationgateway.common.proxies.BundlePublisher;
import de.gematik.demis.notificationgateway.common.services.OkResponseService;
import de.gematik.demis.notificationgateway.domain.HeaderProperties;
import de.gematik.demis.notificationgateway.domain.pathogen.mappers.PathogenMapper;
import java.io.IOException;
import java.net.URISyntaxException;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PathogenSendServiceTest implements BaseTestUtils {
  private final BundlePublisher bundlePublisher = mock(BundlePublisher.class);
  private final NESProperties nesProperties = mock(NESProperties.class);
  private final OkResponseService okResponseService = new OkResponseService();
  private final PathogenMapper mapper = new PathogenMapper();
  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  private final HeaderProperties headerPropertiesMock = mock(HeaderProperties.class);
  private final PathogenSendService service =
      new PathogenSendService(
          bundlePublisher, okResponseService, mapper, nesProperties, headerPropertiesMock);
  private String jsonContent;

  @BeforeEach
  void init() throws URISyntaxException, IOException {
    when(headerPropertiesMock.getLaboratoryNotificationProfile()).thenReturn("core");
    when(headerPropertiesMock.getLaboratoryNotificationVersion()).thenReturn("1.0.0");

    when(bundlePublisher.postRequest(any(), any(), any(), any(), any(), eq("core"), eq("1.0.0")))
        .thenReturn(createJsonOkParameters("nes/nes_response_OK.json"));
    when(nesProperties.laboratoryUrl()).thenReturn(RandomStringUtils.randomAlphabetic(10));

    jsonContent = loadJsonFromFile("/portal/pathogen/pathogen-test.json");
    assert jsonContent != null;
  }

  @Test
  void givenValidNotificationWhenSendThenOkResponse() throws IOException {

    PathogenTest pathogenTest = objectMapper.readValue(jsonContent, PathogenTest.class);

    final OkResponse response = service.send(pathogenTest, RandomStringUtils.randomAlphabetic(5));

    Assertions.assertThat(response)
        .isNotNull()
        .extracting(OkResponse::getStatus)
        .isEqualTo("All OK");
  }

  @Test
  void givenHoneyPotNotificationWhenSendThenError() throws IOException {

    PathogenTest pathogenTest = objectMapper.readValue(jsonContent, PathogenTest.class);
    pathogenTest.getNotifierFacility().oneTimeCode(RandomStringUtils.randomAlphabetic(5));

    final ThrowableAssert.ThrowingCallable throwingCallable =
        () -> service.send(pathogenTest, RandomStringUtils.randomAlphabetic(5));

    Assertions.assertThatThrownBy(throwingCallable)
        .isInstanceOf(HoneypotException.class)
        .hasMessage(CONTENT_NOT_ACCEPTED);
  }
}
