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

import de.gematik.demis.notificationgateway.common.dto.QuestionnaireResponseAnswer;
import java.util.function.Predicate;
import org.hl7.fhir.r4.model.Type;

/**
 * A specific data type of questionnaire response item answer. It works like a reduced visitor
 * pattern.
 */
interface DataType<F extends Type> extends Predicate<QuestionnaireResponseAnswer> {

  /**
   * Convert to FHIR data object
   *
   * @param answer answer with a value
   * @return FHIR data object or <code>null</code> if value not set
   */
  F toFhir(QuestionnaireResponseAnswer answer);

  /**
   * Test if the answer is of the supported data type
   *
   * @param answer answer with a value
   * @return <code>true</code> if the answer is of the supported data type
   */
  @Override
  boolean test(QuestionnaireResponseAnswer answer);
}
