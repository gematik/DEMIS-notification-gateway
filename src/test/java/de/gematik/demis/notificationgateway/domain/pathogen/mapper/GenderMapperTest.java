package de.gematik.demis.notificationgateway.domain.pathogen.mapper;

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

import static org.junit.jupiter.api.Assertions.*;

import de.gematik.demis.notificationgateway.common.dto.Gender;
import de.gematik.demis.notificationgateway.common.mappers.GenderMapper;
import java.util.Optional;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Extension;
import org.junit.jupiter.api.Test;

class GenderMapperTest {

  @Test
  void testMapGenderMale() {
    assertEquals(Enumerations.AdministrativeGender.MALE, GenderMapper.mapGender(Gender.MALE));
  }

  @Test
  void testMapGenderFemale() {
    assertEquals(Enumerations.AdministrativeGender.FEMALE, GenderMapper.mapGender(Gender.FEMALE));
  }

  @Test
  void testMapGenderDiverse() {
    assertEquals(Enumerations.AdministrativeGender.OTHER, GenderMapper.mapGender(Gender.DIVERSE));
  }

  @Test
  void testMapGenderOtherX() {
    assertEquals(Enumerations.AdministrativeGender.OTHER, GenderMapper.mapGender(Gender.OTHERX));
  }

  @Test
  void testMapGenderUnknown() {
    assertEquals(Enumerations.AdministrativeGender.UNKNOWN, GenderMapper.mapGender(Gender.UNKNOWN));
  }

  @Test
  void testCreateGenderExtensionDiverse() {
    Optional<Extension> ext = GenderMapper.createGenderExtension(Gender.DIVERSE);
    assertTrue(ext.isPresent());
    assertInstanceOf(Coding.class, ext.get().getValue());
    org.hl7.fhir.r4.model.Coding coding = (org.hl7.fhir.r4.model.Coding) ext.get().getValue();
    assertEquals("http://fhir.de/StructureDefinition/gender-amtlich-de", ext.get().getUrl());
    assertEquals("D", coding.getCode());
    assertEquals("divers", coding.getDisplay());
  }

  @Test
  void testCreateGenderExtensionOtherX() {
    Optional<Extension> ext = GenderMapper.createGenderExtension(Gender.OTHERX);
    assertTrue(ext.isPresent());
    assertInstanceOf(Coding.class, ext.get().getValue());
    org.hl7.fhir.r4.model.Coding coding = (org.hl7.fhir.r4.model.Coding) ext.get().getValue();
    assertEquals("X", coding.getCode());
    assertEquals("unbestimmt", coding.getDisplay());
  }

  @Test
  void testCreateGenderExtensionMale() {
    assertTrue(GenderMapper.createGenderExtension(Gender.MALE).isEmpty());
  }

  @Test
  void testCreateGenderExtensionFemale() {
    assertTrue(GenderMapper.createGenderExtension(Gender.FEMALE).isEmpty());
  }

  @Test
  void testCreateGenderExtensionUnknown() {
    assertTrue(GenderMapper.createGenderExtension(Gender.UNKNOWN).isEmpty());
  }
}
