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

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.disease.NotificationBundleDiseaseDataBuilder;
import de.gematik.demis.notificationgateway.common.constants.FhirConstants;
import de.gematik.demis.notificationgateway.common.dto.DiseaseNotification;
import de.gematik.demis.notificationgateway.common.dto.DiseaseStatus;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DiseasesTest {

  private final Diseases diseases = new Diseases();

  private DiseaseStatus status;
  private DiseaseNotification notification;
  private DiseaseNotificationContext context;

  @BeforeEach
  void setUp() {
    this.status = new DiseaseStatus();
    this.status.setCategory("cvdp");
    this.notification = new DiseaseNotification();
    this.notification.setStatus(this.status);
    Patient notifiedPerson = new Patient();
    NotificationBundleDiseaseDataBuilder bundle = new NotificationBundleDiseaseDataBuilder();
    bundle.setDefaults();
    bundle.setNotifiedPerson(notifiedPerson);
    bundle.setCommonInformation(new QuestionnaireResponse());
    bundle.setSpecificInformation(new QuestionnaireResponse());
    this.context = new DiseaseNotificationContext(this.notification, bundle, notifiedPerson);
  }

  private Condition createCondition() {
    this.diseases.addDisease(this.context);
    Bundle bundle = this.context.bundle().build();
    return bundle.getEntry().stream()
        .map(Bundle.BundleEntryComponent::getResource)
        .filter(Condition.class::isInstance)
        .map(Condition.class::cast)
        .findFirst()
        .orElseThrow();
  }

  @ParameterizedTest
  @CsvSource({
    "FINAL, active, confirmed",
    "PRELIMINARY, active, unconfirmed",
    "AMENDED, active, confirmed",
    "REFUTED, inactive, refuted",
    "ERROR, , entered-in-error"
  })
  void addDisease_shouldProcessStatus(
      String status, String clinicalStatusCode, String verificationStatusCode) {
    this.status.setStatus(DiseaseStatus.StatusEnum.valueOf(status));
    Condition condition = createCondition();

    // clinical status
    if (StringUtils.isBlank(clinicalStatusCode)) {
      assertThat(condition.hasClinicalStatus()).as("clinical status not set").isFalse();
    } else {
      assertThat(condition.hasClinicalStatus()).as("clinical status set").isTrue();
      Coding clinicalStatus = condition.getClinicalStatus().getCodingFirstRep();
      assertThat(clinicalStatus.getSystem())
          .as("clinical status system")
          .isEqualTo(ClinicalStatus.CODE_SYSTEM);
      assertThat(clinicalStatus.getCode()).as("clinical status code").isEqualTo(clinicalStatusCode);
    }

    // verification status
    Coding verificationStatus = condition.getVerificationStatus().getCodingFirstRep();
    assertThat(verificationStatus.getSystem())
        .as("verification status system")
        .isEqualTo(FhirConstants.CODE_SYSTEM_CONDITION_VERIFICATION_STATUS);
    assertThat(verificationStatus.getCode())
        .as("verification status code")
        .isEqualTo(verificationStatusCode);
  }
}
