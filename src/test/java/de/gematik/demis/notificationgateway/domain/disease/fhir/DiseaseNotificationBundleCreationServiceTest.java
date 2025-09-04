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

import static de.gematik.demis.notificationgateway.utils.FileUtils.loadJsonFromFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import de.gematik.demis.notification.builder.demis.fhir.notification.utils.Compositions;
import de.gematik.demis.notification.builder.demis.fhir.notification.utils.DemisConstants;
import de.gematik.demis.notification.builder.demis.fhir.notification.utils.Utils;
import de.gematik.demis.notificationgateway.FeatureFlags;
import de.gematik.demis.notificationgateway.common.dto.DiseaseNotification;
import de.gematik.demis.notificationgateway.common.enums.NotificationType;
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
import java.util.Optional;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

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
class DiseaseNotificationBundleCreationServiceTest {

  @Autowired private DiseaseNotificationBundleCreationService service;
  private DiseaseNotification input;
  private int counter;

  @ParameterizedTest
  @CsvSource({
    "portal/disease/notification-formly-input.json, portal/disease/notification-formly-output.json",
    "portal/disease/notification-formly-rund-input.json, portal/disease/notification-formly-rund-output.json",
    "portal/disease/patient.address/current-address-is-primary-as-current-input.json, portal/disease/patient.address/current-address-is-primary-as-current-output.json",
    "portal/disease/patient.address/current-address-is-other-facility-input.json, portal/disease/patient.address/current-address-is-other-facility-output.json",
    "portal/disease/patient.address/current-address-is-submitting-facility-input.json, portal/disease/patient.address/current-address-is-submitting-facility-output.json"
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
    String expectedJson = loadJsonFromFile(outputFile);
    FileUtils.assertEqualJson(expectedJson, actualJson, "disease notification FHIR bundleBuilder");
  }

  @Test
  void createDiseaseNotifierFacilityOrgaTypeNoSet() {
    this.input =
        FileUtils.createDiseaseNotification("portal/disease/notification-formly-input.json");
    input.getNotifierFacility().getFacilityInfo().setOrganizationType(null);

    assertThrows(BadRequestException.class, () -> service.createBundle(input));
  }

  @Test
  void createDiseaseWithQuantities() throws BadRequestException {
    input =
        FileUtils.createDiseaseNotification("portal/disease/73.notifications/input/disease_1.json");
    try (final var utils = Mockito.mockStatic(Utils.class)) {
      mockNblUtils(utils);
      Bundle bundle = service.createBundle(input, NotificationType.NON_NOMINAL);

      assertThat(bundle).isNotNull();

      String expectedJson =
          loadJsonFromFile("portal/disease/73.notifications/output/fhir_disease_1.json");

      IParser iParser = FhirContext.forR4Cached().newJsonParser().setPrettyPrint(true);
      String actualJson = iParser.encodeResourceToString(bundle);

      assertThat(actualJson).isEqualToIgnoringWhitespace(expectedJson);
    }
  }

  /** Regression of DEMIS-3971 */
  @Test
  void thatInitialNotificationIdIsNotSetAsNotificationId() throws BadRequestException {
    // simply referenced in the input file under status
    final String initialNotificationId = "05240c75-29e4-4a9f-8965-7c79d7f015ed";
    input =
        FileUtils.createDiseaseNotification(
            "portal/disease/73.notifications/input/disease_with_initialNotificationId.json");
    try (final var utils = Mockito.mockStatic(Utils.class)) {
      mockNblUtils(utils);
      final Bundle bundle = service.createBundle(input, NotificationType.NOMINAL);

      assertThat(bundle).isNotNull();
      final Optional<Composition> from = Compositions.from(bundle);
      assertThat(from).isPresent();
      assertThat(from.get().getIdentifier().getValue()).isNotEqualTo(initialNotificationId);
      assertThat(from.get().getRelatesToFirstRep().getTarget())
          .isInstanceOfSatisfying(
              Reference.class,
              ref -> {
                assertThat(ref.getIdentifier().getValue()).isEqualTo(initialNotificationId);
                assertThat(ref.getIdentifier().getSystem())
                    .isEqualTo(DemisConstants.NOTIFICATION_ID_SYSTEM);
              });
    }
  }
}
