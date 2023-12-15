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

package de.gematik.demis.notificationgateway.domain.pathogen;

import static de.gematik.demis.notificationgateway.common.constants.WebConstants.HEADER_X_REAL_IP;
import static de.gematik.demis.notificationgateway.common.constants.WebConstants.PATHOGEN_PATH;
import static de.gematik.demis.notificationgateway.utils.FileUtils.loadJsonFromFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.demis.notificationgateway.BaseTestUtils;
import de.gematik.demis.notificationgateway.common.constants.MessageConstants;
import de.gematik.demis.notificationgateway.common.dto.CodeDisplay;
import de.gematik.demis.notificationgateway.common.dto.ErrorResponse;
import de.gematik.demis.notificationgateway.common.dto.OkResponse;
import de.gematik.demis.notificationgateway.common.dto.PathogenData;
import de.gematik.demis.notificationgateway.common.properties.NESProperties;
import de.gematik.demis.notificationgateway.common.utils.Reachability;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import org.springframework.test.web.servlet.MockMvc;

@Slf4j
@AutoConfigureMockMvc
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIf(expression = "${testing.enable-e2e}", loadContext = true)
class PathogenRestControllerE2eIT implements BaseTestUtils {

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

  @Test
  void givenValidCodeWhenFindByCodeThenPathogenAnd200() throws Exception {
    String code = "invp";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    final MockHttpServletResponse response =
        this.mockMvc
            .perform(get(PATHOGEN_PATH + "/{code}", code).headers(headers))
            .andReturn()
            .getResponse();

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OK.value());
    assertThat(response.getContentAsString()).isNotBlank();

    final PathogenData pathogenData =
        objectMapper.readValue(response.getContentAsString(), PathogenData.class);

    assertThat(pathogenData).isNotNull();
    assertThat(pathogenData.getCodeDisplay()).hasFieldOrPropertyWithValue("code", "invp");
    assertThat(pathogenData.getMaterials()).isNotEmpty();
    assertThat(pathogenData.getMethods()).isNotEmpty();
  }

  @Test
  void givenInValidCodeWhenFindByCodeThen501() throws Exception {
    final String code = RandomStringUtils.randomAlphabetic(4);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    final MockHttpServletResponse response =
        this.mockMvc
            .perform(get(PATHOGEN_PATH + "/{code}", code).headers(headers))
            .andReturn()
            .getResponse();

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(NOT_IMPLEMENTED.value());
    assertThat(response.getContentAsString()).isNotBlank();

    final ErrorResponse errorResponse =
        objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);

    assertThat(errorResponse)
        .isNotNull()
        .hasFieldOrPropertyWithValue("statusCode", NOT_IMPLEMENTED.value());
  }

  @Test
  void givenValidRequestWhenFindAllThenPathogenListAnd200() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    final MockHttpServletResponse response =
        this.mockMvc.perform(get(PATHOGEN_PATH).headers(headers)).andReturn().getResponse();

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OK.value());
    assertThat(response.getContentAsString()).isNotBlank();

    final List<CodeDisplay> pathogens =
        objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});

    assertThat(pathogens)
        .as(
            "total number of entries for notification categories is limited through the settings of the fhir-ui-data-model-translation-service")
        .isNotEmpty()
        .hasSize(63);
  }

  @Test
  void givenValidPathogenTestWhenSendThen200() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.set(HEADER_X_REAL_IP, "62.23.239.123");
    headers.setContentType(MediaType.APPLICATION_JSON);

    final String jsonContent = loadJsonFromFile("/portal/pathogen/pathogen-test.json");
    assert jsonContent != null;
    final MockHttpServletResponse response =
        this.mockMvc
            .perform(post(PATHOGEN_PATH).headers(headers).content(jsonContent).with(csrf()))
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
  void givenHoneypotContentWhenPostPathogenThen406() throws Exception {
    String path = "portal/pathogen/invalid/honeypot_pathogenTest.json";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.put(HEADER_X_REAL_IP, List.of("62.23.239.123"));

    final MockHttpServletResponse response =
        this.mockMvc
            .perform(
                post(PATHOGEN_PATH).content(getJsonContent(path)).headers(headers).with(csrf()))
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
        .hasFieldOrPropertyWithValue("path", PATHOGEN_PATH)
        .extracting("validationErrors")
        .isNull();
  }
}
