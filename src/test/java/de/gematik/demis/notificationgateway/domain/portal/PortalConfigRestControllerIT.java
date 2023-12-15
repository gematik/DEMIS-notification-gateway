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

package de.gematik.demis.notificationgateway.domain.portal;

import static de.gematik.demis.notificationgateway.common.constants.WebConstants.CONFIG_PORTAL_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@Slf4j
@AutoConfigureMockMvc
@SpringBootTest
class PortalConfigRestControllerIT {

  @Autowired private Environment env;
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void givenConfigPropertiesWhenGetPortalConfigsThen200() throws Exception {

    final MockHttpServletResponse response =
        this.mockMvc.perform(get(CONFIG_PORTAL_PATH)).andReturn().getResponse();

    assertThat(response.getStatus()).isEqualTo(200);

    final String content = response.getContentAsString();
    assertThat(content).isNotBlank();
    final PortalConfigProperties properties =
        objectMapper.readValue(content, PortalConfigProperties.class);
    assertThat(properties).hasNoNullFieldsOrProperties();

    final List<IdentityProvider> identityProviders = properties.getIdentityProviders();
    assertThat(identityProviders)
        .isNotEmpty()
        .hasSize(3)
        .extracting(IdentityProvider::getBaseUrl)
        .hasSize(3)
        .containsExactlyInAnyOrder(
            env.getProperty("config.notification-portal.identity-providers[0].base-url"),
            env.getProperty("config.notification-portal.identity-providers[1].base-url"),
            env.getProperty("config.notification-portal.identity-providers[2].base-url"));

    assertThat(identityProviders)
        .extracting(IdentityProvider::getTenant)
        .hasSize(3)
        .containsExactlyInAnyOrder(
            env.getProperty("config.notification-portal.identity-providers[0].tenant"),
            env.getProperty("config.notification-portal.identity-providers[1].tenant"),
            env.getProperty("config.notification-portal.identity-providers[2].tenant"));

    assertThat(identityProviders)
        .extracting(IdentityProvider::getClientId)
        .hasSize(3)
        .containsExactlyInAnyOrder(
            env.getProperty("config.notification-portal.identity-providers[0].client-id"),
            env.getProperty("config.notification-portal.identity-providers[1].client-id"),
            env.getProperty("config.notification-portal.identity-providers[2].client-id"));

    assertThat(identityProviders)
        .flatExtracting(IdentityProvider::getIssuers)
        .hasSize(3)
        .containsExactlyInAnyOrder(
            env.getProperty("config.notification-portal.identity-providers[0].issuers[0]"),
            env.getProperty("config.notification-portal.identity-providers[1].issuers[0]"),
            env.getProperty("config.notification-portal.identity-providers[2].issuers[0]"));

    assertThat(properties.getGatewayPaths())
        .hasSize(5)
        .containsExactlyInAnyOrderEntriesOf(
            Map.of(
                "bedOccupancy",
                env.getProperty("config.notification-portal.gateway-paths.bedOccupancy"),
                "hospitalLocations",
                env.getProperty("config.notification-portal.gateway-paths.hospitalLocations"),
                "hospitalization",
                env.getProperty("config.notification-portal.gateway-paths.hospitalization"),
                "laboratory",
                env.getProperty("config.notification-portal.gateway-paths.laboratory"),
                "pathogen",
                env.getProperty("config.notification-portal.gateway-paths.pathogen")));

    assertThat(properties.getFeatureFlags())
        .hasSize(3)
        .containsExactlyInAnyOrderEntriesOf(
            Map.of(
                "ssoAuthEnabled",
                env.getProperty(
                    "config.notification-portal.feature-flags.ssoAuthEnabled", Boolean.class),
                "pathogenTileIsOn",
                env.getProperty(
                    "config.notification-portal.feature-flags.pathogenTileIsOn", Boolean.class),
                "tokenInjectionEnabled",
                env.getProperty(
                    "config.notification-portal.feature-flags.tokenInjectionEnabled",
                    Boolean.class)));

    assertThat(properties.getNgxLoggerConfig())
        .extracting(NGXLoggerConfig::getLevel)
        .isEqualTo(
            env.getProperty("config.notification-portal.ngxloggerconfig.level", Integer.class));
    assertThat(properties.getNgxLoggerConfig())
        .extracting(NGXLoggerConfig::isDisableConsoleLogging)
        .isEqualTo(
            env.getProperty(
                "config.notification-portal.ngxloggerconfig.disableConsoleLogging", Boolean.class));
  }
}
