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

package de.gematik.demis.notificationgateway.domain.location.proxies;

import de.gematik.demis.notificationgateway.common.dto.LocationDTO;
import de.gematik.demis.notificationgateway.common.proxies.configuration.CustomFeignConfigurator;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Spring Feign Client to communicate with the Hospital Location Service, that is behind a nginx
 * Instance, with a self-signed certificate. At the moment the TLS Validation of the certificate is
 * skipped, since it requires a valid configuration of nginx itself and this application, whose
 * configured Truststore is read from Classpath, not from the file system.
 */
@FeignClient(
    name = "hospital-location-service",
    url = "${hospital-location-service.url}",
    configuration = CustomFeignConfigurator.class)
public interface LocationsProxy {
  @GetMapping("/hospital-locations")
  ResponseEntity<List<LocationDTO>> findByIK(@RequestParam("ik") String ik);
}
