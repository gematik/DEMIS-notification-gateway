package de.gematik.demis.notificationgateway.domain.pathogen;

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

import static de.gematik.demis.notificationgateway.common.constants.WebConstants.API_NG_NOTIFICATION;
import static de.gematik.demis.notificationgateway.domain.pathogen.enums.LaboratoryNotificationType.ANONYMOUS;
import static de.gematik.demis.notificationgateway.domain.pathogen.enums.LaboratoryNotificationType.LAB;
import static de.gematik.demis.notificationgateway.domain.pathogen.enums.LaboratoryNotificationType.NON_NOMINAL;

import de.gematik.demis.notificationgateway.common.dto.OkResponse;
import de.gematik.demis.notificationgateway.common.dto.PathogenTest;
import de.gematik.demis.notificationgateway.common.exceptions.BadRequestException;
import de.gematik.demis.notificationgateway.common.utils.Token;
import de.gematik.demis.notificationgateway.domain.pathogen.services.PathogenSendService;
import jakarta.security.auth.message.AuthException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping(path = API_NG_NOTIFICATION)
public class PathogenRestController {

  private final PathogenSendService sendService;
  private final Boolean notification7_3Active;

  public PathogenRestController(
      PathogenSendService sendService,
      @Value("${feature.flag.notification7_3}") Boolean notification7_3Active) {
    this.sendService = sendService;
    this.notification7_3Active = notification7_3Active;
  }

  @PostMapping({"/pathogen", "/pathogen/7_1"})
  ResponseEntity<OkResponse> send(
      @RequestBody @Valid PathogenTest pathogenTest, @RequestHeader HttpHeaders headers)
      throws AuthException {

    if (notification7_3Active) {
      return ResponseEntity.ok(
          sendService.processPortalNotificationData(pathogenTest, Token.of(headers), LAB));
    } else {
      return ResponseEntity.ok(sendService.send(pathogenTest, Token.of(headers)));
    }
  }

  @PostMapping("/pathogen/7_3/non_nominal")
  ResponseEntity<OkResponse> send7_3_non_nominal(
      @RequestBody @Valid PathogenTest pathogenTest, @RequestHeader HttpHeaders headers)
      throws BadRequestException, AuthException {

    return ResponseEntity.ok(
        sendService.processPortalNotificationData(pathogenTest, Token.of(headers), NON_NOMINAL));
  }

  @PostMapping("/pathogen/7_3/anonymous")
  ResponseEntity<OkResponse> send7_3_anonymous(
      @RequestBody @Valid PathogenTest pathogenTest, @RequestHeader HttpHeaders headers)
      throws BadRequestException, AuthException {

    return ResponseEntity.ok(
        sendService.processPortalNotificationData(pathogenTest, Token.of(headers), ANONYMOUS));
  }
}
