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

package de.gematik.demis.notificationgateway.domain.pathogen;

import static de.gematik.demis.notificationgateway.common.constants.WebConstants.API_NG_NOTIFICATION;

import de.gematik.demis.notificationgateway.common.dto.CodeDisplay;
import de.gematik.demis.notificationgateway.common.dto.OkResponse;
import de.gematik.demis.notificationgateway.common.dto.PathogenData;
import de.gematik.demis.notificationgateway.common.dto.PathogenTest;
import de.gematik.demis.notificationgateway.common.exceptions.BadRequestException;
import de.gematik.demis.notificationgateway.common.properties.TestUserProperties;
import de.gematik.demis.notificationgateway.domain.pathogen.services.PathogenSearchService;
import de.gematik.demis.notificationgateway.domain.pathogen.services.PathogenSendService;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(path = API_NG_NOTIFICATION)
public class PathogenRestController {

  private final TestUserProperties testUserProperties;
  private final PathogenSendService sendService;
  private final PathogenSearchService searchService;

  @GetMapping("/pathogen/{code}")
  @NewSpan
  ResponseEntity<PathogenData> findByCode(@PathVariable String code) {
    // order 2
    return ResponseEntity.ok(searchService.findByCode(code));
  }

  @GetMapping("/pathogen")
  @NewSpan
  ResponseEntity<List<CodeDisplay>> findAll() {
    // order 1
    return ResponseEntity.ok(searchService.findAll());
  }

  @PostMapping("/pathogen")
  @NewSpan
  ResponseEntity<OkResponse> send(
      @RequestBody @Valid PathogenTest pathogenTest, @RequestHeader Map<String, String> headers)
      throws BadRequestException {
    // order 3
    return ResponseEntity.ok(
        sendService.send(pathogenTest, this.testUserProperties.clientIp(headers)));
  }
}
