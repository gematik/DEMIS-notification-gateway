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

package de.gematik.demis.notificationgateway.common.properties;

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
 * #L%
 */

import de.gematik.demis.notificationgateway.common.exceptions.BadRequestException;
import java.util.Arrays;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class TestUserPropertiesTest {

  private static final String TEST_USER_IP = "127.0.0.1";
  private static final String CLIENT_IP_HEADER = "x-real-ip";

  private final TestUserProperties props = new TestUserProperties();

  @BeforeEach
  void setupTestUserProperties() {
    props.setClientIpHeader(CLIENT_IP_HEADER);
    props.setIpAddress(Arrays.asList(TEST_USER_IP));
  }

  @Test
  void isTestIp() {
    Assertions.assertThat(props.isTestIp(TEST_USER_IP)).as("test user IP").isTrue();
    Assertions.assertThat(props.isTestIp("127.0.0.2")).as("normal user IP").isFalse();
  }

  @Test
  void shouldGetClientIpByExactEquals() throws BadRequestException {
    HttpHeaders headers = new HttpHeaders();
    headers.set(CLIENT_IP_HEADER, TEST_USER_IP);
    Assertions.assertThat(props.clientIp(headers))
        .as("client IP from HTTP headers")
        .isEqualTo(TEST_USER_IP);
  }

  @Test
  void shouldGetClientIpByCaseInsensitiveEquals() throws BadRequestException {
    HttpHeaders headers = new HttpHeaders();
    headers.set(CLIENT_IP_HEADER.toUpperCase(), TEST_USER_IP);
    Assertions.assertThat(props.clientIp(headers))
        .as("client IP from HTTP headers")
        .isEqualTo(TEST_USER_IP);
  }

  @Test
  void shouldGetClientIpByDifferentHeader() throws BadRequestException {
    this.props.setClientIpHeader("my-test-client-ip-header");
    HttpHeaders headers = new HttpHeaders();
    headers.set("my-Test-Client-IP-Header", TEST_USER_IP);
    Assertions.assertThat(props.clientIp(headers))
        .as("client IP from HTTP headers")
        .isEqualTo(TEST_USER_IP);
  }

  @Test
  void givenEmptyGematikIpsWhenIsTestIpThenReturnFalse() {
    props.setIpAddress(Arrays.asList());
    Assertions.assertThat(props.isTestIp(TEST_USER_IP)).as("empty test user IP").isFalse();
  }
}
