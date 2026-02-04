package de.gematik.demis.notificationgateway.common.mappers;

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

import de.gematik.demis.notificationgateway.common.dto.Gender;
import java.util.Optional;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Extension;

/**
 * Mapping Table:
 *
 * <p>"Männlich" → Code Male without Extension "Weiblich" → Code female without Extension "Divers" -
 * Code Other with Extension D "Kein Geschlechtseintrag" - Code Other with Extension X "Unbekannt" -
 * Code Unknown without Extension
 */
public class GenderMapper {

  private GenderMapper() {}

  public static Enumerations.AdministrativeGender mapGender(Gender gender) {
    if (gender == Gender.DIVERSE || gender == Gender.OTHERX) {
      return Enumerations.AdministrativeGender.OTHER;
    } else return Enumerations.AdministrativeGender.valueOf(gender.getValue());
  }

  public static Optional<Extension> createGenderExtension(Gender gender) {
    if (gender == Gender.DIVERSE || gender == Gender.OTHERX) {
      String code = gender == Gender.DIVERSE ? "D" : "X";
      String display = gender == Gender.DIVERSE ? "Divers" : "Kein Geschlechtseintrag";
      Coding coding = new Coding("http://fhir.de/CodeSystem/gender-amtlich-de", code, display);
      Extension extension =
          new Extension("http://fhir.de/StructureDefinition/gender-amtlich-de", coding);
      return Optional.of(extension);
    }
    return Optional.empty();
  }
}
