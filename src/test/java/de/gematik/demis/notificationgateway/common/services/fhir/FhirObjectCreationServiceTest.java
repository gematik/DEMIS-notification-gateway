package de.gematik.demis.notificationgateway.common.services.fhir;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.gematik.demis.notificationgateway.FeatureFlags;
import de.gematik.demis.notificationgateway.common.constants.FhirConstants;
import de.gematik.demis.notificationgateway.common.dto.ContactPointInfo;
import de.gematik.demis.notificationgateway.common.dto.QuickTest;
import de.gematik.demis.notificationgateway.domain.pathogen.fhir.PathogenBundleCreationService;
import de.gematik.demis.notificationgateway.utils.FileUtils;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class FhirObjectCreationServiceTest {
  @Mock private FeatureFlags featureFlags;

  private PathogenBundleCreationService pathogenBundleCreationService;
  private FhirObjectCreationService creationService;

  @BeforeEach
  void setUp() {
    featureFlags = mock(FeatureFlags.class);
    when(featureFlags.isNotifications73()).thenReturn(false);

    pathogenBundleCreationService = new PathogenBundleCreationService(this.featureFlags);
    creationService = new FhirObjectCreationService(featureFlags);
  }

  @Test
  void testCreateParameters() {
    final Bundle bundle = new Bundle();
    Identifier identifier =
        new Identifier()
            .setSystem(FhirConstants.NAMING_SYSTEM_NOTIFICATION_BUNDLE_ID)
            .setValue(UUID.randomUUID().toString());
    bundle.setIdentifier(identifier);

    final Parameters parameters = creationService.createParameters(bundle);
    assertNotNull(parameters);

    assertTrue(parameters.hasParameter());
    final List<ParametersParameterComponent> parameterList = parameters.getParameter();
    assertEquals(1, parameterList.size());

    final ParametersParameterComponent parameter = parameterList.get(0);
    assertTrue(parameter.hasName());
    assertEquals("content", parameter.getName());
    assertTrue(parameter.hasResource());
    assertEquals(bundle, parameter.getResource());
  }

  @Test
  void testCreateCurrentAddressWithoutLine() throws JsonProcessingException {
    final QuickTest quickTest =
        FileUtils.createQuickTest("portal/laboratory/notification_content_min.json");

    final Address address =
        creationService.createAddress(quickTest.getNotifiedPerson().getCurrentAddress(), true);

    final List<Extension> addressExtensions = address.getExtension();
    assertEquals(1, addressExtensions.size());
    final Extension addressExtension = addressExtensions.get(0);
    assertEquals(FhirConstants.STRUCTURE_DEFINITION_ADDRESS_USE, addressExtension.getUrl());
    final Coding value = (Coding) addressExtension.getValue();
    assertEquals(FhirConstants.CODE_SYSTEM_ADDRESS_USE, value.getSystem());
    assertEquals("current", value.getCode());
    assertEquals("Derzeitiger Aufenthaltsort", value.getDisplay());

    assertFalse(address.hasLine());
    assertEquals("Buchhorst", address.getCity());
    assertEquals("DE", address.getCountry());
    assertEquals("21481", address.getPostalCode());
  }

  @Test
  void testCreateCompleteCurrentAddress() throws JsonProcessingException {
    final QuickTest quickTest =
        FileUtils.createQuickTest("portal/laboratory/notification_content_max.json");

    final Address address =
        creationService.createAddress(quickTest.getNotifiedPerson().getCurrentAddress(), true);

    final List<Extension> addressExtensions = address.getExtension();
    assertEquals(1, addressExtensions.size());
    final Extension addressExtension = addressExtensions.get(0);
    assertEquals(FhirConstants.STRUCTURE_DEFINITION_ADDRESS_USE, addressExtension.getUrl());
    final Coding addressExtensionValue = (Coding) addressExtension.getValue();
    assertEquals(FhirConstants.CODE_SYSTEM_ADDRESS_USE, addressExtensionValue.getSystem());
    assertEquals("current", addressExtensionValue.getCode());
    assertEquals("Derzeitiger Aufenthaltsort", addressExtensionValue.getDisplay());

    final StringType addressLine = address.getLine().get(0);
    assertEquals("Betroffenenstraße 1", addressLine.asStringValue());
    final List<Extension> addressLineExtensions = addressLine.getExtension();
    assertEquals(2, addressLineExtensions.size());
    final Extension streetNameExtension = addressLineExtensions.get(0);
    assertEquals(FhirConstants.STRUCTURE_DEFINITION_ADXP_STREET_NAME, streetNameExtension.getUrl());
    assertEquals("Betroffenenstraße", streetNameExtension.getValue().toString());
    final Extension houseNumberExtension = addressLineExtensions.get(1);
    assertEquals(
        FhirConstants.STRUCTURE_DEFINITION_ADXP_HOUSE_NUMBER, houseNumberExtension.getUrl());
    assertEquals("1", houseNumberExtension.getValue().toString());

    assertEquals("21481", address.getPostalCode());
    assertEquals("Buchhorst", address.getCity());
    assertEquals("DE", address.getCountry());
  }

  @Test
  void testCreateCompleteResidenceAddress() throws JsonProcessingException {
    final QuickTest quickTest =
        FileUtils.createQuickTest("portal/laboratory/notification_content_max.json");

    final Address address =
        creationService.createAddress(quickTest.getNotifiedPerson().getResidenceAddress(), true);

    final List<Extension> addressExtensions = address.getExtension();
    assertEquals(1, addressExtensions.size());
    final Extension addressExtension = addressExtensions.get(0);
    assertEquals(FhirConstants.STRUCTURE_DEFINITION_ADDRESS_USE, addressExtension.getUrl());
    final Coding addressExtensionValue = (Coding) addressExtension.getValue();
    assertEquals(FhirConstants.CODE_SYSTEM_ADDRESS_USE, addressExtensionValue.getSystem());
    assertEquals("primary", addressExtensionValue.getCode());
    assertEquals("Hauptwohnsitz", addressExtensionValue.getDisplay());

    final StringType addressLine = address.getLine().get(0);
    assertEquals("Andere Straße 3", addressLine.asStringValue());
    final List<Extension> addressLineExtensions = addressLine.getExtension();
    assertEquals(2, addressLineExtensions.size());
    final Extension streetNameExtension = addressLineExtensions.get(0);
    assertEquals(FhirConstants.STRUCTURE_DEFINITION_ADXP_STREET_NAME, streetNameExtension.getUrl());
    assertEquals("Andere Straße", streetNameExtension.getValue().toString());
    final Extension houseNumberExtension = addressLineExtensions.get(1);
    assertEquals(
        FhirConstants.STRUCTURE_DEFINITION_ADXP_HOUSE_NUMBER, houseNumberExtension.getUrl());
    assertEquals("3", houseNumberExtension.getValue().toString());

    assertEquals("11223", address.getPostalCode());
    assertEquals("Stadt", address.getCity());
    assertEquals("DE", address.getCountry());
  }

  @Test
  void testCreateContactPoint() throws JsonProcessingException {
    final QuickTest quickTest =
        FileUtils.createQuickTest("portal/laboratory/notification_content_max.json");

    final List<ContactPointInfo> contacts = quickTest.getNotifiedPerson().getContacts();
    final ContactPoint phoneContactPoint = creationService.createContactPoint(contacts.get(0));
    assertEquals("phone", phoneContactPoint.getSystem().toCode());
    assertEquals("01234567", phoneContactPoint.getValue());

    final ContactPoint emailContactPoint = creationService.createContactPoint(contacts.get(1));
    assertEquals("email", emailContactPoint.getSystem().toCode());
    assertEquals("bertha@betroffen.de", emailContactPoint.getValue());
  }
}
