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

package de.gematik.demis.notificationgateway.domain.location;

import static de.gematik.demis.notificationgateway.common.constants.MessageConstants.CLIENT_ADDRESS_EMPTY;
import static de.gematik.demis.notificationgateway.common.constants.MessageConstants.FAILED_TO_EXTRACT_IK_NUMBER;
import static de.gematik.demis.notificationgateway.common.constants.WebConstants.API_NG_SERVICES;
import static de.gematik.demis.notificationgateway.common.constants.WebConstants.HEADER_X_REAL_IP;
import static de.gematik.demis.notificationgateway.common.constants.WebConstants.HOSPITAL_LOCATIONS;
import static de.gematik.demis.notificationgateway.common.enums.InternalCoreError.NG_300_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.demis.notificationgateway.BaseTestUtils;
import de.gematik.demis.notificationgateway.common.dto.ErrorResponse;
import de.gematik.demis.notificationgateway.common.dto.LocationDTO;
import de.gematik.demis.notificationgateway.common.utils.Feature;
import de.gematik.demis.notificationgateway.domain.location.proxies.LocationsProxy;
import de.gematik.demis.notificationgateway.security.oauth2.UnifiedOAuth2TokenValidator;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@Slf4j
@AutoConfigureMockMvc
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LocationRestControllerWithoutExpireValidationIT implements BaseTestUtils {
  private static final String URL_LOCATIONS = API_NG_SERVICES + HOSPITAL_LOCATIONS;
  @Autowired private MockMvc mockMvc;

  @Autowired
  @Qualifier("ibmJwtDecoder")
  private NimbusJwtDecoder jwtDecoder;

  @Autowired private UnifiedOAuth2TokenValidator oAuth2TokenValidator;
  @Autowired private ObjectMapper objectMapper;
  @MockBean private LocationsProxy locationsProxy;

  @Value("classpath:hls-test-locations.json")
  private Resource resource;

  @BeforeEach
  void init() {
    // Initialize test-ik
    System.setProperty("test-ik", "987654321");
    jwtDecoder.setJwtValidator(oAuth2TokenValidator);
  }

  @AfterEach
  void tearDown() {
    // De-Initialize test-ik
    System.setProperty("test-ik", "");
  }

  @Test
  @Feature(files = "NG-001")
  void givenTestIKAndValidRequestWhenGetLocationsThen200() throws Exception {
    final List<LocationDTO> testLocations =
        objectMapper.readValue(resource.getInputStream(), new TypeReference<>() {});
    when(locationsProxy.findByIK(anyString())).thenReturn(ResponseEntity.ok(testLocations));
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(EXPIRED_IBM_TOKEN);
    headers.put(HEADER_X_REAL_IP, List.of("62.23.239.123"));

    final MockHttpServletResponse response =
        this.mockMvc.perform(get(URL_LOCATIONS).headers(headers)).andReturn().getResponse();

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OK.value());
    assertThat(response.getContentAsString()).isNotBlank();

    final List<LocationDTO> locations =
        objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
    assertThat(locations)
        .isNotEmpty()
        .hasSize(6)
        .containsExactlyInAnyOrderElementsOf(testLocations);
  }

  @Test
  @Feature(files = "NG-001")
  void givenTestIKAndValidRequestWhenGetLocationsThen200AndEmptyList() throws Exception {
    when(locationsProxy.findByIK(anyString())).thenReturn(ResponseEntity.ok(List.of()));
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(EXPIRED_IBM_TOKEN);
    headers.put(HEADER_X_REAL_IP, List.of("62.23.239.123"));

    final MockHttpServletResponse response =
        this.mockMvc.perform(get(URL_LOCATIONS).headers(headers)).andReturn().getResponse();

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OK.value());
    assertThat(response.getContentAsString())
        .isNotBlank()
        .isEqualTo(Collections.EMPTY_LIST.toString());
  }

  @Test
  @Feature(files = "NG-001")
  void givenWrongIKAttributeWhenGetLocationsThen400() throws Exception {

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(EXPIRED_IBM_TOKEN);
    headers.put(HEADER_X_REAL_IP, List.of("62.23.239.123"));
    System.setProperty("test-ik", "-1");
    final MockHttpServletResponse response =
        this.mockMvc.perform(get(URL_LOCATIONS).headers(headers)).andReturn().getResponse();

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.value());
    assertThat(response.getContentAsString()).isNotBlank();
    final ErrorResponse errorResponse =
        objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);

    assertThat(errorResponse)
        .isNotNull()
        .hasFieldOrPropertyWithValue("message", FAILED_TO_EXTRACT_IK_NUMBER)
        .hasFieldOrPropertyWithValue("path", URL_LOCATIONS)
        .extracting("validationErrors")
        .isNull();
    System.setProperty("test-ik", "987654321");
  }

  @Test
  @Feature(files = "NG-001")
  void givenUnreachableHLSWhenGetLocationsThen500() throws Exception {
    doThrow(new FhirClientConnectionException("unreachable service"))
        .when(locationsProxy)
        .findByIK(anyString());
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(EXPIRED_IBM_TOKEN);
    headers.put(HEADER_X_REAL_IP, List.of("62.23.239.123"));
    final MockHttpServletResponse response =
        this.mockMvc.perform(get(URL_LOCATIONS).headers(headers)).andReturn().getResponse();

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR.value());
    assertThat(response.getContentAsString()).isNotBlank();
    final ErrorResponse errorResponse =
        objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);

    assertThat(errorResponse)
        .isNotNull()
        .hasFieldOrPropertyWithValue("message", NG_300_REQUEST.reason())
        .hasFieldOrPropertyWithValue("path", URL_LOCATIONS)
        .extracting("validationErrors")
        .isNull();
  }

  @Test
  void givenInvalidMethodWhenRequestLocationsThen405() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(EXPIRED_IBM_TOKEN);
    headers.put(HEADER_X_REAL_IP, List.of("62.23.239.123"));

    final MockHttpServletResponse response =
        this.mockMvc.perform(post(URL_LOCATIONS).headers(headers)).andReturn().getResponse();

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(METHOD_NOT_ALLOWED.value());
    assertThat(response.getContentAsString()).isNotBlank();
    final ErrorResponse errorResponse =
        objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
    assertThat(errorResponse)
        .isNotNull()
        .hasFieldOrPropertyWithValue("message", "Request method 'POST' not supported")
        .hasFieldOrPropertyWithValue("path", URL_LOCATIONS)
        .extracting("validationErrors")
        .isNull();
  }

  @Test
  void givenNotAllowedOriginWhenGetLocationsThen403() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(EXPIRED_IBM_TOKEN);
    headers.put(HEADER_X_REAL_IP, List.of("62.23.239.123"));
    headers.setOrigin("http://www.not-allowed.example");

    MockHttpServletResponse errorResponse =
        this.mockMvc
            .perform(get(URL_LOCATIONS).content(getLaboratoryString()).headers(headers))
            .andReturn()
            .getResponse();

    assertThat(errorResponse).isNotNull();
    assertThat(errorResponse.getStatus()).isEqualTo(FORBIDDEN.value());
    assertThat(errorResponse.getContentAsString()).isEqualTo("Invalid CORS request");
  }

  @Test
  void givenMissingRealIpHeaderWhenGetLocationsThen400() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(EXPIRED_IBM_TOKEN);

    final MockHttpServletResponse response =
        this.mockMvc.perform(get(URL_LOCATIONS).headers(headers)).andReturn().getResponse();

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.value());
    assertThat(response.getContentAsString()).isNotBlank();
    final ErrorResponse errorResponse =
        objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);

    assertThat(errorResponse)
        .isNotNull()
        .hasFieldOrPropertyWithValue("message", CLIENT_ADDRESS_EMPTY + " Header: x-real-ip")
        .hasFieldOrPropertyWithValue("path", URL_LOCATIONS)
        .extracting("validationErrors")
        .isNull();
  }

  @Test
  void givenEmptyXRealIPWhenGetLocationsThen400() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(EXPIRED_IBM_TOKEN);
    headers.put(HEADER_X_REAL_IP, List.of(""));

    final MockHttpServletResponse response =
        this.mockMvc
            .perform(get(URL_LOCATIONS).content(getLaboratoryString()).headers(headers))
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
        .hasFieldOrPropertyWithValue("path", URL_LOCATIONS)
        .extracting("validationErrors")
        .isNull();
  }
}
