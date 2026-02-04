package de.gematik.demis.notificationgateway.common.creator;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import de.gematik.demis.notificationgateway.common.dto.ContactPointInfo;
import de.gematik.demis.notificationgateway.common.dto.ContactPointInfo.UsageEnum;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.ContactPoint;
import org.junit.jupiter.api.Test;

class ContactPointCreatorTest {

  @Test
  void createsContactPointWithValidData() {
    ContactPointInfo contactPointInfo = mock(ContactPointInfo.class);
    when(contactPointInfo.getContactType()).thenReturn(ContactPointInfo.ContactTypeEnum.PHONE);
    when(contactPointInfo.getValue()).thenReturn("123456789");
    when(contactPointInfo.getUsage()).thenReturn(UsageEnum.HOME);

    ContactPoint result = ContactPointCreator.createContactPoint(contactPointInfo);

    assertThat(result.getSystem()).isEqualTo(ContactPoint.ContactPointSystem.PHONE);
    assertThat(result.getValue()).isEqualTo("123456789");
    assertThat(result.getUse()).isEqualTo(ContactPoint.ContactPointUse.HOME);
  }

  @Test
  void handlesNullUsageGracefully() {
    ContactPointInfo contactPointInfo = mock(ContactPointInfo.class);
    when(contactPointInfo.getContactType()).thenReturn(ContactPointInfo.ContactTypeEnum.EMAIL);
    when(contactPointInfo.getValue()).thenReturn("test@example.com");
    when(contactPointInfo.getUsage()).thenReturn(null);

    ContactPoint result = ContactPointCreator.createContactPoint(contactPointInfo);

    assertThat(result.getSystem()).isEqualTo(ContactPoint.ContactPointSystem.EMAIL);
    assertThat(result.getValue()).isEqualTo("test@example.com");
    assertThat(result.getUse()).isNull();
  }

  @Test
  void throwsExceptionForInvalidContactType() {
    ContactPointInfo contactPointInfo = mock(ContactPointInfo.class);
    ContactPointInfo.ContactTypeEnum contactPointMock =
        mock(ContactPointInfo.ContactTypeEnum.class);
    when(contactPointInfo.getContactType()).thenReturn(contactPointMock);
    when(contactPointInfo.getValue()).thenReturn("123456789");
    when(contactPointInfo.getContactType().getValue()).thenReturn("invalid");

    assertThatThrownBy(() -> ContactPointCreator.createContactPoint(contactPointInfo))
        .isInstanceOf(FHIRException.class)
        .hasMessageContaining("Unknown ContactPointSystem code 'invalid'");
  }
}
