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

package de.gematik.demis.notificationgateway.domain.hospitalization.fhir;

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.CODE_SYSTEM_ACT_CODE;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.CODE_SYSTEM_HOSPITALIZATION_SERVICE_TYPE;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.STRUCTURE_DEFINITION_HOSPITALIZATION_NOTE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.gematik.demis.notificationgateway.common.constants.FhirConstants;
import de.gematik.demis.notificationgateway.common.dto.Hospitalization;
import de.gematik.demis.notificationgateway.common.utils.DateUtils;
import de.gematik.demis.notificationgateway.utils.FileUtils;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Encounter.EncounterStatus;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

class EncounterCreationServiceTest {

  private final EncounterCreationService creationService = new EncounterCreationService();

  @Test
  void testCreateHospitalizationEncounterWithMinimumInput() throws JsonProcessingException {
    Hospitalization hospitalization =
        FileUtils.createHospitalization(
            "portal/disease/encounter/notification_content_hospitalized_min.json");

    final Optional<Encounter> hospitalizationEncounterOptional =
        creationService.createHospitalizationEncounter(
            hospitalization.getDisease().getDiseaseInfoCommon(), new Patient(), new Organization());

    assertTrue(hospitalizationEncounterOptional.isPresent());

    Encounter hospitalizationEncounter = hospitalizationEncounterOptional.get();
    assertTrue(hospitalizationEncounter.hasId());

    assertTrue(hospitalizationEncounter.hasMeta());
    final Meta meta = hospitalizationEncounter.getMeta();
    assertTrue(meta.hasProfile());
    assertEquals(FhirConstants.PROFILE_HOSPITALIZATION, meta.getProfile().get(0).asStringValue());

    assertEquals(EncounterStatus.INPROGRESS, hospitalizationEncounter.getStatus());
    final Coding classCoding = hospitalizationEncounter.getClass_();
    assertEquals(CODE_SYSTEM_ACT_CODE, classCoding.getSystem());
    assertEquals("IMP", classCoding.getCode());
    assertEquals("inpatient encounter", classCoding.getDisplay());

    assertTrue(hospitalizationEncounter.hasSubject());
    assertTrue(hospitalizationEncounter.hasServiceProvider());

    assertTrue(hospitalizationEncounter.hasPeriod());
    assertEquals(
        DateUtils.createDate(LocalDate.of(2022, 1, 5)),
        hospitalizationEncounter.getPeriod().getStartElement().getValue());
  }

  @Test
  void testCreateIntensiveCareEncounter() throws JsonProcessingException {
    Hospitalization hospitalization =
        FileUtils.createHospitalization(
            "portal/disease/encounter/notification_content_hospitalized_max.json");

    final Optional<Encounter> intensiveCareEncounterOptional =
        creationService.createIntensiveCareEncounter(
            hospitalization.getDisease().getDiseaseInfoCommon(), new Patient(), new Organization());

    assertTrue(intensiveCareEncounterOptional.isPresent());

    Encounter intensiveCareEncounter = intensiveCareEncounterOptional.get();
    assertTrue(intensiveCareEncounter.hasId());

    assertTrue(intensiveCareEncounter.hasMeta());
    final Meta meta = intensiveCareEncounter.getMeta();
    assertTrue(meta.hasProfile());
    assertEquals(FhirConstants.PROFILE_HOSPITALIZATION, meta.getProfile().get(0).asStringValue());

    assertEquals(EncounterStatus.INPROGRESS, intensiveCareEncounter.getStatus());
    final Coding classCoding = intensiveCareEncounter.getClass_();
    assertEquals(CODE_SYSTEM_ACT_CODE, classCoding.getSystem());
    assertEquals("IMP", classCoding.getCode());
    assertEquals("inpatient encounter", classCoding.getDisplay());

    assertTrue(intensiveCareEncounter.hasSubject());
    assertTrue(intensiveCareEncounter.hasServiceProvider());

    assertTrue(intensiveCareEncounter.hasServiceType());
    final List<Coding> serviceTypeCodings = intensiveCareEncounter.getServiceType().getCoding();
    assertEquals(1, serviceTypeCodings.size());
    final Coding serviceTypeCoding = serviceTypeCodings.get(0);
    assertEquals(CODE_SYSTEM_HOSPITALIZATION_SERVICE_TYPE, serviceTypeCoding.getSystem());
    assertEquals("3600", serviceTypeCoding.getCode());
    assertEquals("Intensivmedizin", serviceTypeCoding.getDisplay());

    assertTrue(intensiveCareEncounter.hasPeriod());
    assertEquals(
        DateUtils.createDate(LocalDate.of(2022, 1, 7)),
        intensiveCareEncounter.getPeriod().getStart());
  }

