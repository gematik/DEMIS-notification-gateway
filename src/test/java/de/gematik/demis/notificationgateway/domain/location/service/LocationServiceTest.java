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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.demis.notificationgateway.BaseTestUtils;
import de.gematik.demis.notificationgateway.common.dto.LocationDTO;
import de.gematik.demis.notificationgateway.common.exceptions.BadRequestException;
import de.gematik.demis.notificationgateway.domain.location.proxies.LocationsProxy;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest implements BaseTestUtils {

  @Mock private LocationsProxy locationsProxy;
  @Mock private Environment environment;
  @InjectMocks private LocationService locationService;

  @Test
  void givenTokenWithNoIkAndNoPreferredUsernameClaimsThenFindTestLocationsWithTestIk()
      throws BadRequestException {
    final String ik = RandomStringUtils.randomNumeric(9);
    when(environment.getProperty(TEST_IK_PROPERTY)).thenReturn(ik);
    when(locationsProxy.findByIK(ik)).thenReturn(ResponseEntity.ok(List.of(createLocation())));

    final List<LocationDTO> locations =
        locationService.findLocations(TOKEN_WITH_NO_IK_AND_PREFERRED_USERNAME);

    assertThat(locations).isNotEmpty().hasSize(1);
    verify(environment, times(1)).getProperty(TEST_IK_PROPERTY);
    verify(locationsProxy, times(1)).findByIK(ik);
  }

  @Test
  void givenValidPreferredUsernameAndMissingIkClaimWhenFindLocationsThenLocations()
      throws BadRequestException {
    // missing ik claim and preferred_username = "5-2-260550131"
    final String ik = "260550131";
    when(environment.getProperty(TEST_IK_PROPERTY)).thenReturn(ik);
    when(locationsProxy.findByIK(ik)).thenReturn(ResponseEntity.ok(List.of(createLocation())));

    final List<LocationDTO> locations =
        locationService.findLocations(TOKEN_WITH_NO_IK_AND_PREFERRED_USERNAME_5_2_260550131);

    assertThat(locations).isNotEmpty().hasSize(1);
    verify(environment, times(1)).getProperty(TEST_IK_PROPERTY);
    verify(locationsProxy, times(1)).findByIK(ik);
  }

  @Test
  void givenMissingIKAndWrongPreferredUsernameWhenFindLocationsThenError() {
    // missing ik claim and invalid preferred_username
    final String ik = RandomStringUtils.randomNumeric(9);
    when(environment.getProperty(TEST_IK_PROPERTY)).thenReturn(null);

    assertThatThrownBy(
            () ->
                locationService.findLocations(
                    TOKEN_WITH_NO_IK_AND_PREFERRED_USERNAME_5_3_123456789))
        .hasMessageContaining(FAILED_TO_EXTRACT_IK_NUMBER)
        .isInstanceOf(BadRequestException.class);

    verify(environment, times(1)).getProperty(TEST_IK_PROPERTY);
    verify(locationsProxy, times(0)).findByIK(ik);
  }
}
