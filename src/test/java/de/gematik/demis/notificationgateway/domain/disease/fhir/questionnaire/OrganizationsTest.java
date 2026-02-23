package de.gematik.demis.notificationgateway.domain.disease.fhir.questionnaire;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.disease.NotificationBundleDiseaseDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.utils.DemisConstants;
import de.gematik.demis.notificationgateway.FeatureFlags;
import de.gematik.demis.notificationgateway.common.constants.FhirConstants;
import de.gematik.demis.notificationgateway.common.dto.QuestionnaireResponseAnswer;
import de.gematik.demis.notificationgateway.common.dto.QuestionnaireResponseItem;
import de.gematik.demis.notificationgateway.domain.disease.fhir.DiseaseNotificationContext;
import de.gematik.demis.notificationgateway.domain.disease.fhir.questionnaire.answer.Answers;
import de.gematik.demis.notificationgateway.domain.disease.fhir.questionnaire.answer.AnswersFactory;
import java.util.List;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationsTest {

  @Mock private FeatureFlags featureFlags;
  private Organizations organizations;

  private static QuestionnaireResponseItem createLaboratoryFacilityInput(String bsnr) {
    final QuestionnaireResponseItem bsnrItem = new QuestionnaireResponseItem("bsnr");
    bsnrItem.addAnswerItem(new QuestionnaireResponseAnswer().valueString(bsnr));

    final QuestionnaireResponseItem organizationItem =
        new QuestionnaireResponseItem("LaboratoryFacility");
    organizationItem.setItem(List.of(bsnrItem));

    final QuestionnaireResponseAnswer outerAnswer = new QuestionnaireResponseAnswer();
    outerAnswer.setItem(List.of(organizationItem));

    final QuestionnaireResponseItem input = new QuestionnaireResponseItem("organizationReference");
    input.setAnswer(List.of(outerAnswer));
    return input;
  }

  @BeforeEach
  void createOrganizations() {
    final Answers answers = new AnswersFactory(true).get();
    this.organizations = new Organizations(answers, this.featureFlags);
  }

  @Test
  void givenLaboratoryFacilityWhenCreatingOrganizationThenOrganizationHasIdentifierAsBsnr() {
    // given
    when(featureFlags.isDiseaseStrictProfile()).thenReturn(true);
    final String bsnr = "123456789";
    final QuestionnaireResponseItem input = createLaboratoryFacilityInput(bsnr);
    final DiseaseNotificationContext context =
        new DiseaseNotificationContext(null, new NotificationBundleDiseaseDataBuilder(), null);

    // when
    final FhirResource resource = organizations.createFhirResource(context, input);
    final Organization organization = resource.organization();

    // then
    assertThat(organization).isNotNull();
    assertThat(organization.hasIdentifier()).isTrue();
    final Identifier identifier =
        organization.getIdentifier().stream()
            .filter(id -> FhirConstants.NAMING_SYSTEM_BSNR.equals(id.getSystem()))
            .findFirst()
            .orElse(null);
    assertThat(identifier).isNotNull();
    assertThat(identifier.getValue()).isEqualTo(bsnr);
    assertThat(identifier.getSystem()).isEqualTo(DemisConstants.NAMING_SYSTEM_BSNR);
  }
}
