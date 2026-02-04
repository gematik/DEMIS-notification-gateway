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

import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.disease.EncounterDataBuilder;
import de.gematik.demis.notificationgateway.common.dto.QuestionnaireResponseAnswer;
import de.gematik.demis.notificationgateway.common.dto.QuestionnaireResponseItem;
import de.gematik.demis.notificationgateway.domain.disease.fhir.DiseaseNotificationContext;
import de.gematik.demis.notificationgateway.domain.disease.fhir.questionnaire.answer.Answers;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Encounter;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class Hospitalizations implements ResourceFactory {

  private static final String PERIOD_LINK_ID = "period";
  private static final String PERIOD_START_LINK_ID = "start";
  private static final String PERIOD_END_LINK_ID = "end";
  private static final String REASON_LINK_ID = "reason";

  private final Answers answers;
  private final Organizations organizations;

  @Override
  public FhirResource createFhirResource(
      DiseaseNotificationContext context, QuestionnaireResponseItem item) {
    QuestionnaireResponseItem hospitalizationItem =
        item.getAnswer().getFirst().getItem().getFirst();
    Encounter hospitalization = createEncounter(context, hospitalizationItem);
    context.bundleBuilder().addHospitalization(hospitalization);
    return createFhirResource(hospitalization, item.getLinkId());
  }

  @Override
  public boolean test(QuestionnaireResponseItem item) {
    if ("hospitalizedEncounter".equals(item.getLinkId())) {
      List<QuestionnaireResponseAnswer> itemAnswers = item.getAnswer();
      if ((itemAnswers != null) && !itemAnswers.isEmpty()) {
        List<QuestionnaireResponseItem> answerSubitems = itemAnswers.getFirst().getItem();
        if ((answerSubitems != null) && !answerSubitems.isEmpty()) {
          return "Hospitalization".equals(answerSubitems.getFirst().getLinkId());
        }
      }
    }
    return false;
  }

  private void setNotifiedPerson(
      DiseaseNotificationContext context, EncounterDataBuilder hospitalization) {
    hospitalization.setNotifiedPerson(context.notifiedPerson());
  }

  private Encounter createEncounter(
      DiseaseNotificationContext context, QuestionnaireResponseItem item) {
    final var hospitalization = new EncounterDataBuilder();
    hospitalization.setDefaults();
    setNotifiedPerson(context, hospitalization);
    setServiceType(item, hospitalization);
    setPeriod(item, hospitalization);
    setReason(item, hospitalization);
    setStatus(item, hospitalization);
    setServiceProvider(context, item, hospitalization);
    return hospitalization.build();
  }

  /**
   * The status is currently not defined from the user. We are missing the according value set and
   * code system in the RKI FHIR profile. The status is a mandatory field in the FHIR profile.
   *
   * @param item the item
   * @param hospitalization hospitalization builder
   */
  private void setStatus(QuestionnaireResponseItem item, EncounterDataBuilder hospitalization) {
    Encounter.EncounterStatus status = Encounter.EncounterStatus.INPROGRESS;
    Optional<QuestionnaireResponseItem> period = findSubItem(item, PERIOD_LINK_ID);
    if (period.isPresent()
        && findSubItemAnswer(period.get(), PERIOD_END_LINK_ID)
            .filter(this.answers::containsValue)
            .isPresent()) {
      status = Encounter.EncounterStatus.FINISHED;
    }
    hospitalization.setStatus(status.toCode());
  }

  private void setServiceType(
      QuestionnaireResponseItem item, EncounterDataBuilder hospitalization) {
    findSubItemAnswer(item, "serviceType")
        .ifPresent(
            answer ->
                hospitalization.setServiceType(
                    this.answers.createFhirAnswer(answer).value().toCoding()));
  }

  private void setPeriod(QuestionnaireResponseItem item, EncounterDataBuilder hospitalization) {
    findSubItem(item, PERIOD_LINK_ID).ifPresent(period -> setPeriodItem(period, hospitalization));
  }

  private void setPeriodItem(QuestionnaireResponseItem item, EncounterDataBuilder hospitalization) {
    findSubItemAnswer(item, PERIOD_START_LINK_ID)
        .filter(this.answers::containsValue)
        .ifPresent(
            answer ->
                hospitalization.setPeriodStart(
                    this.answers.createFhirAnswer(answer).value().toDateTimeType()));
    findSubItemAnswer(item, PERIOD_END_LINK_ID)
        .filter(this.answers::containsValue)
        .ifPresent(
            answer ->
                hospitalization.setPeriodEnd(
                    this.answers.createFhirAnswer(answer).value().toDateTimeType()));
  }

  private void setReason(QuestionnaireResponseItem item, EncounterDataBuilder hospitalization) {
    findSubItemAnswer(item, REASON_LINK_ID)
        .ifPresent(
            reason ->
                hospitalization.setReason(
                    this.answers.createFhirAnswer(reason).value().toCoding()));
  }

  private void setServiceProvider(
      DiseaseNotificationContext context,
      QuestionnaireResponseItem item,
      EncounterDataBuilder hospitalization) {
    findSubItem(item, "serviceProvider")
        .ifPresent(
            serviceProvider ->
                setServiceProviderOrganization(context, serviceProvider, hospitalization));
  }

  private void setServiceProviderOrganization(
      DiseaseNotificationContext context,
      QuestionnaireResponseItem serviceProvider,
      EncounterDataBuilder hospitalization) {
    FhirResource resource = this.organizations.createFhirResource(context, serviceProvider);
    hospitalization.setServiceProvider(resource.organization());
  }
}
