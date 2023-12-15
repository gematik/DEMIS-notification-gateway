/*
 * Copyright [2023], gematik GmbH
 *
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
 */

package de.gematik.demis.notificationgateway.domain.hospitalization.fhir;

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.CODE_SYSTEM_CONDITION_VERIFICATION_STATUS;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.CODE_SYSTEM_NOTIFICATION_DISEASE_CATEGORY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.gematik.demis.notificationgateway.common.constants.FhirConstants;
import de.gematik.demis.notificationgateway.common.dto.ConditionInfo;
import de.gematik.demis.notificationgateway.common.dto.Hospitalization;
import de.gematik.demis.notificationgateway.common.exceptions.BadRequestException;
import de.gematik.demis.notificationgateway.common.utils.DateUtils;
import de.gematik.demis.notificationgateway.utils.FileUtils;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DiseaseCVDDCreationServiceTest {

  private final DiseaseCVDDCreationService creationService = new DiseaseCVDDCreationService();

  @SuppressWarnings("unused")
  private static Stream<Arguments> provideConditionInfo() {
    return Stream.of(Arguments.of((ConditionInfo) null), Arguments.of(new ConditionInfo()));
  }

  @ParameterizedTest
  @MethodSource("provideConditionInfo")
  void testCreateDiseaseCVDDWithEmptyConditionInfo(ConditionInfo input) throws BadRequestException {
    final Condition diseaseCVDD = creationService.createDiseaseCVDD(input, new Patient());
    assertNotNull(diseaseCVDD);

    assertTrue(diseaseCVDD.hasId());

    assertTrue(diseaseCVDD.hasMeta());
    final Meta meta = diseaseCVDD.getMeta();
    assertTrue(meta.hasProfile());
    assertEquals(FhirConstants.PROFILE_DISEASE_CVDD, meta.getProfile().get(0).asStringValue());

    assertTrue(diseaseCVDD.hasVerificationStatus());
    final List<Coding> verificationStatusCodings = diseaseCVDD.getVerificationStatus().getCoding();
    assertEquals(1, verificationStatusCodings.size());
    final Coding verificationStatusCoding = verificationStatusCodings.get(0);
    assertEquals(CODE_SYSTEM_CONDITION_VERIFICATION_STATUS, verificationStatusCoding.getSystem());
    assertEquals("confirmed", verificationStatusCoding.getCode());

    assertTrue(diseaseCVDD.hasCode());
    final List<Coding> codeCodings = diseaseCVDD.getCode().getCoding();
    assertEquals(1, codeCodings.size());
    final Coding codeCoding = codeCodings.get(0);
    assertEquals(CODE_SYSTEM_NOTIFICATION_DISEASE_CATEGORY, codeCoding.getSystem());
    assertEquals("cvdd", codeCoding.getCode());
    assertEquals("Coronavirus-Krankheit-2019 (COVID-19)", codeCoding.getDisplay());

    assertTrue(diseaseCVDD.hasSubject());
    assertFalse(diseaseCVDD.hasRecordedDate());
    assertFalse(diseaseCVDD.hasOnset());
    assertFalse(diseaseCVDD.hasEvidence());
    assertFalse(diseaseCVDD.hasNote());
  }

  @Test
  void testDatesInsideDiseaseCVDDAreWithDayPrecision()
      throws JsonProcessingException, BadRequestException {
    Hospitalization hospitalization =
        FileUtils.createHospitalization("portal/disease/notification_content_max.json");

    final Condition diseaseCVDD =
        creationService.createDiseaseCVDD(
            hospitalization.getDisease().getConditionInfo(), new Patient());
    assertNotNull(diseaseCVDD);

    assertTrue(diseaseCVDD.hasOnset());
    assertEquals(
        DateUtils.createDate(LocalDate.of(2022, 1, 1)),
        diseaseCVDD.getOnsetDateTimeType().getValue());
    assertEquals("2022-01-01", diseaseCVDD.getOnsetDateTimeType().getValueAsString());

    assertTrue(diseaseCVDD.hasRecordedDate());
    assertEquals(DateUtils.createDate(LocalDate.of(2022, 1, 2)), diseaseCVDD.getRecordedDate());
    assertEquals("2022-01-02", diseaseCVDD.getRecordedDateElement().getValueAsString());
  }
}
