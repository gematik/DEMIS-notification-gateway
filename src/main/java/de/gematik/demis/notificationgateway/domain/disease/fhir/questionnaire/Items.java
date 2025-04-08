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
import de.gematik.demis.notificationgateway.domain.disease.fhir.questionnaire.answer.Answers;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class Items {

  private static final String LINK_ID_REPEAT_SECTION = "repeat-section";

  private final List<ResourceFactory> resources;
  private final Answers answers;

  List<QuestionnaireResponse.QuestionnaireResponseItemComponent> createFhirItems(
      DiseaseNotificationContext context, QuestionnaireResponseItem item) {
    if (isEmpty(item)) {
      return Collections.emptyList();
    }
    List<QuestionnaireResponse.QuestionnaireResponseItemComponent> items = new LinkedList<>();
    if (StringUtils.startsWith(item.getLinkId(), LINK_ID_REPEAT_SECTION)) {
      item.getItem().stream()
          .map(i -> createFhirItems(context, i))
          .flatMap(List::stream)
          .forEach(items::add);
    } else {
      final var resource = createResourceReferenceItem(context, item);
      if (resource != null) {
        items.add(resource);
      } else {
        items.add(createItem(context, item));
      }
    }
    return items;
  }

  private boolean isEmpty(QuestionnaireResponseItem item) {
    List<QuestionnaireResponseItem> subitems = item.getItem();
    if ((subitems != null) && !subitems.isEmpty()) {
      return false;
    }
    List<QuestionnaireResponseAnswer> itemAnswers = item.getAnswer();
    if ((itemAnswers != null) && !itemAnswers.isEmpty()) {
      return itemAnswers.stream().allMatch(this::isEmpty);
    }
    return true;
  }

  private boolean isNotEmpty(QuestionnaireResponseItem item) {
    return !isEmpty(item);
  }

  private boolean isEmpty(QuestionnaireResponseAnswer answer) {
    List<QuestionnaireResponseItem> subitems = answer.getItem();
    if ((subitems != null) && !subitems.isEmpty()) {
      return false;
    }
    return !this.answers.containsValue(answer);
  }

  private boolean isNotEmpty(QuestionnaireResponseAnswer answer) {
    return !isEmpty(answer);
  }

  private QuestionnaireResponse.QuestionnaireResponseItemComponent createItem(
      DiseaseNotificationContext context, QuestionnaireResponseItem item) {
    final ItemDataBuilder fhir = new ItemDataBuilder();
    fhir.setLinkId(item.getLinkId());
    createAnswers(context, item, fhir);
    createSubItems(context, item, fhir);
    return fhir.build();
  }

  private void createAnswers(
      DiseaseNotificationContext context, QuestionnaireResponseItem item, ItemDataBuilder fhir) {
    List<QuestionnaireResponseAnswer> itemAnswers = item.getAnswer();
    if (itemAnswers != null) {
      itemAnswers.stream()
          .filter(this::isNotEmpty)
          .forEach(answer -> createAnswer(context, answer, fhir));
    }
  }

  private void createAnswer(
      DiseaseNotificationContext context,
      QuestionnaireResponseAnswer answer,
      ItemDataBuilder fhir) {
    final var fhirAnswer = this.answers.createFhirAnswer(answer).object();
    fhir.addAnswer(fhirAnswer);
    final List<QuestionnaireResponseItem> subItems = answer.getItem();
    if (subItems != null) {
      subItems.stream()
          .filter(this::isNotEmpty)
          .flatMap(subItem -> createFhirItems(context, subItem).stream())
          .forEach(fhirAnswer::addItem);
    }
  }

  private void createSubItems(
      DiseaseNotificationContext context, QuestionnaireResponseItem item, ItemDataBuilder fhir) {
    List<QuestionnaireResponseItem> subItems = item.getItem();
    if (subItems != null) {
      subItems.stream()
          .filter(this::isNotEmpty)
          .flatMap(subItem -> createFhirItems(context, subItem).stream())
          .forEach(fhir::addItem);
    }
  }

  private QuestionnaireResponse.QuestionnaireResponseItemComponent createResourceReferenceItem(
      DiseaseNotificationContext context, QuestionnaireResponseItem item) {
    return this.resources.stream()
        .filter(service -> service.test(item))
        .map(service -> service.createFhirResource(context, item).referenceItem())
        .findFirst()
        .orElse(null);
  }
}
