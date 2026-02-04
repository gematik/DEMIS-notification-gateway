package de.gematik.demis.notificationgateway.domain.bedoccupancy.service;

/*-
 * #%L
 * DEMIS Notification-Gateway
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
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
 * For additional notes and disclaimer from gematik and in case of changes by gematik,
 * find details in the "Readme" file.
 * #L%
 */

import de.gematik.demis.notificationgateway.common.dto.BedOccupancy;
import de.gematik.demis.notificationgateway.common.dto.OkResponse;
import de.gematik.demis.notificationgateway.common.exceptions.HoneypotException;
import de.gematik.demis.notificationgateway.common.properties.RPSProperties;
import de.gematik.demis.notificationgateway.common.proxies.BundlePublisher;
import de.gematik.demis.notificationgateway.common.services.OkResponseService;
import de.gematik.demis.notificationgateway.common.utils.Token;
import de.gematik.demis.notificationgateway.domain.bedoccupancy.fhir.ReportBundleCreationService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.springframework.stereotype.Service;

@Slf4j
@AllArgsConstructor
@Service
public class BedOccupancyService {

  private final ReportBundleCreationService bundleCreationService;
  private final BundlePublisher bundlePublisher;
  private final OkResponseService okResponseService;
  private final RPSProperties rpsProperties;

  private final HttpServletRequest request;

  private static void validateHoneypot(BedOccupancy bedOccupancy) {
    if (isSpammer(bedOccupancy)) {
      log.warn(
          "SECURITY ALERT: Honeypot oneTimeCode detected! Potential spam or malicious activity in bed-occupancy report.");
      throw new HoneypotException();
    }
  }

  private static boolean isSpammer(BedOccupancy bedOccupancy) {
    return checkIfStringIsNotBlack(bedOccupancy.getNotifierFacility().getOneTimeCode());
  }

  private static boolean checkIfStringIsNotBlack(String... a) {
    return Arrays.stream(a).anyMatch(StringUtils::isNotBlank);
  }

  public OkResponse handleBedOccupancy(BedOccupancy bedOccupancy, Token token) {
    validateHoneypot(bedOccupancy);
    final Bundle bundle = bundleCreationService.createReportBundle(bedOccupancy);
    final String url = rpsProperties.bedOccupancyUrl();
    final String operation = RPSProperties.OPERATION_NAME;
    log.info("Sending request to {}, operation: {}", "RPS", operation);
    Parameters result = bundlePublisher.postRequest(bundle, url, operation, token, request);
    return okResponseService.buildOkResponse(result);
  }
}
