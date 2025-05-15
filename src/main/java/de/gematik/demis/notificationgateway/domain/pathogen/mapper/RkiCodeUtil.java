package de.gematik.demis.notificationgateway.domain.pathogen.mapper;

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

import de.gematik.demis.notificationgateway.common.dto.ResistanceDTO;
import de.gematik.demis.notificationgateway.common.dto.ResistanceGeneDTO;

/** Utility class to map resistance and resistance gene results to codes determinated by the RKI. */
public class RkiCodeUtil {

  /**
   * Maps the resistance gene result to the corresponding RKI codes.
   *
   * @param interpretationEnum
   * @return
   */
  public static InterpretationValueCode getInterpretationValueCodeForResistanceGene(
      ResistanceGeneDTO.ResistanceGeneResultEnum interpretationEnum) {
    return switch (interpretationEnum) {
      case DETECTED -> new InterpretationValueCode("R", "260373001");
      case NOT_DETECTED -> new InterpretationValueCode("S", "260415000");
      case INDETERMINATE -> new InterpretationValueCode("IND", "82334004");
    };
  }

  /**
   * Maps the resistance result to the corresponding RKI codes.
   *
   * @param interpretationEnum
   * @return
   */
  public static InterpretationValueCode getInterpretationValueCodeForResistance(
      ResistanceDTO.ResistanceResultEnum interpretationEnum) {
    return switch (interpretationEnum) {
      case RESISTANT -> new InterpretationValueCode("R", "30714006");
      case SUSCEPTIBLE_WITH_INCREASED_EXPOSURE -> new InterpretationValueCode("I", "1255965005");
      case INTERMEDIATE -> new InterpretationValueCode("I", "264841006");
      case SUSCEPTIBLE -> new InterpretationValueCode("S", "131196009");
      case INDETERMINATE -> new InterpretationValueCode("IND", "82334004");
    };
  }

  public record InterpretationValueCode(String interpretation, String valueCode) {}
}
