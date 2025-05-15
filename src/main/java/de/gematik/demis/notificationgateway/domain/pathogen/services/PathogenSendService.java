package de.gematik.demis.notificationgateway.domain.pathogen.services;

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

import de.gematik.demis.notificationgateway.common.dto.OkResponse;
import de.gematik.demis.notificationgateway.common.dto.PathogenTest;
import de.gematik.demis.notificationgateway.common.exceptions.HoneypotException;
import de.gematik.demis.notificationgateway.common.properties.NESProperties;
import de.gematik.demis.notificationgateway.common.proxies.BundlePublisher;
import de.gematik.demis.notificationgateway.common.services.OkResponseService;
import de.gematik.demis.notificationgateway.common.utils.Token;
import de.gematik.demis.notificationgateway.domain.HeaderProperties;
import de.gematik.demis.notificationgateway.domain.pathogen.enums.LaboratoryNotificationType;
import de.gematik.demis.notificationgateway.domain.pathogen.fhir.PathogenBundleCreationService;
import jakarta.security.auth.message.AuthException;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PathogenSendService {

  private final BundlePublisher bundlePublisher;
  private final OkResponseService okResponseService;
  private final PathogenBundleCreationService pathogenBundleCreationService;
  private final NESProperties nesProperties;
  private final HeaderProperties headerProperties;

  private static void verifyHoneypot(PathogenTest pathogenTest) {
    if (isSpammer(pathogenTest)) {
      log.warn(
          "SECURITY ALERT: Honeypot oneTimeCode detected! Potential spam or malicious activity in pathogen notification.");
      throw new HoneypotException();
    }
  }

  private static boolean isSpammer(PathogenTest pathogenTest) {
    return checkIfStringIsNotBlack(pathogenTest.getNotifierFacility().getOneTimeCode());
  }

  private static boolean checkIfStringIsNotBlack(String... a) {
    return Arrays.stream(a).anyMatch(StringUtils::isNotBlank);
  }

  /**
   * @deprecated
   * @param pathogenTest
   * @param token
   * @return
   */
  @Deprecated(forRemoval = true)
  public OkResponse send(PathogenTest pathogenTest, Token token) {
    verifyHoneypot(pathogenTest);
    final Bundle bundle = pathogenBundleCreationService.toBundle(pathogenTest);
    final String url = nesProperties.laboratoryUrl();
    final String operation = NESProperties.OPERATION_NAME;
    log.info("Sending request to {}, operation: {}", "NES", operation);
    Parameters result =
        bundlePublisher.postRequest(
            bundle,
            url,
            operation,
            headerProperties.getLaboratoryNotificationProfile(),
            headerProperties.getLaboratoryNotificationVersion(),
            token);
    return okResponseService.buildOkResponse(result);
  }

  public OkResponse processPortalNotificationData(
      PathogenTest pathogenTest, Token token, LaboratoryNotificationType laboratoryNotificationType)
      throws AuthException {
    verifyHoneypot(pathogenTest);
    final Bundle bundle =
        pathogenBundleCreationService.toBundle(pathogenTest, laboratoryNotificationType);
    final String url = nesProperties.laboratoryUrl();
    final String operation = NESProperties.OPERATION_NAME;
    log.info("Sending request to {}, operation: {}", "NES", operation);
    Parameters result =
        bundlePublisher.postRequest(
            bundle,
            url,
            operation,
            headerProperties.getLaboratoryNotificationProfile(),
            headerProperties.getLaboratoryNotificationVersion(),
            token);
    return okResponseService.buildOkResponse(result);
  }
}
