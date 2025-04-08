package de.gematik.demis.notificationgateway.domain.disease;

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

import de.gematik.demis.notificationgateway.common.dto.Condition;
import de.gematik.demis.notificationgateway.common.dto.ContactPointInfo;
import de.gematik.demis.notificationgateway.common.dto.DiseaseNotification;
import de.gematik.demis.notificationgateway.common.dto.DiseaseStatus;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPerson;
import de.gematik.demis.notificationgateway.common.dto.NotifierFacility;
import de.gematik.demis.notificationgateway.utils.FileUtils;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import java.util.Locale;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DiseaseRestControllerNotificationValidationTest {

  private static final Locale DEFAULT_LOCALE = Locale.getDefault();
  private static DiseaseRestController controller;
  private DiseaseNotification diseaseNotification;

  @BeforeAll
  static void setup() {
    setLanguage();
    createController();
  }

  private static void setLanguage() {
    Locale.setDefault(Locale.ENGLISH);
  }

  private static void createController() {
    final var validator = Validation.buildDefaultValidatorFactory().getValidator();
    controller = new DiseaseRestController(validator, null);
  }

  @AfterAll
  static void resetLanguage() {
    Locale.setDefault(DEFAULT_LOCALE);
  }

  @BeforeEach
  void createValidDiseaseNotification() {
    this.diseaseNotification =
        FileUtils.createDiseaseNotification("portal/disease/notification-formly-input.json");
  }

  @Test
  void addDiseaseNotification_shouldRejectNotifierFacilityMissingContactValue() {
    NotifierFacility notifierFacility = this.diseaseNotification.getNotifierFacility();
    ContactPointInfo contact = notifierFacility.getContacts().getFirst();
    assertThat(contact.getContactType())
        .as("using mail contact")
        .isSameAs(ContactPointInfo.ContactTypeEnum.EMAIL);
    contact.setValue(null);
    Assertions.assertThatThrownBy(
            () -> controller.addDiseaseNotification(this.diseaseNotification, null))
        .isInstanceOf(ConstraintViolationException.class)
        .hasMessage("notifierFacility.contacts[0].value: must not be null");
  }

  @Test
  void addDiseaseNotification_shouldRejectNotifiedPersonMissingFirstName() {
    NotifiedPerson notifiedPerson = this.diseaseNotification.getNotifiedPerson();
    notifiedPerson.getInfo().setFirstname(null);
    Assertions.assertThatThrownBy(
            () -> controller.addDiseaseNotification(this.diseaseNotification, null))
        .isInstanceOf(ConstraintViolationException.class)
        .hasMessage("notifiedPerson.info.firstname: must not be null");
  }

  @Test
  void addDiseaseNotification_shouldRejectStatusMissingCategory() {
    DiseaseStatus status = this.diseaseNotification.getStatus();
    status.setCategory(null);
    Assertions.assertThatThrownBy(
            () -> controller.addDiseaseNotification(this.diseaseNotification, null))
        .isInstanceOf(ConstraintViolationException.class)
        .hasMessage("status.category: must not be null");
  }

  @Test
  void addDiseaseNotification_shouldRejectConditionEvidenceMissingCode() {
    Condition condition = this.diseaseNotification.getCondition();
    condition.getEvidence().getFirst().setCode(null);
    Assertions.assertThatThrownBy(
            () -> controller.addDiseaseNotification(this.diseaseNotification, null))
        .isInstanceOf(ConstraintViolationException.class)
        .hasMessage("condition.evidence[0].code: must not be null");
  }

  @Test
  void addDiseaseNotification_shouldRejectNotificationMissingStatus() {
    this.diseaseNotification.setStatus(null);
    Assertions.assertThatThrownBy(
            () -> controller.addDiseaseNotification(this.diseaseNotification, null))
        .isInstanceOf(ConstraintViolationException.class)
        .hasMessage("status: must not be null");
  }

  @Test
  void addDiseaseNotification_shouldRejectNotificationMissingCondition() {
    this.diseaseNotification.setCondition(null);
    Assertions.assertThatThrownBy(
            () -> controller.addDiseaseNotification(this.diseaseNotification, null))
        .isInstanceOf(ConstraintViolationException.class)
        .hasMessage("condition: must not be null");
  }
}
