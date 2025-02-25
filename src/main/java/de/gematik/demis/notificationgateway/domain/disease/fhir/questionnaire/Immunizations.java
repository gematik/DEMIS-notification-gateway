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
 * #L%
 */

import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.disease.ImmunizationDataBuilder;
import de.gematik.demis.notificationgateway.common.dto.QuestionnaireResponseAnswer;
import de.gematik.demis.notificationgateway.common.dto.QuestionnaireResponseItem;
import de.gematik.demis.notificationgateway.domain.disease.fhir.DiseaseNotificationContext;
import de.gematik.demis.notificationgateway.domain.disease.fhir.questionnaire.answer.Answers;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Immunization;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class Immunizations implements ResourceFactory {

  private final Answers answers;

  @Override
  public FhirResource createFhirResource(
      DiseaseNotificationContext context, QuestionnaireResponseItem item) {
    QuestionnaireResponseItem immunizationItem = item.getAnswer().getFirst().getItem().getFirst();
    Immunization immunization = createImmunization(context, immunizationItem);
    context.bundle().addImmunization(immunization);
    return createFhirResource(immunization, item.getLinkId());
  }

  @Override
  public boolean test(QuestionnaireResponseItem item) {
    String linkId = item.getLinkId();
    return StringUtils.equals(linkId, "immunizationRef")
        || StringUtils.equals(linkId, "immunizationMotherRef");
  }

  private Immunization createImmunization(
      DiseaseNotificationContext context, QuestionnaireResponseItem item) {
    final var immunization = new ImmunizationDataBuilder();
    immunization.setDefaults();
    setProfileUrl(context, immunization);
    setNotifiedPerson(context, immunization);
    setVaccineCode(item, immunization);
    setOccurrence(item, immunization);
    setNote(item, immunization);
    return immunization.build();
  }

  private void setProfileUrl(
      DiseaseNotificationContext context, ImmunizationDataBuilder immunization) {
    immunization.setProfileUrlByDisease(context.category());
  }

  private void setNotifiedPerson(
      DiseaseNotificationContext context, ImmunizationDataBuilder immunization) {
    immunization.setNotifiedPerson(context.notifiedPerson());
  }

  private void setVaccineCode(
      QuestionnaireResponseItem item, ImmunizationDataBuilder immunization) {
    var vaccineCode = getSubItemAnswer(item, "vaccineCode");
    immunization.setVaccineCode(this.answers.createFhirAnswer(vaccineCode).value().toCoding());
  }

  private void setOccurrence(QuestionnaireResponseItem item, ImmunizationDataBuilder immunization) {
    var occurrence = getSubItemAnswer(item, "occurrence");
    if (answers.containsValue(occurrence)) {
      immunization.setOccurrence(
          this.answers.createFhirAnswer(occurrence).value().toDateTimeType());
    }
  }

  private void setNote(QuestionnaireResponseItem item, ImmunizationDataBuilder immunization) {
    findSubItemAnswer(item, "note")
        .map(QuestionnaireResponseAnswer::getValueString)
        .map(StringUtils::trimToNull)
        .filter(Objects::nonNull)
        .ifPresent(immunization::addNote);
  }
}
