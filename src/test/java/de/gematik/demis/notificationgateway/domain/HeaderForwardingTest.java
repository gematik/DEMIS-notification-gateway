package de.gematik.demis.notificationgateway.domain;

/*-
 * #%L
 * DEMIS Notification-Gateway
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
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
 * For additional notes and disclaimer from gematik and in case of changes by gematik,
 * find details in the "Readme" file.
 * #L%
 */

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static de.gematik.demis.notificationgateway.common.constants.WebConstants.PATHOGEN_PATH;
import static de.gematik.demis.notificationgateway.utils.FileUtils.loadJsonFromFile;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    useMainMethod = SpringBootTest.UseMainMethod.ALWAYS,
    properties = {"notification.api.baseUrl=http://localhost:7070"})
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureWireMock(port = 0)
class HeaderForwardingTest {
  private MockMvc mockMvc;
  private static final WireMockServer NPS_SERVER = new WireMockServer(7070);

  @BeforeEach
  void init(WebApplicationContext context) {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

    NPS_SERVER.start();
    configureFor(NPS_SERVER.port());
    WireMock.reset();
  }

  @Test
  void testHeadersAreForwarded() throws Exception {
    final String jsonContent = loadJsonFromFile("/portal/pathogen/pathogen7_3DTO.json");

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth("token");
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("x-fhir-api-request-origin", "internal");
    headers.set("x-fhir-api-submission-type", "pathogen");
    headers.set("x-fhir-api-version", "v6");
    headers.set("x-fhir-profile", "fhir-profile-snapshots");

    Assertions.assertNotNull(jsonContent);
    mockMvc
        .perform(
            post(PATHOGEN_PATH + "/7.3/non_nominal")
                .content(jsonContent)
                .headers(headers)
                .with(csrf()))
        .andReturn()
        .getResponse();

    WireMock.verify(
        1,
        postRequestedFor(urlEqualTo("/notification-api/fhir/$process-notification"))
            .withHeader("x-fhir-api-request-origin", WireMock.equalTo("internal"))
            .withHeader("x-fhir-api-submission-type", WireMock.equalTo("pathogen"))
            .withHeader("x-fhir-api-version", WireMock.equalTo("v6"))
            .withHeader("x-fhir-profile", WireMock.equalTo("fhir-profile-snapshots")));
  }
}
