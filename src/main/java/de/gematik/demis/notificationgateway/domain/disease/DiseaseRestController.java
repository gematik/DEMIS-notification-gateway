package de.gematik.demis.notificationgateway.domain.disease;

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

import de.gematik.demis.notificationgateway.common.constants.WebConstants;
import de.gematik.demis.notificationgateway.common.dto.DiseaseNotification;
import de.gematik.demis.notificationgateway.common.dto.OkResponse;
import de.gematik.demis.notificationgateway.common.exceptions.BadRequestException;
import de.gematik.demis.notificationgateway.common.utils.Token;
import jakarta.security.auth.message.AuthException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = WebConstants.API_NG_NOTIFICATION)
class DiseaseRestController {

  private final Validator validator;
  private final DiseaseNotificationService notificationService;

  @PostMapping(
      path = "/disease",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = "application/json")
  public ResponseEntity<OkResponse> addDiseaseNotification(
      @RequestBody DiseaseNotification notification, @RequestHeader HttpHeaders headers)
      throws BadRequestException, AuthException {
    final long startMillis = System.currentTimeMillis();
    log.debug("Received disease notification.");
    validate(notification);
    OkResponse okResponse = send(notification, headers);
    log.info(
        "Processed disease notification! Id: {} Duration: {}ms",
        okResponse.getNotificationId(),
        (System.currentTimeMillis() - startMillis));
    log.debug("Sending response to portal with status code: 200");
    return ResponseEntity.ok(okResponse);
  }

  private void validate(DiseaseNotification notification) {
    Set<ConstraintViolation<ValidationDiseaseNotification>> violations =
        this.validator.validate(ValidationDiseaseNotification.of(notification));
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }

  private OkResponse send(DiseaseNotification content, HttpHeaders headers)
      throws BadRequestException, AuthException {
    return this.notificationService.sendNotification(content, Token.of(headers));
  }
}
