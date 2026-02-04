package de.gematik.demis.notificationgateway.domain.disease.fhir.questionnaire.answer;

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

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.demis.notificationgateway.FeatureFlags;
import de.gematik.demis.notificationgateway.common.dto.CodeDisplay;
import de.gematik.demis.notificationgateway.common.dto.QuestionnaireResponseAnswer;
import org.hl7.fhir.r4.model.Coding;
import org.junit.jupiter.api.Test;

class CodingDataTypeTest {

  private static final String SYSTEM_VERSION = "v1";
  private static final String SYSTEM_URL = "http://system";
  private static final String CODE = "code123";

  private static CodingDataType createCodingDataType(boolean diseaseStrictProfile) {
    return new CodingDataType(
        FeatureFlags.builder().diseaseStrictProfile(diseaseStrictProfile).build());
  }

  @Test
  void givenSystemVersionWhenStrictCodingToFhirThenReturnCodingWithVersion() {

    // given
    final QuestionnaireResponseAnswer openApiItemAnswer = new QuestionnaireResponseAnswer();
    CodeDisplay coding = new CodeDisplay();
    coding.setSystem(SYSTEM_URL);
    coding.setCode(CODE);
    coding.setVersion(SYSTEM_VERSION);
    openApiItemAnswer.setValueCoding(coding);

    // when
    final Coding fhirCoding = createCodingDataType(true).toFhir(openApiItemAnswer);

    // then
    assertThat(fhirCoding).isNotNull();
    assertThat(fhirCoding.getVersion()).isEqualTo(SYSTEM_VERSION);
    assertThat(fhirCoding.getSystem()).isEqualTo(SYSTEM_URL);
    assertThat(fhirCoding.getCode()).isEqualTo(CODE);
    assertThat(fhirCoding.getDisplay()).isNull();
  }

  @Test
  void givenNoSystemVersionWhenStrictCodingToFhirThenReturnCodingWithoutVersion() {

    // given
    final QuestionnaireResponseAnswer openApiItemAnswer = new QuestionnaireResponseAnswer();
    CodeDisplay coding = new CodeDisplay();
    coding.setSystem(SYSTEM_URL);
    coding.setCode(CODE);
    openApiItemAnswer.setValueCoding(coding);

    // when
    final Coding fhirCoding = createCodingDataType(true).toFhir(openApiItemAnswer);

    // then
    assertThat(fhirCoding).isNotNull();
    assertThat(fhirCoding.getVersion()).isNull();
    assertThat(fhirCoding.getSystem()).isEqualTo(SYSTEM_URL);
    assertThat(fhirCoding.getCode()).isEqualTo(CODE);
    assertThat(fhirCoding.getDisplay()).isNull();
  }

  @Test
  void givenSystemVersionWhenNonStrictCodingToFhirThenReturnCodingWithoutVersion() {

    // DELETE WITH FEATURE_FLAG_DISEASE_STRICT

    // given
    final QuestionnaireResponseAnswer openApiItemAnswer = new QuestionnaireResponseAnswer();
    CodeDisplay coding = new CodeDisplay();
    coding.setSystem(SYSTEM_URL);
    coding.setCode(CODE);
    coding.setVersion(SYSTEM_VERSION);
    openApiItemAnswer.setValueCoding(coding);

    // when
    final Coding fhirCoding = createCodingDataType(false).toFhir(openApiItemAnswer);

    // then
    assertThat(fhirCoding).isNotNull();
    assertThat(fhirCoding.getVersion()).isNull();
    assertThat(fhirCoding.getSystem()).isEqualTo(SYSTEM_URL);
    assertThat(fhirCoding.getCode()).isEqualTo(CODE);
    assertThat(fhirCoding.getDisplay()).isNull();
  }

  @Test
  void givenNoSystemVersionWhenNonStrictCodingToFhirThenReturnCodingWithoutVersion() {

    // DELETE WITH FEATURE_FLAG_DISEASE_STRICT

    // given
    final QuestionnaireResponseAnswer openApiItemAnswer = new QuestionnaireResponseAnswer();
    CodeDisplay coding = new CodeDisplay();
    coding.setSystem(SYSTEM_URL);
    coding.setCode(CODE);
    openApiItemAnswer.setValueCoding(coding);

    // when
    final Coding fhirCoding = createCodingDataType(false).toFhir(openApiItemAnswer);

    // then
    assertThat(fhirCoding).isNotNull();
    assertThat(fhirCoding.getVersion()).isNull();
    assertThat(fhirCoding.getSystem()).isEqualTo(SYSTEM_URL);
    assertThat(fhirCoding.getCode()).isEqualTo(CODE);
    assertThat(fhirCoding.getDisplay()).isNull();
  }
}
