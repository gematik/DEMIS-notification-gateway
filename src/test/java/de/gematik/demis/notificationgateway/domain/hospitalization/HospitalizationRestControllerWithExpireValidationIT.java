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

import static de.gematik.demis.notificationgateway.common.constants.WebConstants.HOSPITALIZATION_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.demis.notificationgateway.BaseTestUtils;
import de.gematik.demis.notificationgateway.common.dto.ErrorResponse;
import de.gematik.demis.notificationgateway.common.enums.SupportedRealm;
import de.gematik.demis.notificationgateway.common.proxies.BundlePublisher;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@Slf4j
@AutoConfigureMockMvc
@ActiveProfiles("test")
@SpringBootTest
class HospitalizationRestControllerWithExpireValidationIT implements BaseTestUtils {

  @MockBean BundlePublisher bundlePublisher;
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void givenNotAllowedOriginWhenPostHospitalizationThen403() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setOrigin("http://www.not-allowed.example");

    MockHttpServletResponse errorResponse =
        this.mockMvc
            .perform(
                post(HOSPITALIZATION_PATH).content(getHospitalizationString()).headers(headers))
            .andReturn()
            .getResponse();

    assertThat(errorResponse).isNotNull();
    assertThat(errorResponse.getStatus()).isEqualTo(FORBIDDEN.value());
    assertThat(errorResponse.getContentAsString()).isEqualTo("Invalid CORS request");

