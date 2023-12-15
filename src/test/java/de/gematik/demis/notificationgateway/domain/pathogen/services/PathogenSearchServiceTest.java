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

package de.gematik.demis.notificationgateway.domain.pathogen.services;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.demis.notificationgateway.common.dto.PathogenData;
import de.gematik.demis.notificationgateway.domain.pathogen.proxies.FhirDataTranslationProxy;
import de.gematik.demis.notificationgateway.utils.FileUtils;
import java.io.IOException;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class PathogenSearchServiceTest {
  private final FhirDataTranslationProxy fhirDataTranslationProxy =
      mock(FhirDataTranslationProxy.class);

  private final PathogenSearchService service = new PathogenSearchService(fhirDataTranslationProxy);

  @Test
  void testFindByCode() throws IOException {

    final PathogenData expectedINVPPathogen =
        FileUtils.createPathogenData("/portal/pathogen/pathogen-invp.json");

    String code = RandomStringUtils.randomAlphabetic(5);
    when(fhirDataTranslationProxy.findByCode(code))
        .thenReturn(ResponseEntity.ok(expectedINVPPathogen));

    final PathogenData pathogenData = service.findByCode(code);

    Assertions.assertThat(pathogenData).isNotNull().isEqualTo(expectedINVPPathogen);
  }
}
