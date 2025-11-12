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

import ca.uhn.fhir.context.BaseRuntimeChildDefinition;
import ca.uhn.fhir.context.BaseRuntimeElementDefinition;
import ca.uhn.fhir.util.IModelVisitor2;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.r4.model.Coding;

@RequiredArgsConstructor
@Slf4j
final class TerminologyVersionsVisitor implements IModelVisitor2 {

  /** Default version to set if no specific version is provided for a terminology system */
  static final String VERSION_DEFAULT = "0.0.0";

  private final Map<String, String> terminologyVersions;

  @Override
  public boolean acceptElement(
      IBase element,
      List<IBase> theContainingElementPath,
      List<BaseRuntimeChildDefinition> theChildDefinitionPath,
      List<BaseRuntimeElementDefinition<?>> theElementDefinitionPath) {
    checkVersionIfCoding(element);
    return true;
  }

  @Override
  public boolean acceptUndeclaredExtension(
      IBaseExtension<?, ?> extension,
      List<IBase> theContainingElementPath,
      List<BaseRuntimeChildDefinition> theChildDefinitionPath,
      List<BaseRuntimeElementDefinition<?>> theElementDefinitionPath) {
    checkVersionIfCoding(extension.getValue());
    return true;
  }

  private void checkVersionIfCoding(Object object) {
    if (object instanceof Coding coding) {
      setVersionIfEmpty(coding);
    }
  }

  private void setVersionIfEmpty(Coding coding) {
    final String system = coding.getSystem();
    final String existingVersion = coding.getVersion();
    if (existingVersion != null) {
      log.debug(
          "Coding with system '{}' already has version '{}', not overwriting it.",
          system,
          existingVersion);
    } else {
      setVersion(coding, system);
    }
  }

  private void setVersion(Coding coding, String system) {
    final String version = terminologyVersions.get(system);
    if (version != null) {
      coding.setVersion(version);
    } else {
      coding.setVersion(VERSION_DEFAULT);
      log.debug(
          "No terminology version found for coding system! Using default version. System: {} Version: {}",
          system,
          VERSION_DEFAULT);
    }
  }
}
