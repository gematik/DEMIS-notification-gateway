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

package de.gematik.demis.notificationgateway.domain.location.service;

import static de.gematik.demis.notificationgateway.common.constants.MessageConstants.FAILED_TO_EXTRACT_IK_NUMBER;
import static de.gematik.demis.notificationgateway.common.utils.JwtUtils.TEST_IK_PROPERTY;
import static de.gematik.demis.notificationgateway.common.utils.JwtUtils.extractIkNumber;

import de.gematik.demis.notificationgateway.common.dto.LocationDTO;
import de.gematik.demis.notificationgateway.common.exceptions.BadRequestException;
import de.gematik.demis.notificationgateway.domain.location.proxies.LocationsProxy;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LocationService {
  private final LocationsProxy locationsProxy;
  private final Environment environment;

  public List<LocationDTO> findLocations(String token) throws BadRequestException {
    final String testIKProperty = environment.getProperty(TEST_IK_PROPERTY);
    final String ik = extractIkNumber(token, testIKProperty);
    // ik is required
    if (StringUtils.isBlank(ik)) {
      throw new BadRequestException(FAILED_TO_EXTRACT_IK_NUMBER);
    }

    return locationsProxy.findByIK(ik).getBody();
  }
}
