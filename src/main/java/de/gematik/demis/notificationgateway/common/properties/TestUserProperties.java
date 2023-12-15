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

import de.gematik.demis.notificationgateway.common.constants.MessageConstants;
import de.gematik.demis.notificationgateway.common.exceptions.BadRequestException;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@Slf4j
public class TestUserProperties {

  @Value("${testuser.token.client.username}")
  private String username;

  @Value("${testuser.auth.cert.password}")
  private String authCertPassword;

  @Value("${testuser.auth.cert.keystore}")
  private String authCertPath;

  @Value("${testuser.auth.cert.alias}")
  private String authCertAlias;

  @Value("#{'${notification.gematik.ip}'.split(',')}")
  private List<String> ipAddress;

  @Value("${notification.headers.client.ip:x-real-ip}")
  private String clientIpHeader;

  public boolean isTestIp(String remoteAddress) {
    if (StringUtils.isEmpty(remoteAddress) || this.ipAddress.contains(remoteAddress)) {
      log.debug("Detected testing IP address: {}", remoteAddress);
      return true;
    }
    return false;
  }

  /**
   * Get client IP from headers
   *
   * @param headers HTTP request message headers
   * @return value of header with client IP address
   * @throws BadRequestException client IP address is missing
   */
  public String clientIp(Map<String, String> headers) throws BadRequestException {
    String key =
        headers.keySet().stream()
            .filter(k -> StringUtils.equalsIgnoreCase(k, this.clientIpHeader))
            .findFirst()
            .orElseThrow(this::createMissingClientIpException);
    String clientIp = headers.get(key);
    if (StringUtils.isBlank(clientIp)) {
      throw createMissingClientIpException();
    }
    return clientIp;
  }

  private BadRequestException createMissingClientIpException() {
    return new BadRequestException(
        MessageConstants.CLIENT_ADDRESS_EMPTY + " Header: " + this.clientIpHeader);
  }
}
