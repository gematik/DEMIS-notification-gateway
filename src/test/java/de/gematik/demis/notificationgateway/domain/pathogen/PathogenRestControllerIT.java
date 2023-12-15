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
import static de.gematik.demis.notificationgateway.common.enums.InternalCoreError.NG_300_REQUEST;
import static de.gematik.demis.notificationgateway.utils.FileUtils.loadJsonFromFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE;
import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;
import static org.springframework.http.HttpStatus.OK;
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
import de.gematik.demis.notificationgateway.common.proxies.BundlePublisher;
import de.gematik.demis.notificationgateway.domain.pathogen.proxies.FhirDataTranslationProxy;
import de.gematik.demis.notificationgateway.utils.FileUtils;
import feign.FeignException;
import feign.Request;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@Slf4j
@AutoConfigureMockMvc
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PathogenRestControllerIT implements BaseTestUtils {

  @MockBean BundlePublisher bundlePublisher;
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockBean private FhirDataTranslationProxy fhirDataTranslationProxy;

  @Test
  void givenValidCodeWhenFindByCodeThenPathogenAnd200() throws Exception {
    String code = "invp";
    final PathogenData expectedPathogenData =
        FileUtils.createPathogenData("/portal/pathogen/pathogen-invp.json");
    when(fhirDataTranslationProxy.findByCode(code))
        .thenReturn(ResponseEntity.ok(expectedPathogenData));
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

    assertThat(pathogenData).isNotNull().isEqualTo(expectedPathogenData);
  }

  @Test
  void givenInValidCodeWhenFindByCodeThen501() throws Exception {
    final String code = "abcd";
    final String message = "pathogen not found";
    final String requestUri = RandomStringUtils.randomAlphabetic(10);
    final Request feignRequest =
        Request.create(Request.HttpMethod.GET, requestUri, Map.of(), null, null, null);

    final FeignException.NotImplemented feignException =
        new FeignException.NotImplemented(message, feignRequest, message.getBytes(), Map.of());
    doThrow(feignException).when(fhirDataTranslationProxy).findByCode(code);
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
        .hasFieldOrPropertyWithValue("statusCode", NOT_IMPLEMENTED.value())
        .hasFieldOrPropertyWithValue("message", message)
        .hasFieldOrPropertyWithValue("path", requestUri);
  }

  @Test
  void givenValidRequestWhenFindAllThenPathogenListAnd200() throws Exception {
    final List<CodeDisplay> expectedPathogens =
        List.of(
            FileUtils.createPathogenData("/portal/pathogen/pathogen-invp.json").getCodeDisplay(),
            FileUtils.createPathogenData("/portal/pathogen/pathogen-advp.json").getCodeDisplay());
    when(fhirDataTranslationProxy.findAll()).thenReturn(ResponseEntity.ok(expectedPathogens));
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    final MockHttpServletResponse response =
        this.mockMvc.perform(get(PATHOGEN_PATH).headers(headers)).andReturn().getResponse();

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OK.value());
    assertThat(response.getContentAsString()).isNotBlank();

    final List<CodeDisplay> pathogens =
        objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});

    assertThat(pathogens).isNotNull().hasSize(2).isEqualTo(expectedPathogens);
  }

  @Test
  void givenUnreachableServiceWhenFindAllThen500() throws Exception {
    final String message = "time out";
    final String requestUri = RandomStringUtils.randomAlphabetic(10);
    final Request feignRequest =
        Request.create(Request.HttpMethod.GET, requestUri, Map.of(), null, null, null);
    final String body =
        objectMapper
            .createObjectNode()
            .put("error", NG_300_REQUEST.reason())
            .put("path", requestUri)
            .toString();
    final FeignException.GatewayTimeout feignException =
        new FeignException.GatewayTimeout(message, feignRequest, body.getBytes(), Map.of());
    doThrow(feignException).when(fhirDataTranslationProxy).findAll();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    final MockHttpServletResponse response =
        this.mockMvc.perform(get(PATHOGEN_PATH).headers(headers)).andReturn().getResponse();

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR.value());
    assertThat(response.getContentAsString()).isNotBlank();

    final ErrorResponse errorResponse =
        objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);

    assertThat(errorResponse)
        .isNotNull()
        .hasFieldOrPropertyWithValue("statusCode", INTERNAL_SERVER_ERROR.value())
        .hasFieldOrPropertyWithValue("message", NG_300_REQUEST.reason())
        .hasFieldOrPropertyWithValue("path", requestUri);
  }

  @Test
  void givenValidPathogenTestWhenSendThen200() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.set(HEADER_X_REAL_IP, "123");
    headers.setContentType(MediaType.APPLICATION_JSON);

    when(bundlePublisher.postRequest(
            any(), any(), any(), any(), any(), eq("rki.demis.r4.core"), eq("1.23.0")))
        .thenReturn(createJsonOkParameters("nes/nes_response_OK.json"));

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
