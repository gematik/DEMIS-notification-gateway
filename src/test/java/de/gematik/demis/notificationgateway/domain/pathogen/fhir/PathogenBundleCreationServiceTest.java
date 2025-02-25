package de.gematik.demis.notificationgateway.domain.pathogen.fhir;

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

import ca.uhn.fhir.context.FhirContext;
import de.gematik.demis.notification.builder.demis.fhir.notification.utils.Utils;
import de.gematik.demis.notificationgateway.common.dto.PathogenTest;
import de.gematik.demis.notificationgateway.utils.FileUtils;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PathogenBundleCreationServiceTest {

  private final PathogenBundleCreationService pathogenBundleCreationService =
      new PathogenBundleCreationService(true, false, false);
  private final PathogenBundleCreationService withAddressFeature =
      new PathogenBundleCreationService(true, true, false);
  private final PathogenBundleCreationService withMultipleSpecimenFeature =
      new PathogenBundleCreationService(true, true, true);

  private int counter;

  @ParameterizedTest
  @CsvSource({
    "portal/pathogen/pathogen-test.json, portal/pathogen/pathogen-test-bundle.json",
    "portal/pathogen/pathogen-test-prefix.json, portal/pathogen/pathogen-test-bundle-prefix.json",
    "portal/pathogen/pathogen-test-salutation.json, portal/pathogen/pathogen-test-bundle-salutation.json",
    "portal/pathogen/pathogen-test-salutation-prefix.json, portal/pathogen/pathogen-test-bundle-salutation-prefix.json",
    "portal/pathogen/pathogen-test-with-resistance-gene.json, portal/pathogen/pathogen-test-bundle-with-resistance-gene.json"
  })
  void toBundle_shouldCreateBundle(String input, String expectedOutput) throws Exception {
    try (final var utils = Mockito.mockStatic(Utils.class)) {
      mockNblUtils(utils);
      testBundleCreation(this.pathogenBundleCreationService, input, expectedOutput);
    }
  }

  @ParameterizedTest
  @CsvSource({
    "portal/pathogen/pathogen-otherFacility.json, portal/pathogen/pathogen-otherFacility-bundle.json",
    "portal/pathogen/pathogen-otherAddress.json, portal/pathogen/pathogen-otherAddress-bundle.json",
    "portal/pathogen/pathogen-submittingFacility.json, portal/pathogen/pathogen-submittingFacility-bundle.json",
    "portal/pathogen/pathogen-currentResidence.json, portal/pathogen/pathogen-currentResidence-bundle.json",
    "portal/pathogen/pathogen-allPossibleAddresses.json, portal/pathogen/pathogen-allPossibleAddresses-bundle.json",
  })
  void toBundle_shouldCreateBundleWithNewAddressData(String input, String expectedOutput)
      throws Exception {
    try (final var utils = Mockito.mockStatic(Utils.class)) {
      mockNblUtils(utils);
      testBundleCreation(withAddressFeature, input, expectedOutput);
    }
  }

  @ParameterizedTest
  @CsvSource({
    "portal/pathogen/pathogen-test-multiple-specimen.json, portal/pathogen/pathogen-test-multiple-specimen-bundle.json",
    "portal/pathogen/pathogen-test-multiple-specimen-prefix.json, portal/pathogen/pathogen-test-multiple-specimen-prefix-bundle.json",
    "portal/pathogen/pathogen-test-multiple-specimen-salutation.json, portal/pathogen/pathogen-test-multiple-specimen-salutation-bundle.json",
    "portal/pathogen/pathogen-test-multiple-specimen-salutation-prefix.json, portal/pathogen/pathogen-test-multiple-specimen-salutation-prefix-bundle.json",
    "portal/pathogen/pathogen-test-multiple-specimen-otherFacility.json, portal/pathogen/pathogen-test-multiple-specimen-otherFacility-bundle.json",
    "portal/pathogen/pathogen-test-multiple-specimen-otherAddress.json, portal/pathogen/pathogen-test-multiple-specimen-otherAddress-bundle.json",
    "portal/pathogen/pathogen-test-multiple-specimen-submittingFacility.json, portal/pathogen/pathogen-test-multiple-specimen-submittingFacility-bundle.json",
    "portal/pathogen/pathogen-test-multiple-specimen-currentResidence.json, portal/pathogen/pathogen-test-multiple-specimen-currentResidence-bundle.json",
    "portal/pathogen/pathogen-test-multiple-specimen-allPossibleAddresses.json, portal/pathogen/pathogen-test-multiple-specimen-allPossibleAddresses-bundle.json"
  })
  void toBundle_shouldCreateBundleWithMultipleSpecimenData(String input, String expectedOutput)
      throws Exception {
    try (final var utils = Mockito.mockStatic(Utils.class)) {
      mockNblUtils(utils);
      testBundleCreation(withMultipleSpecimenFeature, input, expectedOutput);
    }
  }

  /**
   * Creates predictable, increasing resource IDs that will be matched against a static document.
   * The test fails if the order of the FHIR bundle entries changes. The generated resource IDs
   * start at 555-42-23-1.
   *
   * @param utils the mocked static utils
   */
  private void mockNblUtils(MockedStatic<Utils> utils) {
    final Date date =
        Date.from(Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse("2024-03-14T14:45:00+01:00")));
    utils.when(Utils::getCurrentDate).thenReturn(date);
    utils.when(Utils::generateUuidString).thenAnswer(i -> "555-42-23-" + ++counter);
  }

  private void testBundleCreation(
      PathogenBundleCreationService instance, String input, String expectedOutput)
      throws Exception {
    PathogenTest pathogenTest = FileUtils.unmarshal(input, PathogenTest.class);
    Bundle bundle = instance.toBundle(pathogenTest);
    String actualJson =
        FhirContext.forR4Cached()
            .newJsonParser()
            .setPrettyPrint(true)
            .encodeResourceToString(bundle);
    String expectedJson = FileUtils.loadJsonFromFile(expectedOutput);
    FileUtils.assertEqualJson(expectedJson, actualJson, "disease notification FHIR bundle");
  }
}
