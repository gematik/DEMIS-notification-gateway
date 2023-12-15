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

package de.gematik.demis.notificationgateway.utils;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.demis.notificationgateway.common.dto.BedOccupancy;
import de.gematik.demis.notificationgateway.common.dto.Hospitalization;
import de.gematik.demis.notificationgateway.common.dto.PathogenData;
import de.gematik.demis.notificationgateway.common.dto.QuickTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;

@Slf4j
public class FileUtils {

  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
  private static final String TEST_RESOURCE_PATH = "src/test/resources";

  public static QuickTest createQuickTest(String filePath) throws JsonProcessingException {
    final String jsonString = FileUtils.loadJsonFromFile(filePath);

    return OBJECT_MAPPER.readValue(jsonString, QuickTest.class);
  }

  public static Hospitalization createHospitalization(String filePath)
      throws JsonProcessingException {
    final String jsonString = FileUtils.loadJsonFromFile(filePath);

    return OBJECT_MAPPER.readValue(jsonString, Hospitalization.class);
  }

  public static BedOccupancy createBedOccupancy(String filePath) throws JsonProcessingException {
    final String jsonString = FileUtils.loadJsonFromFile(filePath);

    return OBJECT_MAPPER.readValue(jsonString, BedOccupancy.class);
  }

  public static Bundle createBundleFromFile(String filePath) {
    final String bundleJsonString = FileUtils.loadJsonFromFile(filePath);
    return (Bundle) FhirContext.forR4().newJsonParser().parseResource(bundleJsonString);
  }

  public static Parameters createParametersFromFile(String filePath) {
    final String parametersJsonString = FileUtils.loadJsonFromFile(filePath);
    return (Parameters) FhirContext.forR4().newJsonParser().parseResource(parametersJsonString);
  }

  public static OperationOutcome createOperationOutcomeFromFile(String filePath) {
    final String operationOutcomeJsonString = FileUtils.loadJsonFromFile(filePath);
    return (OperationOutcome)
        FhirContext.forR4().newJsonParser().parseResource(operationOutcomeJsonString);
  }

  public static PathogenData createPathogenData(String filePath) throws JsonProcessingException {
    final String jsonString = FileUtils.loadJsonFromFile(filePath);

    return OBJECT_MAPPER.readValue(jsonString, PathogenData.class);
  }

  /**
   * Load the given file and return content. The file to load should be places in path resources
   *
   * @param filePath Path to the json file for load
   * @return string of json or null if file path is null or file not found
   */
  public static String loadJsonFromFile(String filePath) {
    Objects.requireNonNull(filePath, "require nonNull file path");

    try {
      filePath = checkLeadingBackslash(filePath);
      filePath = TEST_RESOURCE_PATH + filePath;

      return Files.readString(Paths.get(filePath));

    } catch (IOException | IllegalArgumentException e) {
      log.error("Unable to read file input", e);
    }
    return null;
  }

  private static String checkLeadingBackslash(String path) {
    if (path != null && (!path.startsWith("\\") && !path.startsWith("/"))) {
      return "/" + path;
    }
    return path;
  }
}
