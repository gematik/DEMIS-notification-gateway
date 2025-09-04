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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.gematik.demis.notificationgateway.BaseTestUtils;
import de.gematik.demis.notificationgateway.common.constants.MessageConstants;
import de.gematik.demis.notificationgateway.common.dto.ErrorResponse;
import de.gematik.demis.notificationgateway.common.dto.OkResponse;
import de.gematik.demis.notificationgateway.common.dto.ValidationError;
import de.gematik.demis.notificationgateway.common.proxies.BundlePublisher;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
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
    properties = "feature.flag.notifications-73=true")
class PathogenRestControllerIT implements BaseTestUtils {

  @Autowired private ObjectMapper objectMapper;

  @Nested
  @DisplayName("§7.1 Pathogen Test")
  class Paragraph7_1 {
    public MockMvc mockMvc;
    @MockitoBean BundlePublisher bundlePublisher;

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
              .perform(
                  post("/api/ng/notification/pathogen/7.3/non_nominal")
                      .headers(headers)
                      .content(jsonContent)
                      .with(csrf()))
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
                  post(PATHOGEN_PATH + "/7.1")
                      .content(getJsonContent(path))
                      .headers(headers)
                      .with(csrf()))
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
          .hasFieldOrPropertyWithValue("path", "/api/ng/notification/pathogen/7.1")
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
                  post(PATHOGEN_PATH + "/7.1")
                      .content(getJsonContent(path))
                      .headers(headers)
                      .with(csrf()))
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
          .hasFieldOrPropertyWithValue("path", "/api/ng/notification/pathogen/7.1")
          .extracting("validationErrors")
          .asInstanceOf(InstanceOfAssertFactories.LIST)
          .hasSize(1)
          .first()
          .asInstanceOf(InstanceOfAssertFactories.type(ValidationError.class))
          .hasFieldOrPropertyWithValue("field", "pathogenDTO.specimenList");
    }
  }

  @Nested
  @DisplayName("§7.3 Pathogen Test non nominal")
  class Paragraph7_3_nonNominal {
    public MockMvc mockMvc;
    @MockitoBean BundlePublisher bundlePublisher;

    Paragraph7_3_nonNominal() throws JsonProcessingException {}

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

      final String jsonContent = loadJsonFromFile("/portal/pathogen/pathogen7_3DTO.json");
      assert jsonContent != null;
      final MockHttpServletResponse response =
          this.mockMvc
              .perform(
                  post(PATHOGEN_PATH + "/7.3/non_nominal")
                      .headers(headers)
                      .content(jsonContent)
                      .with(csrf()))
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

      // Capture the Bundle passed to the postRequest method
      ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
      verify(bundlePublisher)
          .postRequest(
              bundleCaptor.capture(), any(), any(), eq("fhir-profile-snapshots"), eq("v6"), any());

      // Convert the captured Bundle to JSON
      Bundle capturedBundle = bundleCaptor.getValue();
      String actualJson =
          FhirContext.forR4Cached()
              .newJsonParser()
              .setPrettyPrint(true)
              .encodeResourceToString(capturedBundle);

      // Load the expected JSON
      String expectedJson = loadJsonFromFile("/portal/pathogen/expected/pathogen-bundle-7.3.json");

      // Parse JSON and normalize dynamic fields
      ObjectMapper objectMapper = new ObjectMapper();
      JsonNode actualJsonNode = objectMapper.readTree(actualJson);
      JsonNode expectedJsonNode = objectMapper.readTree(expectedJson);

      normalizeDynamicFields(actualJsonNode);
      normalizeDynamicFields(expectedJsonNode);

      // Compare the normalized JSON
      assertThat(actualJsonNode).isEqualTo(expectedJsonNode);
    }

    private void normalizeDynamicFields(JsonNode node) {
      if (node.isObject()) {
        ObjectNode objectNode = (ObjectNode) node;

        // Replace dynamic fields with placeholders
        if (objectNode.has("id")) {
          objectNode.put("id", "PLACEHOLDER_ID");
        }
        if (objectNode.has("timestamp")) {
          objectNode.put("timestamp", "PLACEHOLDER_TIMESTAMP");
        }
        if (objectNode.has("fullUrl")) {
          String fullUrl = objectNode.get("fullUrl").asText();
          objectNode.put("fullUrl", fullUrl.replaceAll("[a-f0-9\\-]{36}", "PLACEHOLDER_ID"));
        }
        if (objectNode.has("reference")) {
          String reference = objectNode.get("reference").asText();
          objectNode.put("reference", reference.replaceAll("[a-f0-9\\-]{36}", "PLACEHOLDER_ID"));
        }
        if (objectNode.has("value")) {
          String value = objectNode.get("value").asText();
          objectNode.put("value", value.replaceAll("[a-f0-9\\-]{36}", "PLACEHOLDER_ID"));
        }
        if (objectNode.has("issued")) {
          objectNode.put("issued", "PLACEHOLDER_ISSUED_DATE");
        }
        if (objectNode.has("date")) {
          objectNode.put("date", "PLACEHOLDER_DATE");
        }

        // Recursively normalize nested fields
        objectNode.fields().forEachRemaining(entry -> normalizeDynamicFields(entry.getValue()));
      } else if (node.isArray()) {
        for (JsonNode arrayElement : node) {
          normalizeDynamicFields(arrayElement);
        }
      }
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
                  post("/api/ng/notification/pathogen/7.3/non_nominal")
                      .content(getJsonContent(path))
                      .headers(headers)
                      .with(csrf()))
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
          .hasFieldOrPropertyWithValue("path", "/api/ng/notification/pathogen/7.3/non_nominal")
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
                  post("/api/ng/notification/pathogen/7.3/non_nominal")
                      .content(getJsonContent(path))
                      .headers(headers)
                      .with(csrf()))
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
          .hasFieldOrPropertyWithValue("path", "/api/ng/notification/pathogen/7.3/non_nominal")
          .extracting("validationErrors")
          .asInstanceOf(InstanceOfAssertFactories.LIST)
          .hasSize(1)
          .first()
          .asInstanceOf(InstanceOfAssertFactories.type(ValidationError.class))
          .hasFieldOrPropertyWithValue("field", "pathogenDTO.specimenList");
    }
  }
}
