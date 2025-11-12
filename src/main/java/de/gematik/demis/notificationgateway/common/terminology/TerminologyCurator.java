package de.gematik.demis.notificationgateway.common.terminology;

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

import ca.uhn.fhir.context.FhirContext;
import de.gematik.demis.notificationgateway.common.dto.TerminologyVersion;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;

/**
 * Curates FHIR resources by setting terminology versions on all codings based on provided
 * terminology versions.
 */
@RequiredArgsConstructor
@Slf4j
public final class TerminologyCurator {

  private final List<TerminologyVersion> terminologyVersions;
  private final FhirContext fhirContext = FhirContext.forR4Cached();

  /**
   * Sets the terminology versions to all codings of the given bundle
   *
   * @param bundle the FHIR bundle to set the terminology versions on
   */
  public void setCodeSystemVersions(Bundle bundle) {
    log(bundle);
    this.fhirContext.newTerser().visit(bundle, createVisitor());
  }

  private void log(Bundle bundle) {
    if (log.isDebugEnabled()) {
      final Identifier identifier = bundle.getIdentifier();
      log.debug(
          "Setting terminology versions to all codings of the bundle. NotificationBundleId: {}",
          identifier == null ? "null" : identifier.getValue());
    }
  }

  private TerminologyVersionsVisitor createVisitor() {
    return new TerminologyVersionsVisitor(createTerminologyVersionsMap());
  }

  private Map<String, String> createTerminologyVersionsMap() {
    if ((this.terminologyVersions == null) || this.terminologyVersions.isEmpty()) {
      return Map.of();
    }
    return this.terminologyVersions.stream()
        .collect(Collectors.toMap(TerminologyVersion::getSystem, TerminologyVersion::getVersion));
  }
}
