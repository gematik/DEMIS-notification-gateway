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

import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.disease.questionnaire.CommonInformationDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.disease.questionnaire.SpecificInformationDataBuilder;
import de.gematik.demis.notificationgateway.common.dto.QuestionnaireResponse;
import de.gematik.demis.notificationgateway.domain.disease.fhir.DiseaseNotificationContext;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseStatus;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class QuestionnaireResponses {

  private final Items items;

  /**
   * Create common information questionnaire response
   *
   * @param context context
   * @param questionnaireResponse questionnaire response with common information
   */
  public void addCommon(
      DiseaseNotificationContext context, QuestionnaireResponse questionnaireResponse) {
    CommonInformationDataBuilder common = new CommonInformationDataBuilder();
    common.setDefaults();
    common.setNotifiedPerson(context.notifiedPerson());
    common.setStandardStatus(QuestionnaireResponseStatus.COMPLETED);
    createItems(context, questionnaireResponse, common::addItem);
    context.bundleBuilder().setCommonInformation(common.build());
  }

  /**
   * Create disease specific information questionnaire response
   *
   * @param context context
   * @param questionnaireResponse questionnaire response with disease specific information
   */
  public void addSpecific(
      DiseaseNotificationContext context, QuestionnaireResponse questionnaireResponse) {
    SpecificInformationDataBuilder specific = new SpecificInformationDataBuilder();
    setSpecifics(context, specific);
    specific.setDefaults();
    specific.setNotifiedPerson(context.notifiedPerson());
    specific.setStandardStatus(QuestionnaireResponseStatus.COMPLETED);
    createItems(context, questionnaireResponse, specific::addItem);
    context.bundleBuilder().setSpecificInformation(specific.build());
  }

  private void setSpecifics(
      DiseaseNotificationContext context, SpecificInformationDataBuilder specific) {
    String category = context.category();
    specific.setProfileUrlByDisease(category);
    specific.setQuestionnaireUrlByDisease(category);
  }

  private void createItems(
      DiseaseNotificationContext context,
      QuestionnaireResponse questionnaireResponse,
      Consumer<org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent>
          items) {
    questionnaireResponse.getItem().stream()
        .flatMap(item -> this.items.createFhirItems(context, item).stream())
        .forEach(items);
  }
}
