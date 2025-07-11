package de.gematik.demis.notificationgateway.domain.disease.fhir.questionnaire;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.disease.NotificationBundleDiseaseDataBuilder;
import de.gematik.demis.notificationgateway.common.dto.QuestionnaireResponseItem;
import de.gematik.demis.notificationgateway.domain.disease.fhir.DiseaseNotificationContext;
import de.gematik.demis.notificationgateway.domain.disease.fhir.questionnaire.answer.Answers;
import de.gematik.demis.notificationgateway.domain.disease.fhir.questionnaire.answer.AnswersFactory;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HospitalizationsTest {

  private static final ObjectMapper JSON = new ObjectMapper();

  @Mock private Organizations organizations;
  @Mock private FhirResource organization;
  @Mock private DiseaseNotificationContext context;
  private Hospitalizations hospitalizations;

  private static QuestionnaireResponseItem readFile(String filename) throws IOException {
    File file = new File("src/test/resources/portal/disease/hospitalization/" + filename);
    return JSON.readValue(file, QuestionnaireResponseItem.class);
  }

  @BeforeEach
  void setUp() {
    Answers answers = new AnswersFactory().get();
    hospitalizations = new Hospitalizations(answers, organizations);
    when(context.bundleBuilder()).thenReturn(new NotificationBundleDiseaseDataBuilder());
  }

  @Test
  void givenReasonedHospitalizationWhenCreateFhirResourceThenEncounterWithReason()
      throws IOException {
    // given
    when(organizations.createFhirResource((DiseaseNotificationContext) any(), any()))
        .thenReturn(organization);
    // when
    final Encounter encounter = createEncounter("inbound-hospitalization-with-reason.json");
    // then
    assertThat(encounter).isNotNull();
    verifyReason(encounter);
  }

  private void verifyReason(Encounter encounter) {
    List<CodeableConcept> codeableConcepts = encounter.getReasonCode();
    assertThat(codeableConcepts).isNotNull().hasSize(1);
    List<Coding> codings = codeableConcepts.getFirst().getCoding();
    assertThat(codings).isNotNull().hasSize(1);
    Coding coding = codings.getFirst();
    assertThat(coding.getSystem())
        .isEqualTo("https://demis.rki.de/fhir/CodeSystem/hospitalizationReason");
    assertThat(coding.getCode()).isEqualTo("becauseOfDisease");
  }

  private Encounter createEncounter(String file) throws IOException {
    return (Encounter) hospitalizations.createFhirResource(context, readFile(file)).resource();
  }
}
