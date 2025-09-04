package de.gematik.demis.notificationgateway.domain.disease.fhir;

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

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import de.gematik.demis.notification.builder.demis.fhir.notification.utils.Utils;
import de.gematik.demis.notificationgateway.FeatureFlags;
import de.gematik.demis.notificationgateway.common.dto.DiseaseNotification;
import de.gematik.demis.notificationgateway.common.exceptions.BadRequestException;
import de.gematik.demis.notificationgateway.common.services.fhir.FhirObjectCreationService;
import de.gematik.demis.notificationgateway.domain.disease.fhir.questionnaire.Hospitalizations;
import de.gematik.demis.notificationgateway.domain.disease.fhir.questionnaire.Immunizations;
import de.gematik.demis.notificationgateway.domain.disease.fhir.questionnaire.Items;
import de.gematik.demis.notificationgateway.domain.disease.fhir.questionnaire.Organizations;
import de.gematik.demis.notificationgateway.domain.disease.fhir.questionnaire.QuestionnaireResponses;
import de.gematik.demis.notificationgateway.domain.disease.fhir.questionnaire.answer.Answers;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@ExtendWith(MockitoExtension.class)
@EnableConfigurationProperties(FeatureFlags.class)
@SpringBootTest(
    classes = {
      DiseaseNotificationBundleCreationService.class,
      FhirObjectCreationService.class,
      NotifiedPersonCreationService.class,
      OrganizationCreationService.class,
      PractitionerRoleCreationService.class,
      Diseases.class,
      QuestionnaireResponses.class,
      Items.class,
      Immunizations.class,
      Hospitalizations.class,
      Organizations.class,
      Answers.class
    })
@TestPropertySource(
    properties = {"feature.flag.notifications-73=true", "logging.level.de.gematik=DEBUG"})
class HospitalizationCopyCheckboxesTest {

  @Autowired private DiseaseNotificationBundleCreationService service;
  private DiseaseNotification input;
  private int counter;

  @ParameterizedTest
  @CsvSource({
    "portal/disease/hospitalization/hospitalization-at-notifier-and-contacts-copied-input.json, portal/disease/hospitalization/hospitalization-at-notifier-and-contacts-copied-output.json",
    "portal/disease/hospitalization/hospitalization-at-notifier-and-contacts-manual-input.json, portal/disease/hospitalization/hospitalization-at-notifier-and-contacts-manual-output.json",
    "portal/disease/hospitalization/hospitalization-at-other-and-contacts-copied-input.json, portal/disease/hospitalization/hospitalization-at-other-and-contacts-copied-output.json",
    "portal/disease/hospitalization/hospitalization-at-other-and-contacts-manual-input.json, portal/disease/hospitalization/hospitalization-at-other-and-contacts-manual-output.json",
  })
  void createBundle_shouldCreateBundle(String inputFile, String outputFile)
      throws BadRequestException {
    this.input = FileUtils.createDiseaseNotification(inputFile);
    try (final var utils = Mockito.mockStatic(Utils.class)) {
      mockNblUtils(utils);
      testBundleCreation(outputFile);
    }
  }

  /**
   * Creates predictable, increasing resource IDs that will be matched against a static document.
   * The test fails if the order of the FHIR bundleBuilder entries changes. The generated resource
   * IDs start at 555-42-23-1.
   *
   * @param utils the mocked static utils
   */
  private void mockNblUtils(MockedStatic<Utils> utils) {
    final Date date =
        Date.from(Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse("2024-03-14T14:45:00+01:00")));
    utils.when(Utils::getCurrentDate).thenReturn(date);
    utils.when(Utils::generateUuidString).thenAnswer(i -> "555-42-23-" + ++counter);
  }

  private void testBundleCreation(String outputFile) throws BadRequestException {
    final Bundle bundle = this.service.createBundle(this.input);
    assertThat(bundle).isNotNull();
    verify(outputFile, bundle);
  }

  private void verify(String outputFile, Bundle bundle) {
    String actualJson =
        FhirContext.forR4Cached()
            .newJsonParser()
            .setPrettyPrint(true)
            .encodeResourceToString(bundle);
    String expectedJson = FileUtils.loadJsonFromFile(outputFile);
    FileUtils.assertEqualJson(expectedJson, actualJson, "disease notification FHIR bundleBuilder");
  }
}
