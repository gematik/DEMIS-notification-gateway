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

package de.gematik.demis.notificationgateway.common.utils;

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.DEMIS_RKI_DE_FHIR;

import de.gematik.demis.notificationgateway.common.dto.VaccinationInfo;
import de.gematik.demis.notificationgateway.common.enums.VaccineCoding;
import de.gematik.demis.notificationgateway.common.exceptions.BadRequestException;
import lombok.experimental.UtilityClass;
import org.apache.logging.log4j.util.Strings;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;

@UtilityClass
public class ReferenceUtils {

  public static String getFullUrl(Resource resource) {
    return DEMIS_RKI_DE_FHIR + resource.getResourceType().toString() + "/" + resource.getId();
  }

  public static Reference createReference(Resource resource) {
    return new Reference(resource.getResourceType().toString() + "/" + resource.getId());
  }

  public static Annotation createAnnotation(String additionalInfo) {
    if (Strings.isBlank(additionalInfo)) {
      return null;
    }
    return new Annotation().setText(additionalInfo);
  }

  public static CodeableConcept createCodeableConcept(VaccinationInfo.VaccineEnum vaccine)
      throws BadRequestException {

    return new CodeableConcept(VaccineCoding.byVaccine(vaccine).getCoding());
  }
}
