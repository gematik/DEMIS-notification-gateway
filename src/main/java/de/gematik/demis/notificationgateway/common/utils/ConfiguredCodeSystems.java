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

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeSystem.ConceptDefinitionComponent;
import org.hl7.fhir.r4.model.Coding;

@RequiredArgsConstructor
@Slf4j
public final class ConfiguredCodeSystems {

  private static ConfiguredCodeSystems instance;

  private final Map<String, Coding> addressUseCodeSystemMap;
  private final Map<String, Coding> organizationTypeCodeSystemMap;

  public static ConfiguredCodeSystems getInstance() {
    if (instance == null) {
      final IParser parser = FhirContext.forR4Cached().newJsonParser();
      final Map<String, Coding> addressUse = loadCodeSystem(parser, "/codesystem/addressUse.json");
      final Map<String, Coding> organizationType =
          loadCodeSystem(parser, "/codesystem/organizationType.json");
      instance = new ConfiguredCodeSystems(addressUse, organizationType);
    }
    return instance;
  }

  private static Map<String, Coding> loadCodeSystem(
      final IParser parser, final String resourceName) {
    log.debug("loading CodeSystem from resource {}", resourceName);
    final InputStream contentStream = FileUtils.loadFileFromClasspath(resourceName);
    if (contentStream == null) {
      log.error("failed to load content, resource not found: {}", resourceName);
      throw new IllegalArgumentException(
          "failed to load content, resource not found: " + resourceName);
    }
    final Map<String, Coding> codeSystemMap = new HashMap<>();
    final boolean result = fillCodeSystemMap(parser, contentStream, codeSystemMap);
    if (!result) {
      log.error("failed to load codeSystem from resource {}", resourceName);
    }
    return codeSystemMap;
  }

  private static boolean fillCodeSystemMap(
      final IParser parser,
      final InputStream codeSystemContentStream,
      final Map<String, Coding> codeSystemMap) {
    final CodeSystem parsedCodeSystem =
        parser.parseResource(CodeSystem.class, codeSystemContentStream);
    if (!parsedCodeSystem.hasConcept()) {
      log.error("concept field is missing");
      return false;
    }

    if (!parsedCodeSystem.hasUrl()) {
      log.error("url field is missing");
      return false;
    }

    final String systemUrl = parsedCodeSystem.getUrl();
    final ListIterator<ConceptDefinitionComponent> conceptIterator =
        parsedCodeSystem.getConcept().listIterator(0);

    while (conceptIterator.hasNext()) {
      final CodeSystem.ConceptDefinitionComponent conceptDefinitionComponent =
          conceptIterator.next();
      addCoding(codeSystemMap, conceptDefinitionComponent, systemUrl);
      addSubCodings(codeSystemMap, systemUrl, conceptDefinitionComponent);
    }

    if (parsedCodeSystem.hasCount()
        && (parsedCodeSystem.getCount() != codeSystemMap.keySet().size())) {
      log.error("parsing of code system map incomplete: url=" + systemUrl);
      return false;
    }

    return true;
  }

  private static void addSubCodings(
      Map<String, Coding> codeSystemMap,
      String systemUrl,
      ConceptDefinitionComponent conceptDefinitionComponent) {
    if (conceptDefinitionComponent.hasConcept()) {
      final List<ConceptDefinitionComponent> conceptList = conceptDefinitionComponent.getConcept();
      final ListIterator<ConceptDefinitionComponent> conceptListIterator =
          conceptList.listIterator(0);
      while (conceptListIterator.hasNext()) {
        final ConceptDefinitionComponent next = conceptListIterator.next();
        addCoding(codeSystemMap, next, systemUrl);
        addSubCodings(codeSystemMap, systemUrl, next);
      }
    }
  }

  private static void addCoding(
      final Map<String, Coding> codeSystemMap,
      final ConceptDefinitionComponent conceptDefinitionComponent,
      final String system) {

    final Coding coding =
        new Coding().setCode(conceptDefinitionComponent.getCode()).setSystem(system);

    if (conceptDefinitionComponent.hasDisplay()) {
      coding.setDisplay(conceptDefinitionComponent.getDisplay());
    }

    codeSystemMap.put(conceptDefinitionComponent.getCode(), coding);
  }

  private static Coding copy(Coding coding) {
    if (coding == null) {
      return null;
    }
    return coding.copy();
  }

  public Coding getAddressUseCoding(final String code) {
    return copy(addressUseCodeSystemMap.get(code));
  }

  public Coding getOrganizationTypeCoding(final String code) {
    return copy(organizationTypeCodeSystemMap.get(code));
  }
}
