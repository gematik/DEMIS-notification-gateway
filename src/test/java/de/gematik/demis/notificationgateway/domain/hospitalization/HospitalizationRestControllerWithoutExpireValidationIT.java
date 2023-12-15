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

package de.gematik.demis.notificationgateway.domain.hospitalization;

import static de.gematik.demis.notificationgateway.common.constants.MessageConstants.CLIENT_ADDRESS_EMPTY;
import static de.gematik.demis.notificationgateway.common.constants.MessageConstants.MISSING_VACCINATION_DATE_OF;
import static de.gematik.demis.notificationgateway.common.constants.WebConstants.HEADER_X_REAL_IP;
import static de.gematik.demis.notificationgateway.common.constants.WebConstants.HOSPITALIZATION_PATH;
import static de.gematik.demis.notificationgateway.common.dto.VaccinationInfo.VaccineEnum.VALNEVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;
import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.demis.notificationgateway.BaseTestUtils;
import de.gematik.demis.notificationgateway.common.constants.MessageConstants;
import de.gematik.demis.notificationgateway.common.dto.ErrorResponse;
import de.gematik.demis.notificationgateway.common.dto.OkResponse;
import de.gematik.demis.notificationgateway.common.properties.NESProperties;
import de.gematik.demis.notificationgateway.common.utils.Feature;
import de.gematik.demis.notificationgateway.common.utils.Reachability;
import de.gematik.demis.notificationgateway.security.oauth2.UnifiedOAuth2TokenValidator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assumptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import org.springframework.test.web.servlet.MockMvc;

@Slf4j
@AutoConfigureMockMvc
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIf(expression = "${testing.enable-e2e}", loadContext = true)
class HospitalizationRestControllerWithoutExpireValidationIT implements BaseTestUtils {

  private static final String BASE_PATH = "portal/disease/";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private NESProperties nesProperties;

  @BeforeEach
  void assumeReachableNes() {
    String url = this.nesProperties.getBaseUrl();
    Assumptions.assumeThat(new Reachability().test(url))
        .as("DEMIS DEV-stage NES is reachable")
        .isTrue();
  }

  @Autowired
  @Qualifier("ibmJwtDecoder")
  private NimbusJwtDecoder ibmJwtDecoder;

  @Autowired
  @Qualifier("demisJwtDecoder")
  private NimbusJwtDecoder demisJwtDecoder;

  @Autowired private UnifiedOAuth2TokenValidator oAuth2TokenValidator;

  @BeforeEach
  void init() {
    ibmJwtDecoder.setJwtValidator(oAuth2TokenValidator);
    demisJwtDecoder.setJwtValidator(oAuth2TokenValidator);
    System.setProperty("test-ik", "987654321");
  }

  @AfterEach
  void tearDown() {
    // De-Initialize test-ik
    System.setProperty("test-ik", "");
  }

  @Test
  void givenInvalidContentTypeWhenPostHospitalizationThen415() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(EXPIRED_IBM_TOKEN);
    headers.put(HttpHeaders.CONTENT_TYPE, List.of("application/fhir+json"));

