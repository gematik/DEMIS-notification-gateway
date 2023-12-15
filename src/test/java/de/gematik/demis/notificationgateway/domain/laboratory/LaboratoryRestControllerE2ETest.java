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

package de.gematik.demis.notificationgateway.domain.laboratory;

import static de.gematik.demis.notificationgateway.common.constants.WebConstants.HEADER_X_REAL_IP;
import static de.gematik.demis.notificationgateway.common.constants.WebConstants.LABORATORY_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.demis.notificationgateway.BaseTestUtils;
import de.gematik.demis.notificationgateway.common.dto.OkResponse;
import de.gematik.demis.notificationgateway.common.properties.NESProperties;
import de.gematik.demis.notificationgateway.common.utils.Reachability;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
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
@SpringBootTest
@EnabledIf(expression = "${testing.enable-e2e}", loadContext = true)
class LaboratoryRestControllerE2ETest implements BaseTestUtils {

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
  void givenValidQuickTestNotificationWhenPostLaboratoryThen200() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setOrigin("http://localhost:4200");
    headers.put(HEADER_X_REAL_IP, List.of("62.23.239.123"));

    final MockHttpServletResponse response =
        this.mockMvc
            .perform(
                post(LABORATORY_PATH).content(getLaboratoryString()).headers(headers).with(csrf()))
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
}
