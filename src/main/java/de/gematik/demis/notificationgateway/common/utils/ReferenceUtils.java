package de.gematik.demis.notificationgateway.common.utils;

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

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.DEMIS_RKI_DE_FHIR;

import lombok.experimental.UtilityClass;
import org.apache.logging.log4j.util.Strings;
import org.hl7.fhir.r4.model.Annotation;
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
}