    Mockito.verify(bundlePublisher, Mockito.never())
        .postRequest(
            any(Bundle.class),
            any(SupportedRealm.class),
            any(String.class),
            any(String.class),
            anyString(),
            eq("rki.demis.r4.core"),
            eq("1.23.0"));
  }

  @Test
  void givenExpiredBearerTokenWhenPostHospitalizationThen401() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(EXPIRED_IBM_TOKEN);

    MockHttpServletResponse response =
        this.mockMvc
            .perform(
                post(HOSPITALIZATION_PATH).content(getHospitalizationString()).headers(headers))
            .andReturn()
            .getResponse();

    assertThat(response.getStatus()).isEqualTo(UNAUTHORIZED.value());

    Mockito.verify(bundlePublisher, Mockito.never())
        .postRequest(
            any(Bundle.class),
            any(SupportedRealm.class),
            any(String.class),
            any(String.class),
            anyString(),
            eq("rki.demis.r4.core"),
            eq("1.23.0"));
  }

  @Test
  void givenTokenFromOtherIDPWhenPostHospitalizationThen401WrongIssuer() throws Exception {
    String accessToken =
        "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJHX0RSY085c"
            + "ll1VG1TYVhRWGo2OFlLblhpd2ZJT3lUZEpOQ2hZZjZTeWJ3In0.eyJleHAiOjE2NDY0MTI3Mjc"
            + "sImlhdCI6MTY0NjQxMjY2NywianRpIjoiYTE4MGZmZDUtNjg2OS00OTU1LWE5ZWYtYTZjZWUwO"
            + "TBiZjk4IiwiaXNzIjoiaHR0cHM6Ly9kZW1pcy1pbnQucmtpLmRlL2F1dGgvcmVhbG1zL21hc3R"
            + "lciIsInN1YiI6ImFmMmZmZjZhLWJlNGMtNGIxYS04Mjg3LWZhYjIyMTNjOWQxMiIsInR5cCI6I"
            + "kJlYXJlciIsImF6cCI6ImFkbWluLWNsaSIsInNlc3Npb25fc3RhdGUiOiJiODNkNjViNS00OWF"
            + "mLTQ3YWQtOTQyZC0yNjc3MmUzZDI5MWUiLCJhY3IiOiIxIiwic2NvcGUiOiJwcm9maWxlIGVtY"
            + "WlsIiwic2lkIjoiYjgzZDY1YjUtNDlhZi00N2FkLTk0MmQtMjY3NzJlM2QyOTFlIiwiZW1haWx"
            + "fdmVyaWZpZWQiOmZhbHNlLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJrbWdlbWF0aWsifQ.WylIIH"
            + "eGLfQ8KYzSFtGYca1VnsnBTc6gH34-PrL6tdz5LJFdSX9Z482AUjoyYIFDTwp3Qs6Eoh5hKA3z"
            + "TrZUtn-59RueIzUle-OLz1-5gWVR2OJfi_D3FqarA3ey8ztBpoXNd3hFNen2ty2jR34Sm5kL3a"
            + "DGA1hHZPTTYFkIxTRQSWtW6gyseHO0bIVdzvBEsia84SH_KoWJF388d7wiyykG_H3ppVFxdUkT"
            + "03AhIDywFk1TTSjDCTFaFA7d_IOjLkJ7ftSsPFf2JrX_8AV1a9aFobeLObXwwSVmyP9o2jsha9"
            + "uPfbGwk3dV7L_L6vHP-Gw1-UBGrqlVILL8bCBxuA";

    HttpHeaders headers = new HttpHeaders();
    headers.add("x-real-ip", "127.0.0.1");
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(accessToken);

    MockHttpServletResponse response =
        this.mockMvc
            .perform(
                post(HOSPITALIZATION_PATH).content(getHospitalizationString()).headers(headers))
            .andReturn()
            .getResponse();

    assertThat(response.getStatus()).isEqualTo(UNAUTHORIZED.value());

    ErrorResponse errorResponse =
        objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);

    assertThat(errorResponse)
        .isNotNull()
        .hasNoNullFieldsOrPropertiesExcept("validationErrors")
        .hasFieldOrPropertyWithValue("message", "Invalid issuer")
        .hasFieldOrPropertyWithValue("statusCode", UNAUTHORIZED.value())
        .hasFieldOrPropertyWithValue("path", HOSPITALIZATION_PATH);

    Mockito.verify(bundlePublisher, Mockito.never())
        .postRequest(
            any(Bundle.class),
            any(SupportedRealm.class),
            any(String.class),
            any(String.class),
            anyString(),
            eq("rki.demis.r4.core"),
            eq("1.23.0"));
  }

  @Test
  void givenMissingAuthorizationHeaderWhenPostHospitalizationThen401() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    MockHttpServletResponse response =
        this.mockMvc
            .perform(
                post(HOSPITALIZATION_PATH)
                    .content(getHospitalizationString())
                    .headers(headers)
                    .with(csrf()))
            .andReturn()
            .getResponse();

    assertThat(response.getStatus()).isEqualTo(UNAUTHORIZED.value());

    ErrorResponse errorResponse =
        objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);

    assertThat(errorResponse)
        .isNotNull()
        .hasNoNullFieldsOrPropertiesExcept("validationErrors")
        .hasFieldOrPropertyWithValue(
            "message", "Full authentication is required to access this resource")
        .hasFieldOrPropertyWithValue("statusCode", UNAUTHORIZED.value())
        .hasFieldOrPropertyWithValue("path", HOSPITALIZATION_PATH);

    Mockito.verify(bundlePublisher, Mockito.never())
        .postRequest(
            any(Bundle.class),
            any(SupportedRealm.class),
            any(String.class),
            any(String.class),
            anyString(),
            eq("rki.demis.r4.core"),
            eq("1.23.0"));
  }

  @Test
  void givenEmptyBearerAuthorizationHeaderWhenPostHospitalizationThen401() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth("");

    MockHttpServletResponse response =
        this.mockMvc
            .perform(
                post(HOSPITALIZATION_PATH)
                    .content(getHospitalizationString())
                    .headers(headers)
                    .with(csrf()))
            .andReturn()
            .getResponse();

    assertThat(response.getStatus()).isEqualTo(UNAUTHORIZED.value());

    ErrorResponse errorResponse =
        objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);

    assertThat(errorResponse)
        .isNotNull()
        .hasNoNullFieldsOrPropertiesExcept("validationErrors")
        .hasFieldOrPropertyWithValue("message", "Bearer token is malformed")
        .hasFieldOrPropertyWithValue("statusCode", UNAUTHORIZED.value())
        .hasFieldOrPropertyWithValue("path", HOSPITALIZATION_PATH);

    Mockito.verify(bundlePublisher, Mockito.never())
        .postRequest(
            any(Bundle.class),
            any(SupportedRealm.class),
            any(String.class),
            any(String.class),
            anyString(),
            eq("rki.demis.r4.core"),
            eq("1.23.0"));
  }

  @Test
  void givenNonBearerAuthorizationHeaderWhenPostHospitalizationThen401() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBasicAuth("it should be a bearer token");

    MockHttpServletResponse response =
        this.mockMvc
            .perform(
                post(HOSPITALIZATION_PATH)
                    .content(getHospitalizationString())
                    .headers(headers)
                    .with(csrf()))
            .andReturn()
            .getResponse();

    assertThat(response.getStatus()).isEqualTo(UNAUTHORIZED.value());

    ErrorResponse errorResponse =
        objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);

    assertThat(errorResponse)
        .isNotNull()
        .hasNoNullFieldsOrPropertiesExcept("validationErrors")
        .hasFieldOrPropertyWithValue(
            "message", "Full authentication is required to access this resource")
        .hasFieldOrPropertyWithValue("statusCode", UNAUTHORIZED.value())
        .hasFieldOrPropertyWithValue("path", HOSPITALIZATION_PATH);

    Mockito.verify(bundlePublisher, Mockito.never())
        .postRequest(
            any(Bundle.class),
            any(SupportedRealm.class),
            any(String.class),
            any(String.class),
            anyString(),
            eq("rki.demis.r4.core"),
            eq("1.23.0"));
  }
}
