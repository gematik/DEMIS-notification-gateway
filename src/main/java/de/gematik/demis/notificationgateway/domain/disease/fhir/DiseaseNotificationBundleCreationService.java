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

import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.disease.NotificationBundleDiseaseDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.disease.NotificationBundleDiseaseNonNominalDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.disease.NotificationDiseaseDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.RelatesToBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.utils.DemisConstants;
import de.gematik.demis.notificationgateway.FeatureFlags;
import de.gematik.demis.notificationgateway.common.dto.DiseaseNotification;
import de.gematik.demis.notificationgateway.common.dto.DiseaseStatus;
import de.gematik.demis.notificationgateway.common.dto.NotifierFacility;
import de.gematik.demis.notificationgateway.common.dto.QuestionnaireResponse;
import de.gematik.demis.notificationgateway.common.enums.NotificationType;
import de.gematik.demis.notificationgateway.common.exceptions.BadRequestException;
import de.gematik.demis.notificationgateway.common.terminology.TerminologyCurator;
import de.gematik.demis.notificationgateway.domain.disease.fhir.questionnaire.QuestionnaireResponses;
import jakarta.annotation.Nullable;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class DiseaseNotificationBundleCreationService {

  private final NotifiedPersonCreationService notifiedPersonCreationService;
  private final OrganizationCreationService organizationCreationService;
  private final PractitionerRoleCreationService practitionerRoleCreationService;
  private final Diseases diseases;
  private final QuestionnaireResponses questionnaireResponses;
  private final FeatureFlags featureFlags;

  /**
   * @deprecated should be removed with feature.flag.notifications.7_3
   * @param diseaseNotification
   * @return
   * @throws BadRequestException
   */
  @Deprecated
  public Bundle createBundle(DiseaseNotification diseaseNotification) throws BadRequestException {
    return createBundle(diseaseNotification, NotificationType.NOMINAL);
  }

  /**
   * Create FHIR bundleBuilder for disease notification
   *
   * @param diseaseNotification disease notification
   * @return FHIR bundleBuilder
   */
  public Bundle createBundle(
      DiseaseNotification diseaseNotification, NotificationType notificationType)
      throws BadRequestException {
    PractitionerRole notifier = createNotifier(diseaseNotification);

    Patient patient;

    if (featureFlags.isFollowUpNotificationActive()
        || featureFlags.isPathogenStrictSnapshotActive()) {
      Object patientDataFromFE =
          diseaseNotification.getNotifiedPerson() != null
              ? diseaseNotification.getNotifiedPerson()
              : diseaseNotification.getNotifiedPersonAnonymous();
      patient = this.notifiedPersonCreationService.createPatient(patientDataFromFE, notifier);
    } else {
      patient =
          this.notifiedPersonCreationService.createPatientLegacy(
              diseaseNotification.getNotifiedPerson(), notifier);
    }
    final DiseaseNotificationContext context =
        createContext(diseaseNotification, notifier, patient, notificationType);
    createDisease(context);
    createCommonQuestionnaireResponse(diseaseNotification, context);
    createDiseaseQuestionnaireResponse(diseaseNotification, context);
    createComposition(context);
    final Bundle bundle = context.bundleBuilder().build();
    setTerminologyVersions(diseaseNotification, bundle);
    return bundle;
  }

  private void setTerminologyVersions(DiseaseNotification diseaseNotification, Bundle bundle) {
    if (featureFlags.isDiseaseStrictProfile()) {
      new TerminologyCurator(diseaseNotification.getTerminologyVersions())
          .setCodeSystemVersions(bundle);
    }
  }

  private PractitionerRole createNotifier(DiseaseNotification diseaseNotification)
      throws BadRequestException {
    final Organization notifierFacility =
        createNotifierFacility(diseaseNotification.getNotifierFacility());
    return practitionerRoleCreationService.createNotifierRole(notifierFacility);
  }

  private DiseaseNotificationContext createContext(
      DiseaseNotification diseaseNotification,
      PractitionerRole notifierRole,
      Patient notifiedPerson,
      NotificationType notificationType)
      throws BadRequestException {
    NotificationBundleDiseaseDataBuilder bundleBuilder;
    switch (notificationType) {
      case NOMINAL -> bundleBuilder = new NotificationBundleDiseaseDataBuilder();
      case NON_NOMINAL, ANONYMOUS ->
          bundleBuilder = new NotificationBundleDiseaseNonNominalDataBuilder();
      default -> throw new BadRequestException("Unsupported notification type");
    }
    bundleBuilder.setDefaults();
    bundleBuilder.setNotifierRole(notifierRole);
    setNotifiedPerson(notifiedPerson, bundleBuilder);
    return new DiseaseNotificationContext(diseaseNotification, bundleBuilder, notifiedPerson);
  }

  private void setNotifiedPerson(
      Patient notifiedPerson, NotificationBundleDiseaseDataBuilder bundle) {
    bundle.setNotifiedPerson(notifiedPerson);
    if (notifiedPerson.hasAddress()) {
      notifiedPerson.getAddress().stream()
          .map(this::getNotifiedPersonFacilityExtension)
          .filter(Objects::nonNull)
          .map(Extension::getValue)
          .map(Reference.class::cast)
          .map(Reference::getResource)
          .map(Organization.class::cast)
          .forEach(bundle::addNotifiedPersonFacilities);
    }
  }

  @Nullable
  private Extension getNotifiedPersonFacilityExtension(Address address) {
    return address.getExtensionByUrl(
        DemisConstants.STRUCTURE_DEFINITION_FACILITY_ADDRESS_NOTIFIED_PERSON);
  }

  private Organization createNotifierFacility(NotifierFacility notifierFacilityContent)
      throws BadRequestException {
    final String orgaType = notifierFacilityContent.getFacilityInfo().getOrganizationType();
    if (StringUtils.isBlank(orgaType)) {
      throw new BadRequestException("notifierFacility.facilityInfo.organizationType is required");
    }
    return organizationCreationService.createNotifierFacility(notifierFacilityContent);
  }

  private void createDisease(DiseaseNotificationContext context) {
    this.diseases.addDisease(context);
  }

  private void createCommonQuestionnaireResponse(
      DiseaseNotification diseaseNotification, DiseaseNotificationContext context) {
    var common = diseaseNotification.getCommon();
    if ((common != null) && !common.getItem().isEmpty()) {
      this.questionnaireResponses.addCommon(context, diseaseNotification.getCommon());
    }
  }

  private void createDiseaseQuestionnaireResponse(
      DiseaseNotification diseaseNotification, DiseaseNotificationContext context) {
    QuestionnaireResponse disease = diseaseNotification.getDisease();
    if ((disease != null) && !disease.getItem().isEmpty()) {
      this.questionnaireResponses.addSpecific(context, disease);
    }
  }

  private void createComposition(DiseaseNotificationContext context) {
    NotificationBundleDiseaseDataBuilder bundle = context.bundleBuilder();
    NotificationDiseaseDataBuilder composition = bundle.createComposition();
    DiseaseStatus status = context.notification().getStatus();
    Composition.CompositionStatus compositionStatus =
        switch (status.getStatus()) {
          case FINAL, REFUTED -> Composition.CompositionStatus.FINAL;
          case PRELIMINARY -> Composition.CompositionStatus.PRELIMINARY;
          case AMENDED, ERROR -> Composition.CompositionStatus.AMENDED;
        };
    composition.setStatus(compositionStatus);
    String initialNotificationId = status.getInitialNotificationId();
    if (StringUtils.isNotBlank(initialNotificationId)) {
      composition.setRelatesTo(RelatesToBuilder.forInitialNotificationId(initialNotificationId));
    }
    bundle.setNotificationDisease(composition.build());
  }
}
