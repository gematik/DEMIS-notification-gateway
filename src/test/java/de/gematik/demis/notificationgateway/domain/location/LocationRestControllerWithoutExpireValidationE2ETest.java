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

import static de.gematik.demis.notificationgateway.common.constants.WebConstants.API_NG_SERVICES;
import static de.gematik.demis.notificationgateway.common.constants.WebConstants.HEADER_X_REAL_IP;
import static de.gematik.demis.notificationgateway.common.constants.WebConstants.HOSPITAL_LOCATIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.demis.notificationgateway.BaseTestUtils;
import de.gematik.demis.notificationgateway.common.dto.LocationDTO;
import de.gematik.demis.notificationgateway.security.oauth2.UnifiedOAuth2TokenValidator;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
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
@Disabled("HLS is not reachable, missing certificate to communicate with nginx")
class LocationRestControllerWithoutExpireValidationE2ETest implements BaseTestUtils {
  private static final String URL_LOCATIONS = API_NG_SERVICES + HOSPITAL_LOCATIONS;
  @Autowired private MockMvc mockMvc;

  @Autowired
  @Qualifier("ibmJwtDecoder")
  private NimbusJwtDecoder jwtDecoder;

  @Autowired private UnifiedOAuth2TokenValidator oAuth2TokenValidator;
  @Autowired private ObjectMapper objectMapper;

  @Value("classpath:hls-test-locations.json")
  private Resource resource;

  @BeforeEach
  void init() {
    jwtDecoder.setJwtValidator(oAuth2TokenValidator);
  }

  @Test
  void givenTestIKAndValidRequestWhenGetLocationsThen200() throws Exception {
    final List<LocationDTO> testLocations =
        objectMapper.readValue(resource.getInputStream(), new TypeReference<>() {});
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
  void givenTestIKWithoutLocationsAndValidRequestWhenGetLocationsThen200AndEmptyList()
      throws Exception {
    System.setProperty("test-ik", "999999999");
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
    System.setProperty("test-ik", "987654321");
  }
}
