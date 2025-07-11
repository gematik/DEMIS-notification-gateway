package de.gematik.demis.notificationgateway.domain.disease.fhir;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.PractitionerRoleBuilder;
import de.gematik.demis.notificationgateway.common.constants.FhirConstants;
import de.gematik.demis.notificationgateway.common.dto.QuickTest;
import de.gematik.demis.notificationgateway.common.services.fhir.FhirObjectCreationService;
import de.gematik.demis.notificationgateway.utils.FileUtils;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.HumanName.NameUse;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotifiedPersonCreationServiceTest {

  private final FhirObjectCreationService fhirObjectCreationService =
      new FhirObjectCreationService(false);
  private final OrganizationCreationService organizationCreationService =
      new OrganizationCreationService(fhirObjectCreationService, false);
  private final NotifiedPersonCreationService notifiedPersonCreationService =
      new NotifiedPersonCreationService(fhirObjectCreationService, organizationCreationService);

  @Test
  void testCreateNotifiedPersonWithMinimumInput() throws JsonProcessingException {
    final QuickTest quickTest =
        FileUtils.createQuickTest("portal/laboratory/notification_content_min.json");

    final PractitionerRole submitter = new PractitionerRoleBuilder().setDefaults().build();

    final Patient notifiedPerson =
        notifiedPersonCreationService.createPatient(quickTest.getNotifiedPerson(), submitter);
    Assertions.assertNotNull(notifiedPerson);

    assertTrue(notifiedPerson.hasId());

    final Meta meta = notifiedPerson.getMeta();
    assertEquals(
        FhirConstants.PROFILE_NOTIFIED_PERSON, meta.getProfile().getFirst().asStringValue());

    final List<HumanName> names = notifiedPerson.getName();
    assertEquals(1, names.size());
    final HumanName name = names.getFirst();
    assertEquals(NameUse.OFFICIAL, name.getUse());
    assertEquals("Betroffen", name.getFamily());
    assertEquals("Bertha", name.getGiven().getFirst().asStringValue());

    assertFalse(notifiedPerson.hasTelecom());

    assertEquals("FEMALE", notifiedPerson.getGender().toString());

    Assertions.assertNull(notifiedPerson.getBirthDate());

    final List<Address> addresses = notifiedPerson.getAddress();
    assertThat(addresses).hasSize(2);
    final Address currentAddress = addresses.getFirst();
    assertThat(currentAddress.getCity()).isEqualTo("Buchhorst");
    assertThat(currentAddress.getPostalCode()).isEqualTo("21481");
    assertThat(currentAddress.getCountry()).isEqualTo("DE");

    final Address residenceAddress = addresses.get(1);
    assertThat(residenceAddress.getCity()).isEqualTo("residence");
    assertThat(residenceAddress.getPostalCode()).isEqualTo("21481");
    assertThat(residenceAddress.getCountry()).isEqualTo("DE");
  }

  @Test
  void testCreateNotifiedPersonWithMaximumInput() throws JsonProcessingException {
    final QuickTest quickTest =
        FileUtils.createQuickTest("portal/laboratory/notification_content_max.json");

    final PractitionerRole submitter = new PractitionerRoleBuilder().setDefaults().build();
    final Patient notifiedPerson =
        notifiedPersonCreationService.createPatient(quickTest.getNotifiedPerson(), submitter);
    Assertions.assertNotNull(notifiedPerson);

    assertTrue(notifiedPerson.hasId());

    final Meta meta = notifiedPerson.getMeta();
    assertEquals(
        FhirConstants.PROFILE_NOTIFIED_PERSON, meta.getProfile().getFirst().asStringValue());

    final List<HumanName> names = notifiedPerson.getName();
    assertEquals(1, names.size());
    final HumanName name = names.getFirst();
    assertEquals(NameUse.OFFICIAL, name.getUse());
    assertEquals("Betroffen", name.getFamily());
    final List<StringType> givens = name.getGiven();
    assertEquals(3, givens.size());
    assertEquals("Bertha-Luise", givens.getFirst().asStringValue());
    assertEquals("Hanna", givens.get(1).asStringValue());
    assertEquals("Karin", givens.get(2).asStringValue());

    assertTrue(notifiedPerson.hasTelecom());
    final List<ContactPoint> telecom = notifiedPerson.getTelecom();
    assertEquals(2, telecom.size());
    assertEquals("phone", telecom.getFirst().getSystem().toCode());
    assertEquals("01234567", telecom.getFirst().getValue());
    assertEquals("email", telecom.get(1).getSystem().toCode());
    assertEquals("bertha@betroffen.de", telecom.get(1).getValue());

    assertEquals("FEMALE", notifiedPerson.getGender().toString());

    final LocalDate birthdate =
        LocalDate.ofInstant(notifiedPerson.getBirthDate().toInstant(), ZoneId.systemDefault());
    assertEquals(LocalDate.of(1999, 6, 9), birthdate);

    final List<Address> addresses = notifiedPerson.getAddress();
    assertEquals(2, addresses.size());
    final Address currentAddress = addresses.getFirst();
    assertEquals("Betroffenenstraße 1", currentAddress.getLine().getFirst().asStringValue());

    final Address residenceAddress = addresses.getLast();
    assertEquals("Andere Straße 3", residenceAddress.getLine().getFirst().asStringValue());
  }
}
