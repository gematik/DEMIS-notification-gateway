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
 * #L%
 */

import static de.gematik.demis.notificationgateway.common.dto.AddressType.OTHER_FACILITY;

import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.disease.NotificationBundleDiseaseDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.disease.NotificationDiseaseDataBuilder;
import de.gematik.demis.notificationgateway.common.dto.DiseaseNotification;
import de.gematik.demis.notificationgateway.common.dto.DiseaseStatus;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPersonAddressInfo;
import de.gematik.demis.notificationgateway.common.dto.NotifierFacility;
import de.gematik.demis.notificationgateway.common.dto.QuestionnaireResponse;
import de.gematik.demis.notificationgateway.common.exceptions.BadRequestException;
import de.gematik.demis.notificationgateway.domain.disease.fhir.questionnaire.QuestionnaireResponses;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class DiseaseNotificationBundleCreationService {

  @Value("${feature.flag.disease_address_patient:false}")
  private boolean isNewAddressFeatureEnabled;

  private final NotifiedPersonCreationService notifiedPersonCreationService;
  private final OrganizationCreationService organizationCreationService;
  private final PractitionerRoleCreationService practitionerRoleCreationService;
  private final Diseases diseases;
  private final QuestionnaireResponses questionnaireResponses;

  /**
   * Create FHIR bundle for disease notification
   *
   * @param diseaseNotification disease notification
   * @return FHIR bundle
   */
  public Bundle createBundle(DiseaseNotification diseaseNotification) throws BadRequestException {
    PractitionerRole notifier = createNotifier(diseaseNotification);
    Optional<Organization> otherFacility = createOtherFacility(diseaseNotification);
    Patient patient = createPatient(diseaseNotification, notifier, otherFacility);

    DiseaseNotificationContext context =
        createContext(diseaseNotification, notifier, patient, otherFacility);
    createDisease(context);
    createCommonQuestionnaireResponse(diseaseNotification, context);
    createDiseaseQuestionnaireResponse(diseaseNotification, context);
    createComposition(context);
    return context.bundle().build();
  }

  private PractitionerRole createNotifier(DiseaseNotification diseaseNotification)
      throws BadRequestException {
    final Organization notifierFacility =
        createNotifierFacility(diseaseNotification.getNotifierFacility());
    return practitionerRoleCreationService.createNotifierRole(notifierFacility);
  }

  private Optional<Organization> createOtherFacility(DiseaseNotification diseaseNotification) {
    final NotifiedPersonAddressInfo notifiedPersonAddressInfo =
        diseaseNotification.getNotifiedPerson().getCurrentAddress();
    if (notifiedPersonAddressInfo != null
        && notifiedPersonAddressInfo.getAddressType() == OTHER_FACILITY) {
      return Optional.of(
          organizationCreationService.createOtherFacility(notifiedPersonAddressInfo));
    }
    return Optional.empty();
  }

  /**
   * Creates a Patient based on DiseaseNotification, PractitionerRole, and optional Organization.
   *
   * @param diseaseNotification the disease notification containing the notified person information
   * @param notifierRole the practitioner role associated with the notifier
   * @param otherFacility an optional organization representing another facility (i.e. andere
   *     Einrichtung / Unterkunft in disease-portal)
   * @return the created Patient resource
   */
  private Patient createPatient(
      DiseaseNotification diseaseNotification,
      PractitionerRole notifierRole,
      Optional<Organization> otherFacility) {
    if (isNewAddressFeatureEnabled) {
      return this.notifiedPersonCreationService.createPatient(
          diseaseNotification.getNotifiedPerson(), notifierRole, otherFacility);
    } else {
      return this.notifiedPersonCreationService.createPatient(
          diseaseNotification.getNotifiedPerson());
    }
  }

  private DiseaseNotificationContext createContext(
      DiseaseNotification diseaseNotification,
      PractitionerRole notifierRole,
      Patient notifiedPerson,
      Optional<Organization> otherFacility) {
    NotificationBundleDiseaseDataBuilder bundle = new NotificationBundleDiseaseDataBuilder();
    bundle.setDefaults();
    bundle.setNotifierRole(notifierRole);
    bundle.setNotifiedPerson(notifiedPerson);
    otherFacility.ifPresent(bundle::addAdditionalEntry);
    return new DiseaseNotificationContext(diseaseNotification, bundle, notifiedPerson);
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
    this.questionnaireResponses.addCommon(context, diseaseNotification.getCommon());
  }

  private void createDiseaseQuestionnaireResponse(
      DiseaseNotification diseaseNotification, DiseaseNotificationContext context) {
    QuestionnaireResponse disease = diseaseNotification.getDisease();
    if ((disease != null) && !disease.getItem().isEmpty()) {
      this.questionnaireResponses.addSpecific(context, disease);
    }
  }

  private void createComposition(DiseaseNotificationContext context) {
    NotificationBundleDiseaseDataBuilder bundle = context.bundle();
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
      composition.setIdentifierAsNotificationId(initialNotificationId);
    }
    bundle.setNotificationDisease(composition.build());
  }
}