    this.mockMvc
        .perform(post(HOSPITALIZATION_PATH).content(getHospitalizationString()).headers(headers))
        .andExpect(status().isUnsupportedMediaType());
  }

  @Test
  void givenAllowedOriginWhenPostHospitalizationThen200() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(EXPIRED_IBM_TOKEN);
    headers.setOrigin("http://localhost:4200");
    headers.put(HEADER_X_REAL_IP, List.of("123.456"));

    final MockHttpServletResponse response =
        this.mockMvc
            .perform(
                post(HOSPITALIZATION_PATH).content(getHospitalizationString()).headers(headers))
            .andReturn()
            .getResponse();

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OK.value());
    assertThat(response.getContentAsString()).isNotBlank();
    final OkResponse okResponse =
        objectMapper.readValue(response.getContentAsString(), OkResponse.class);
    assertThat(okResponse)
        .isNotNull()
        .hasNoNullFieldsOrProperties()
        .hasFieldOrPropertyWithValue("status", "All OK")
        .hasFieldOrPropertyWithValue("title", "Meldevorgangsquittung");

    String pdfText = pdfToText(okResponse.getContent());
    assertThat(pdfText).isNotBlank().contains("Meldungs-ID " + okResponse.getNotificationId());
  }

  @Test
  void givenInvalidDemisTokenWhenPostHospitalizationThen401WithMissingRoles() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(EXPIRED_DEMIS_LAB_TOKEN);
    headers.setOrigin("http://localhost:4200");
    headers.put(HEADER_X_REAL_IP, List.of("123.456"));

    final MockHttpServletResponse response =
        this.mockMvc
            .perform(
                post(HOSPITALIZATION_PATH).content(getHospitalizationString()).headers(headers))
            .andReturn()
            .getResponse();

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(UNAUTHORIZED.value());
    assertThat(response.getContentAsString()).isNotBlank();
    final ErrorResponse errorResponse =
        objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
    assertThat(errorResponse)
        .isNotNull()
        .hasFieldOrPropertyWithValue("message", "Invalid access role defined in JWT Token")
        .hasFieldOrPropertyWithValue("path", HOSPITALIZATION_PATH)
        .extracting("validationErrors")
        .isNull();
  }

  @Test
  void givenMissingXRealIPWhenPostHospitalizationThen400() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(EXPIRED_IBM_TOKEN);
    headers.put(HEADER_X_REAL_IP, List.of(""));

    final MockHttpServletResponse response =
        this.mockMvc
            .perform(
                post(HOSPITALIZATION_PATH).content(getHospitalizationString()).headers(headers))
            .andReturn()
            .getResponse();

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.value());
    assertThat(response.getContentAsString()).isNotBlank();
    final ErrorResponse errorResponse =
        objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
    assertThat(errorResponse)
        .isNotNull()
        .hasFieldOrPropertyWithValue("message", CLIENT_ADDRESS_EMPTY + " Header: x-real-ip")
        .hasFieldOrPropertyWithValue("path", HOSPITALIZATION_PATH)
        .extracting("validationErrors")
        .isNull();
  }

  @Test
  void givenInvalidMethodWhenRequestHospitalizationThen405() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(EXPIRED_IBM_TOKEN);
    headers.setContentType(MediaType.APPLICATION_JSON);

    final MockHttpServletResponse response =
        this.mockMvc.perform(get(HOSPITALIZATION_PATH).headers(headers)).andReturn().getResponse();

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(METHOD_NOT_ALLOWED.value());
    assertThat(response.getContentAsString()).isNotBlank();
    final ErrorResponse errorResponse =
        objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
    assertThat(errorResponse)
        .isNotNull()
        .hasFieldOrPropertyWithValue("message", "Request method 'GET' not supported")
        .hasFieldOrPropertyWithValue("path", HOSPITALIZATION_PATH)
        .extracting(ErrorResponse::getValidationErrors)
        .isNull();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "notification-unknown-use-case.json",
        "notification-dead-use-case.json",
        "notification-military-use-case.json",
        "notification_all_supported_vaccines.json",
        "encounter/notification_content_hospitalization_limited.json",
        "notification-missing-vaccinations-use-case.json",
        "notification-not-community-register-vaccination-without-date.json",
        "notification-not-community-register-vaccination-with-date.json"
      })
  @Feature(files = {"NG-006", "NG-007"})
  void givenDiversValidUseCasesWhenPostHospitalizationThen200(String fileName) throws Exception {
    String path = BASE_PATH + fileName;
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(EXPIRED_IBM_TOKEN);
    headers.put(HEADER_X_REAL_IP, List.of("62.23.239.123"));

    final MockHttpServletResponse response =
        this.mockMvc
            .perform(post(HOSPITALIZATION_PATH).content(getJsonContent(path)).headers(headers))
            .andReturn()
            .getResponse();

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OK.value());
    assertThat(response.getContentAsString()).isNotBlank();

    final OkResponse okResponse =
        objectMapper.readValue(response.getContentAsString(), OkResponse.class);
    assertThat(okResponse)
        .isNotNull()
        .hasNoNullFieldsOrProperties()
        .hasFieldOrPropertyWithValue("status", "All OK")
        .hasFieldOrPropertyWithValue("title", "Meldevorgangsquittung")
        .hasFieldOrPropertyWithValue("authorName", "DEMIS");

    String pdfText = pdfToText(okResponse.getContent());
    assertThat(pdfText).isNotBlank().contains("Meldungs-ID " + okResponse.getNotificationId());
  }

  @Test
  @Feature(files = "NG-004")
  void givenCommunityRegisterVaccineWithoutDateWhenPostHospitalizationThen400() throws Exception {
    String path = BASE_PATH + "notification-missing-vaccination-date-use-case.json";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(EXPIRED_IBM_TOKEN);
    headers.put(HEADER_X_REAL_IP, List.of("62.23.239.123"));

    final MockHttpServletResponse response =
        this.mockMvc
            .perform(post(HOSPITALIZATION_PATH).content(getJsonContent(path)).headers(headers))
            .andReturn()
            .getResponse();

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.value());
    assertThat(response.getContentAsString()).isNotBlank();
    final ErrorResponse errorResponse =
        objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
    assertThat(errorResponse)
        .isNotNull()
        .hasNoNullFieldsOrPropertiesExcept("validationErrors")
        .hasFieldOrPropertyWithValue("message", MISSING_VACCINATION_DATE_OF + VALNEVA)
        .hasFieldOrPropertyWithValue("path", HOSPITALIZATION_PATH);
  }

  @Test
  void givenHoneypotContentWhenPostHospitalizationThen406() throws Exception {
    String path = "portal/disease/invalid/honeypot_hospitalization.json";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(EXPIRED_IBM_TOKEN);
    headers.put(HEADER_X_REAL_IP, List.of("62.23.239.123"));

    final MockHttpServletResponse response =
        this.mockMvc
            .perform(post(HOSPITALIZATION_PATH).content(getJsonContent(path)).headers(headers))
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
        .hasFieldOrPropertyWithValue("path", HOSPITALIZATION_PATH)
        .extracting("validationErrors")
        .isNull();
  }
}
