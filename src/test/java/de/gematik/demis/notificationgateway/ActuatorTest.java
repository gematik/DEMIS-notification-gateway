package de.gematik.demis.notificationgateway;

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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

// AutoConfigureMockMvc is deprecated
@Disabled
@SpringBootTest()
@AutoConfigureMockMvc
class ActuatorTest {
  @Autowired private MockMvc mockMvc;

  @Test
  void givenHealthIsUnsecuredWhenGetThenReturn200() throws Exception {
    mockMvc
        .perform(get("/actuator/health").with(csrf()))
        .andExpect(status().is2xxSuccessful())
        .andExpect(jsonPath("$.status", is("UP")))
        .andExpect(jsonPath("$.groups", contains("liveness", "readiness")));
  }

  @Test
  void givenLivenessIsUnsecuredWhenGetThenReturn200() throws Exception {
    mockMvc
        .perform(get("/actuator/health/liveness").with(csrf()))
        .andExpect(status().is2xxSuccessful())
        .andExpect(jsonPath("$.status", is("UP")));
  }

  @Test
  void givenReadinessIsUnsecuredWhenGetThenReturn200() throws Exception {
    mockMvc
        .perform(get("/actuator/health/readiness").with(csrf()))
        .andExpect(status().is2xxSuccessful())
        .andExpect(jsonPath("$.status", is("UP")));
  }
}
