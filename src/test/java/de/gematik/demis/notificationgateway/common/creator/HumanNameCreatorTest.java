package de.gematik.demis.notificationgateway.common.creator;

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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.HumanNameDataBuilder;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPersonBasicInfo;
import de.gematik.demis.notificationgateway.common.dto.PractitionerInfo;
import java.util.Optional;
import org.hl7.fhir.r4.model.HumanName;
import org.junit.jupiter.api.Test;

class HumanNameCreatorTest {

  @Test
  void createHumanNameShouldReturnHumanNameWithAllFieldsSet() {
    PractitionerInfo practitionerInfo = new PractitionerInfo();
    practitionerInfo.setFirstname("John");
    practitionerInfo.setLastname("Doe");
    practitionerInfo.setPrefix("Dr.");
    practitionerInfo.setSalutation(PractitionerInfo.SalutationEnum.MR);

    HumanName humanName = HumanNameCreator.createHumanName(practitionerInfo);

    assertThat(humanName.getGivenAsSingleString()).isEqualTo("John");
    assertThat(humanName.getFamily()).isEqualTo("Doe");
    assertThat(humanName.getPrefixAsSingleString()).isEqualTo("Dr.");
    assertThat(humanName.getText()).contains("Herr");
  }

  @Test
  void createHumanNameShouldHandleNullSalutationGracefully() {
    PractitionerInfo practitionerInfo = new PractitionerInfo();
    practitionerInfo.setFirstname("Jane");
    practitionerInfo.setLastname("Smith");
    practitionerInfo.setPrefix("Prof.");

    HumanName humanName = HumanNameCreator.createHumanName(practitionerInfo);

    assertThat(humanName.getGivenAsSingleString()).isEqualTo("Jane");
    assertThat(humanName.getFamily()).isEqualTo("Smith");
    assertThat(humanName.getPrefixAsSingleString()).isEqualTo("Prof.");
    assertThat(humanName.getText()).doesNotContain("Herr").doesNotContain("Frau");
  }

  @Test
  void findSalutationShouldReturnCorrectSalutationForMr() {
    PractitionerInfo practitionerInfo = new PractitionerInfo();
    practitionerInfo.setSalutation(PractitionerInfo.SalutationEnum.MR);

    Optional<HumanNameDataBuilder.Salutation> salutation =
        HumanNameCreator.findSalutation(practitionerInfo);

    assertThat(salutation).isPresent().contains(HumanNameDataBuilder.Salutation.MR);
  }

  @Test
  void findSalutationShouldReturnCorrectSalutationForMrs() {
    PractitionerInfo practitionerInfo = new PractitionerInfo();
    practitionerInfo.setSalutation(PractitionerInfo.SalutationEnum.MRS);

    Optional<HumanNameDataBuilder.Salutation> salutation =
        HumanNameCreator.findSalutation(practitionerInfo);

    assertThat(salutation).isPresent().contains(HumanNameDataBuilder.Salutation.MRS);
  }

  @Test
  void findSalutationShouldReturnEmptyOptionalForNullSalutation() {
    PractitionerInfo practitionerInfo = new PractitionerInfo();

    Optional<HumanNameDataBuilder.Salutation> salutation =
        HumanNameCreator.findSalutation(practitionerInfo);

    assertThat(salutation).isEmpty();
  }

  @Test
  void createHumanNameShouldReturnHumanNameWithGivenAndFamilyName() {
    NotifiedPersonBasicInfo personInfo = new NotifiedPersonBasicInfo();
    personInfo.setFirstname("Alice");
    personInfo.setLastname("Johnson");

    HumanName humanName = HumanNameCreator.createHumanName(personInfo);

    assertThat(humanName.getGivenAsSingleString()).isEqualTo("Alice");
    assertThat(humanName.getFamily()).isEqualTo("Johnson");
  }

  @Test
  void createHumanNameShouldHandleNullFirstnameGracefully() {
    NotifiedPersonBasicInfo personInfo = new NotifiedPersonBasicInfo();
    personInfo.setLastname("Johnson");

    HumanName humanName = HumanNameCreator.createHumanName(personInfo);

    assertThat(humanName.getGiven()).isEmpty();
    assertThat(humanName.getFamily()).isEqualTo("Johnson");
  }

  @Test
  void createHumanNameShouldHandleNullLastnameGracefully() {
    NotifiedPersonBasicInfo personInfo = new NotifiedPersonBasicInfo();
    personInfo.setFirstname("Alice");

    HumanName humanName = HumanNameCreator.createHumanName(personInfo);

    assertThat(humanName.getGivenAsSingleString()).isEqualTo("Alice");
    assertThat(humanName.getFamily()).isNull();
  }

  @Test
  void createHumanNameShouldHandleNullPersonInfoGracefully() {
    assertThatThrownBy(() -> HumanNameCreator.createHumanName((NotifiedPersonBasicInfo) null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("PersonInfo cannot be null");
  }
}
