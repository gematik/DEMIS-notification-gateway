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

package de.gematik.demis.notificationgateway.domain.bedoccupancy;

import static de.gematik.demis.notificationgateway.common.constants.MessageConstants.VALIDATION_ERROR_OCCURRED;
import static de.gematik.demis.notificationgateway.common.constants.WebConstants.BED_OCCUPANCY_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.demis.notificationgateway.BaseTestUtils;
import de.gematik.demis.notificationgateway.common.constants.MessageConstants;
import de.gematik.demis.notificationgateway.common.constants.WebConstants;
import de.gematik.demis.notificationgateway.common.dto.ErrorResponse;
import de.gematik.demis.notificationgateway.common.enums.SupportedRealm;
import de.gematik.demis.notificationgateway.common.proxies.BundlePublisher;
import de.gematik.demis.notificationgateway.common.utils.Feature;
import de.gematik.demis.notificationgateway.security.oauth2.UnifiedOAuth2TokenValidator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@Slf4j
@AutoConfigureMockMvc
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BedOccupancyRestControllerWithoutExpireValidationIT implements BaseTestUtils {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired
  @Qualifier("ibmJwtDecoder")
  private NimbusJwtDecoder jwtDecoder;

  @Autowired private UnifiedOAuth2TokenValidator oAuth2TokenValidator;

  @MockBean private BundlePublisher bundlePublisher;

  @BeforeEach
  void init() {
    jwtDecoder.setJwtValidator(oAuth2TokenValidator);
  }

  @Test
  @Feature(files = "NG-002")
  void givenValidReportWhenPostBedOccupancyThen200() throws Exception {
    String path = "portal/bedoccupancy/report_content_max.json";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(EXPIRED_IBM_TOKEN);
    headers.put(WebConstants.HEADER_X_REAL_IP, List.of("62.23.239.123"));
    when(bundlePublisher.postRequest(
            any(), any(), any(), any(), any(), eq("rki.demis.r4.core"), eq("1.23.0")))
        .thenReturn(createJsonOkParameters("nes/nes_response_OK.json"));

    final MockHttpServletResponse response =
        this.mockMvc
            .perform(post(BED_OCCUPANCY_PATH).content(getJsonContent(path)).headers(headers))
            .andReturn()
            .getResponse();

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OK.value());
    assertThat(response.getContentAsString()).contains("All OK");

    Mockito.verify(bundlePublisher, times(1))
        .postRequest(
            any(Bundle.class),
            eq(SupportedRealm.HOSPITAL),
            any(String.class),
            any(String.class),
            eq("62.23.239.123"),
            eq("rki.demis.r4.core"),
            eq("1.23.0"));
  }

  @ParameterizedTest
  @CsvSource({
    "portal/bedoccupancy/invalid/no_location_id.json,notifierFacility.locationID,must not be null",
    "portal/bedoccupancy/invalid/empty_location_id.json,notifierFacility.locationID,size must be between 1 and 2147483647"
  })
  @Feature(files = "NG-002, NG-004")
  void givenInvalidReportWhenPostBedOccupancyThen400(
      String path, String field, String expectedMessage) throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(EXPIRED_IBM_TOKEN);
    headers.put(WebConstants.HEADER_X_REAL_IP, List.of("62.23.239.123"));

    final MockHttpServletResponse response =
        this.mockMvc
            .perform(post(BED_OCCUPANCY_PATH).content(getJsonContent(path)).headers(headers))
            .andReturn()
            .getResponse();

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.value());
    assertThat(response.getContentAsString()).isNotBlank();

    final ErrorResponse errorResponse =
        objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
    assertThat(errorResponse)
        .isNotNull()
        .hasFieldOrPropertyWithValue("message", VALIDATION_ERROR_OCCURRED)
        .hasFieldOrPropertyWithValue("path", BED_OCCUPANCY_PATH)
        .extracting(ErrorResponse::getValidationErrors)
        .asList()
        .hasSize(1)
        .first()
        .hasFieldOrPropertyWithValue("field", field)
        .hasFieldOrPropertyWithValue("message", expectedMessage);

    Mockito.verify(bundlePublisher, Mockito.never())
        .postRequest(
            any(Bundle.class),
            eq(SupportedRealm.HOSPITAL),
            any(String.class),
            any(String.class),
            anyString(),
            eq("rki.demis.r4.core"),
            eq("1.23.0"));
  }

  @Test
  void givenHoneypotContentWhenPostBedOccupancyThen406() throws Exception {
    String path = "portal/bedoccupancy/invalid/honeypot_bedOccupancy.json";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(EXPIRED_IBM_TOKEN);
    headers.put(WebConstants.HEADER_X_REAL_IP, List.of("62.23.239.123"));

    final MockHttpServletResponse response =
        this.mockMvc
            .perform(post(BED_OCCUPANCY_PATH).content(getJsonContent(path)).headers(headers))
            .andReturn()
            .getResponse();

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(NOT_ACCEPTABLE.value());
    assertThat(response.getContentAsString()).isNotBlank();
    final ErrorResponse errorResponse =
        objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
    assertThat(errorResponse)
        .isNotNull()
        .hasFieldOrPropertyWithValue("message", MessageConstants.CONTENT_NOT_ACCEPTED)
        .hasFieldOrPropertyWithValue("path", BED_OCCUPANCY_PATH)
        .extracting("validationErrors")
        .isNull();

    Mockito.verify(bundlePublisher, Mockito.never())
        .postRequest(
            any(Bundle.class),
            eq(SupportedRealm.HOSPITAL),
            any(String.class),
            any(String.class),
            anyString(),
            eq("rki.demis.r4.core"),
            eq("1.23.0"));
  }
}
