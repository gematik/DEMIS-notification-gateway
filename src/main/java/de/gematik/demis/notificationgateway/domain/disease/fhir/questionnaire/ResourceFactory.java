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

import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.disease.questionnaire.ItemDataBuilder;
import de.gematik.demis.notificationgateway.common.dto.QuestionnaireResponseAnswer;
import de.gematik.demis.notificationgateway.common.dto.QuestionnaireResponseItem;
import de.gematik.demis.notificationgateway.domain.disease.fhir.DiseaseNotificationContext;
import java.util.Optional;
import java.util.function.Predicate;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;

interface ResourceFactory extends Predicate<QuestionnaireResponseItem> {
  /**
   * Create FHIR resource from item data. Adds resource to the bundle and returns an item containing
   * a <code>valueReference</code>.
   *
   * @param context context
   * @param item item with resource data
   * @return resource reference, FHIR item with <code>valueReference</code>
   */
  FhirResource createFhirResource(
      DiseaseNotificationContext context, QuestionnaireResponseItem item);

  /**
   * Test the given item if it contains a resource
   *
   * @param item item to test
   * @return <code>true</code> if the item contains a resource, <code>false</code> if not
   */
  @Override
  boolean test(QuestionnaireResponseItem item);

  default QuestionnaireResponseItem getSubItem(QuestionnaireResponseItem item, String linkId) {
    return findSubItem(item, linkId)
        .orElseThrow(() -> new IllegalArgumentException("Item not found: " + linkId));
  }

  default Optional<QuestionnaireResponseItem> findSubItem(
      QuestionnaireResponseItem item, String linkId) {
    return item.getItem().stream().filter(i -> i.getLinkId().contains(linkId)).findFirst();
  }

  default QuestionnaireResponseAnswer getSubItemAnswer(
      QuestionnaireResponseItem item, String linkId) {
    return getSubItem(item, linkId).getAnswer().getFirst();
  }

  default Optional<QuestionnaireResponseAnswer> findSubItemAnswer(
      QuestionnaireResponseItem item, String linkId) {
    return findSubItem(item, linkId).map(i -> i.getAnswer().getFirst());
  }

  default FhirResource createFhirResource(Resource resource, String linkId) {
    ItemDataBuilder item = new ItemDataBuilder();
    item.setLinkId(linkId);
    item.addAnswer(
        new QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent()
            .setValue(new Reference(resource)));
    return new FhirResource(resource, item.build());
  }
}
