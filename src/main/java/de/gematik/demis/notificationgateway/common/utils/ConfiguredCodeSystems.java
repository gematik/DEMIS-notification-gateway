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
 * #L%
 */

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.CODE_SYSTEM_NULL_FLAVOR;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeSystem.ConceptDefinitionComponent;
import org.hl7.fhir.r4.model.Coding;

@Slf4j
public class ConfiguredCodeSystems {
  private static ConfiguredCodeSystems instance;
  // DEMIS code systems
  private final Map<String, Coding> addressUseCodeSystemMap = new HashMap<>();
  private final Map<String, Coding> organizationTypeCodeSystemMap = new HashMap<>();
  private final Map<String, Coding> notificationCategoryCodeSystemMap = new HashMap<>();
  private final Map<String, Coding> conclusionCodeCodeSystemMap = new HashMap<>();
  private final Map<String, Coding> notificationDiseaseCategoryCodeSystemMap = new HashMap<>();
  private final Map<String, Coding> hospitalizationServiceTypeCodeSystemMap = new HashMap<>();
  private final Map<String, Coding> vaccineCodeSystemMap = new HashMap<>();
  private final Map<String, Coding> yesOrNoCodeSystemMap = new HashMap<>();
  private final Map<String, Coding> militaryAffiliationCodeSystemMap = new HashMap<>();
  private final Map<String, Coding> geographicRegionCodeSystemMap = new HashMap<>();
  private final Map<String, Coding> organizationAssociationCodeSystemMap = new HashMap<>();
  private final Map<String, Coding> infectionEnvironmentSettingCodeSystemMap = new HashMap<>();
  private final Map<String, Coding> notificationTypeCodeSystemMap = new HashMap<>();
  private final Map<String, Coding> sectionCodeCodeSystemMap = new HashMap<>();

  private final Map<String, Coding> nullFlavors = new HashMap<>();

  // general code systems
  private final Map<String, Coding> observationCategoryCodeSystemMap = new HashMap<>();

  private final IParser parser;

  public static ConfiguredCodeSystems getInstance() {
    if (instance == null) {
      instance = new ConfiguredCodeSystems();
    }
    return instance;
  }

  private ConfiguredCodeSystems() {
    FhirContext ctx = FhirContext.forR4();
    parser = ctx.newJsonParser();

    loadCodeSystem("/codesystem/addressUse.json", addressUseCodeSystemMap);
    loadCodeSystem("/codesystem/organizationType.json", organizationTypeCodeSystemMap);
    loadCodeSystem("/codesystem/notificationCategory.json", notificationCategoryCodeSystemMap);
    loadCodeSystem("/codesystem/conclusionCode.json", conclusionCodeCodeSystemMap);
    loadCodeSystem(
        "/codesystem/CodeSystem-observation-category.json", observationCategoryCodeSystemMap);
    loadCodeSystem(
        "/codesystem/notificationDiseaseCategory.json", notificationDiseaseCategoryCodeSystemMap);
    loadCodeSystem(
        "/codesystem/hospitalizationServiceType.json", hospitalizationServiceTypeCodeSystemMap);
    loadCodeSystem("/codesystem/vaccine.json", vaccineCodeSystemMap);
    loadCodeSystem("/codesystem/yesOrNoAnswer.json", yesOrNoCodeSystemMap);
    loadCodeSystem("/codesystem/militaryAffiliation.json", militaryAffiliationCodeSystemMap);
    loadCodeSystem("/codesystem/geographicRegion.json", geographicRegionCodeSystemMap);
    loadCodeSystem(
        "/codesystem/organizationAssociation.json", organizationAssociationCodeSystemMap);
    loadCodeSystem(
        "/codesystem/infectionEnvironmentSetting.json", infectionEnvironmentSettingCodeSystemMap);
    loadCodeSystem("/codesystem/notificationType.json", notificationTypeCodeSystemMap);
    loadCodeSystem("/codesystem/sectionCode.json", sectionCodeCodeSystemMap);

    initNullFlavors();
  }

  private void initNullFlavors() {
    nullFlavors.put("NASK", new Coding(CODE_SYSTEM_NULL_FLAVOR, "NASK", "not asked"));
    nullFlavors.put("ASKU", new Coding(CODE_SYSTEM_NULL_FLAVOR, "ASKU", "asked but unknown"));
  }

  public Coding getAddressUseCoding(final String code) {
    return addressUseCodeSystemMap.get(code);
  }

  public Coding getOrganizationTypeCoding(final String code) {
    return organizationTypeCodeSystemMap.get(code);
  }

  public Coding getObservationCategoryCoding(final String code) {
    return observationCategoryCodeSystemMap.get(code);
  }

  public Coding getNotificationCategoryCoding(final String code) {
    return notificationCategoryCodeSystemMap.get(code);
  }

  public Coding getConclusionCodeCoding(final String code) {
    return conclusionCodeCodeSystemMap.get(code);
  }

  public Coding getNotificationDiseaseCategoryCoding(final String code) {
    return notificationDiseaseCategoryCodeSystemMap.get(code);
  }

  public Coding getHospitalizationServiceTypeCoding(final String code) {
    return hospitalizationServiceTypeCodeSystemMap.get(code);
  }

  public Coding getVaccineCoding(final String code) {
    return vaccineCodeSystemMap.get(code);
  }

  public Coding getNullFlavor(final String code) {
    return nullFlavors.get(code);
  }

  public Coding getYesOrNoCoding(final String code) {
    return yesOrNoCodeSystemMap.get(code);
  }

  public Coding getMilitaryAffiliationCoding(final String code) {
    return militaryAffiliationCodeSystemMap.get(code);
  }

  public Coding getGeographicRegionCoding(final String code) {
    return geographicRegionCodeSystemMap.get(code);
  }

  public Coding getOrganizationAssociationCoding(final String code) {
    return organizationAssociationCodeSystemMap.get(code);
  }

  public Coding getInfectionEnvironmentSettingCoding(final String code) {
    return infectionEnvironmentSettingCodeSystemMap.get(code);
  }

  public Coding getNotificationTypeCoding(final String code) {
    return notificationTypeCodeSystemMap.get(code);
  }

  public Coding getSectionCodeCoding(final String code) {
    return sectionCodeCodeSystemMap.get(code);
  }

  private void loadCodeSystem(final String resourceName, final Map<String, Coding> codeSystemMap) {
    log.debug("loading CodeSystem from resource {}", resourceName);

    final InputStream contentStream = FileUtils.loadFileFromClasspath(resourceName);
    if (contentStream == null) {
      log.error("failed to load content, resource not found: {}", resourceName);
      return;
    }

    final boolean result = fillCodeSystemMap(contentStream, codeSystemMap);
    if (!result) {
      log.error("failed to load codeSystem from resource {}", resourceName);
    }
  }

  private boolean fillCodeSystemMap(
      final InputStream codeSystemContentStream, final Map<String, Coding> codeSystemMap) {
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

  private void addSubCodings(
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

  private void addCoding(
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
}