  @Test
  void testReturnsHospitalizationEncounterNullWhenNotHospitalized() throws JsonProcessingException {
    Hospitalization hospitalization =
        FileUtils.createHospitalization(
            "portal/disease/encounter/notification_content_not_hospitalized.json");

    final Optional<Encounter> hospitalizationEncounterOptional =
        creationService.createHospitalizationEncounter(
            hospitalization.getDisease().getDiseaseInfoCommon(), new Patient(), new Organization());

    assertFalse(hospitalizationEncounterOptional.isPresent());
  }

  @Test
  void testReturnsIntensiveCareEncounterNullWhenNotHospitalized() throws JsonProcessingException {
    Hospitalization hospitalization =
        FileUtils.createHospitalization(
            "portal/disease/encounter/notification_content_not_hospitalized.json");

    final Optional<Encounter> intensiveCareEncounterOptional =
        creationService.createIntensiveCareEncounter(
            hospitalization.getDisease().getDiseaseInfoCommon(), new Patient(), new Organization());

    assertFalse(intensiveCareEncounterOptional.isPresent());
  }

  @Test
  void testReturnsIntensiveCareEncounterNullWhenNotInIntensiveCare()
      throws JsonProcessingException {
    Hospitalization hospitalization =
        FileUtils.createHospitalization(
            "portal/disease/encounter/notification_content_hospitalized_min.json");

    final Optional<Encounter> intensiveCareEncounterOptional =
        creationService.createIntensiveCareEncounter(
            hospitalization.getDisease().getDiseaseInfoCommon(), new Patient(), new Organization());

    assertFalse(intensiveCareEncounterOptional.isPresent());
  }

  @Test
  void testHospitalizationEncounterContainsNote() throws JsonProcessingException {
    Hospitalization hospitalization =
        FileUtils.createHospitalization(
            "portal/disease/encounter/notification_content_hospitalized_max.json");

    final Optional<Encounter> hospitalizationEncounterOptional =
        creationService.createHospitalizationEncounter(
            hospitalization.getDisease().getDiseaseInfoCommon(), new Patient(), new Organization());

    assertTrue(hospitalizationEncounterOptional.isPresent());

    Encounter hospitalizationEncounter = hospitalizationEncounterOptional.get();
    assertTrue(hospitalizationEncounter.hasExtension());
    final List<Extension> extensions = hospitalizationEncounter.getExtension();
    assertEquals(1, extensions.size());
    final Extension extension = extensions.get(0);
    assertEquals(STRUCTURE_DEFINITION_HOSPITALIZATION_NOTE, extension.getUrl());
    assertEquals("wichtige Zusatzinformation", extension.getValue().toString());
  }

  @Test
  void testHospitalizationEncounterWithHospitalizationEndDate() throws JsonProcessingException {
    Hospitalization hospitalization =
        FileUtils.createHospitalization(
            "portal/disease/encounter/notification_content_hospitalization_limited.json");

    final Optional<Encounter> hospitalizationEncounterOptional =
        creationService.createHospitalizationEncounter(
            hospitalization.getDisease().getDiseaseInfoCommon(), new Patient(), new Organization());

    assertTrue(hospitalizationEncounterOptional.isPresent());
    Encounter hospitalizationEncounter = hospitalizationEncounterOptional.get();
    assertTrue(hospitalizationEncounter.hasPeriod());
    assertNotNull(hospitalizationEncounter.getPeriod().getEnd());
  }
}
