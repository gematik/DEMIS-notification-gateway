package de.gematik.demis.notificationgateway.domain.pathogen;

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

import static de.gematik.demis.notificationgateway.common.constants.WebConstants.PATHOGEN_PATH;
import static de.gematik.demis.notificationgateway.utils.FileUtils.loadJsonFromFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.demis.notificationgateway.BaseTestUtils;
import de.gematik.demis.notificationgateway.common.constants.MessageConstants;
import de.gematik.demis.notificationgateway.common.dto.ErrorResponse;
import de.gematik.demis.notificationgateway.common.dto.OkResponse;
import de.gematik.demis.notificationgateway.common.dto.ValidationError;
import de.gematik.demis.notificationgateway.common.proxies.BundlePublisher;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "feature.flag.notifications.7_3=false")
class PathogenRestControllerRegressionIT implements BaseTestUtils {

  @MockitoBean BundlePublisher bundlePublisher;
  private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @BeforeEach
  void init(WebApplicationContext context) {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  @Test
  void givenValidPathogenTestWhenPostPathogenThen200() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth("token");
    headers.setContentType(MediaType.APPLICATION_JSON);

    when(bundlePublisher.postRequest(
            any(), any(), any(), eq("fhir-profile-snapshots"), eq("v6"), any()))
        .thenReturn(createJsonOkParameters("nes/nes_response_OK.json"));

    final String jsonContent = loadJsonFromFile("/portal/pathogen/specimenPrep.json");
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
    headers.setBearerAuth("token");
    headers.setContentType(MediaType.APPLICATION_JSON);

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
        .asInstanceOf(InstanceOfAssertFactories.LIST)
        .isNullOrEmpty();
  }

  @ParameterizedTest
  @CsvSource({
    "portal/pathogen/invalid/empty_specimen_list_pathogenTest.json",
    "portal/pathogen/invalid/without_specimen_list_pathogenTest.json"
  })
  void givenInvalidSpecimenListWhenPostPathogenThen400(String path) throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth("token");
    headers.setContentType(MediaType.APPLICATION_JSON);

    final MockHttpServletResponse response =
        this.mockMvc
            .perform(
                post(PATHOGEN_PATH).content(getJsonContent(path)).headers(headers).with(csrf()))
            .andReturn()
            .getResponse();

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.value());
    assertThat(response.getContentAsString()).isNotBlank();
    final ErrorResponse errorResponse =
        objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
    assertThat(errorResponse)
        .isNotNull()
        .hasFieldOrPropertyWithValue("message", MessageConstants.VALIDATION_ERROR_OCCURRED)
        .hasFieldOrPropertyWithValue("path", PATHOGEN_PATH)
        .extracting("validationErrors")
        .asInstanceOf(InstanceOfAssertFactories.LIST)
        .hasSize(1)
        .first()
        .asInstanceOf(InstanceOfAssertFactories.type(ValidationError.class))
        .hasFieldOrPropertyWithValue("field", "pathogenDTO.specimenList");
  }
}
