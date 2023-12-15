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

import static de.gematik.demis.notificationgateway.common.constants.WebConstants.API_NG_NOTIFICATION;

import de.gematik.demis.notificationgateway.common.dto.OkResponse;
import de.gematik.demis.notificationgateway.common.dto.QuickTest;
import de.gematik.demis.notificationgateway.common.exceptions.BadRequestException;
import de.gematik.demis.notificationgateway.common.properties.TestUserProperties;
import de.gematik.demis.notificationgateway.domain.laboratory.service.LaboratoryService;
import java.util.Map;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@AllArgsConstructor
@Validated
@RequestMapping(path = API_NG_NOTIFICATION)
public class LaboratoryRestController {

  private final TestUserProperties testUserProperties;
  private final LaboratoryService laboratoryService;

  @PostMapping(
      path = "/laboratory",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = "application/json")
  @NewSpan
  public ResponseEntity<OkResponse> addLaboratoryNotification(
      @RequestBody final @Valid QuickTest content, @RequestHeader Map<String, String> headers)
      throws BadRequestException {
    log.debug("Received laboratory notification.");
    final OkResponse okResponse =
        laboratoryService.sendNotification(content, this.testUserProperties.clientIp(headers));
    log.info("Notification id: {}", okResponse.getNotificationId());
    log.debug("Sending response to portal with status code: 200");
    return ResponseEntity.ok(okResponse);
  }
}
