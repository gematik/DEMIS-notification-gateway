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

package de.gematik.demis.notificationgateway.domain.hospitalization.service;

import static de.gematik.demis.notificationgateway.common.enums.SupportedRealm.LAB;

import de.gematik.demis.notificationgateway.common.dto.Hospitalization;
import de.gematik.demis.notificationgateway.common.dto.OkResponse;
import de.gematik.demis.notificationgateway.common.exceptions.BadRequestException;
import de.gematik.demis.notificationgateway.common.exceptions.HoneypotException;
import de.gematik.demis.notificationgateway.common.properties.NESProperties;
import de.gematik.demis.notificationgateway.common.proxies.BundlePublisher;
import de.gematik.demis.notificationgateway.common.services.OkResponseService;
import de.gematik.demis.notificationgateway.common.utils.HoneypotUtils;
import de.gematik.demis.notificationgateway.domain.HeaderProperties;
import de.gematik.demis.notificationgateway.domain.hospitalization.fhir.DiseaseBundleCreationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.springframework.stereotype.Service;

@Slf4j
@AllArgsConstructor
@Service
public class HospitalizationService {
  private final DiseaseBundleCreationService bundleCreationService;
  private final BundlePublisher bundlePublisher;
  private final OkResponseService okResponseService;
  private final NESProperties nesProperties;

  private final HeaderProperties headerProperties;

  public OkResponse sendNotification(Hospitalization hospitalization, String remoteAddress)
      throws BadRequestException {

    final boolean isSpammer = HoneypotUtils.isHospitalizationSpammer(hospitalization);
    if (isSpammer) {
      throw new HoneypotException();
    }

    final Bundle bundle = bundleCreationService.createDiseaseBundle(hospitalization);
    final String url = nesProperties.hospitalizationUrl();
    final String operation = NESProperties.OPERATION_NAME;
    log.info("Sending request to {}, operation: {}", "NES", operation);
    Parameters result =
        bundlePublisher.postRequest(
            bundle,
            LAB,
            url,
            operation,
            remoteAddress,
            headerProperties.getDiseaseNotificationProfile(),
            headerProperties.getDiseaseNotificationVersion());

    return okResponseService.buildOkResponse(result);
  }
}
