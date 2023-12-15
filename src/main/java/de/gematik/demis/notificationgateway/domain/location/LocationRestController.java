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
import static de.gematik.demis.notificationgateway.common.constants.WebConstants.HOSPITAL_LOCATIONS;

import de.gematik.demis.notificationgateway.common.dto.LocationDTO;
import de.gematik.demis.notificationgateway.common.exceptions.BadRequestException;
import de.gematik.demis.notificationgateway.common.properties.TestUserProperties;
import de.gematik.demis.notificationgateway.domain.location.service.LocationService;
import de.gematik.demis.notificationgateway.security.oauth2.authentication.AuthenticationFacade;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(API_NG_SERVICES)
@Validated
@RequiredArgsConstructor
@Slf4j
public class LocationRestController {

  private final TestUserProperties testUserProperties;
  private final LocationService locationService;
  private final AuthenticationFacade authenticationFacade;

  @GetMapping(HOSPITAL_LOCATIONS)
  @NewSpan
  ResponseEntity<List<LocationDTO>> findByIK(@RequestHeader Map<String, String> headers)
      throws BadRequestException {
    // client IP has to be set in headers
    String clientIp = this.testUserProperties.clientIp(headers);
    log.debug("Client IP address is: {}", clientIp);
    // get the token
    final String authenticationToken = authenticationFacade.getAuthenticationToken();
    final List<LocationDTO> locations = locationService.findLocations(authenticationToken);
    log.debug("locations for current principal: {}", locations.size());
    return ResponseEntity.ok(locations);
  }
}
