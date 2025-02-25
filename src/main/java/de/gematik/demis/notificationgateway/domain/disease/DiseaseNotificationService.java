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
 * #L%
 */

import static de.gematik.demis.notificationgateway.common.enums.SupportedRealm.LAB;

import de.gematik.demis.notificationgateway.common.dto.DiseaseNotification;
import de.gematik.demis.notificationgateway.common.dto.OkResponse;
import de.gematik.demis.notificationgateway.common.exceptions.BadRequestException;
import de.gematik.demis.notificationgateway.common.exceptions.HoneypotException;
import de.gematik.demis.notificationgateway.common.properties.NESProperties;
import de.gematik.demis.notificationgateway.common.proxies.BundlePublisher;
import de.gematik.demis.notificationgateway.common.request.Metadata;
import de.gematik.demis.notificationgateway.common.services.OkResponseService;
import de.gematik.demis.notificationgateway.domain.HeaderProperties;
import de.gematik.demis.notificationgateway.domain.disease.fhir.DiseaseNotificationBundleCreationService;
import jakarta.security.auth.message.AuthException;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
class DiseaseNotificationService {

  private static final String LOG_SEND =
      "Sending disease notification request to DEMIS. Operation: " + NESProperties.OPERATION_NAME;

  private final DiseaseNotificationBundleCreationService bundleCreationService;
  private final BundlePublisher bundlePublisher;
  private final OkResponseService okResponseService;
  private final NESProperties nesProperties;
  private final HeaderProperties headerProperties;

  private static void validateHoneypot(DiseaseNotification notification) {
    if (isSpammer(notification)) {
      log.warn(
          "SECURITY ALERT: Honeypot oneTimeCode detected! Potential spam or malicious activity in disease notification.");
      throw new HoneypotException();
    }
  }

  private static boolean isSpammer(DiseaseNotification notification) {
    return checkIfStringIsNotBlack(notification.getNotifierFacility().getOneTimeCode());
  }

  private static boolean checkIfStringIsNotBlack(String... a) {
    return Arrays.stream(a).anyMatch(StringUtils::isNotBlank);
  }

  /**
   * Convert disease notification to FHIR document. Send the FHIR document to DEMIS core API.
   *
   * @param notification disease notification
   * @param metadata inbound request metadata
   * @return response of DEMIS core
   * @throws BadRequestException illegal disease notification structure or data
   */
  OkResponse sendNotification(DiseaseNotification notification, Metadata metadata)
      throws BadRequestException, AuthException {
    validateHoneypot(notification);
    final Bundle bundle = this.bundleCreationService.createBundle(notification);
    final String url = this.nesProperties.hospitalizationUrl();
    final String operation = NESProperties.OPERATION_NAME;
    log.info(LOG_SEND);
    Parameters result =
        this.bundlePublisher.postRequest(
            bundle,
            LAB,
            url,
            operation,
            this.headerProperties.getDiseaseNotificationProfile(),
            this.headerProperties.getDiseaseNotificationVersion(),
            metadata);
    return this.okResponseService.buildOkResponse(result);
  }
}
