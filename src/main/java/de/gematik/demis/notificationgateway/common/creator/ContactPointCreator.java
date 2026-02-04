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

import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.TelecomDataBuilder;
import de.gematik.demis.notificationgateway.common.dto.ContactPointInfo;
import org.hl7.fhir.r4.model.ContactPoint;

/**
 * Utility class for creating FHIR ContactPoint objects.
 *
 * <p>This class provides a method to create a {@link ContactPoint} instance based on the provided
 * {@link ContactPointInfo} data.
 */
public class ContactPointCreator {

  /**
   * Private constructor to prevent instantiation of this utility class.
   *
   * <p>This class is designed as a utility class and should not be instantiated.
   */
  private ContactPointCreator() {}

  /**
   * Creates a FHIR {@link ContactPoint} object using the provided {@link ContactPointInfo}.
   *
   * @param contactPointInfo The information required to create the ContactPoint, including contact
   *     type, value, and usage.
   * @return A {@link ContactPoint} object populated with the provided data.
   */
  public static ContactPoint createContactPoint(ContactPointInfo contactPointInfo) {
    final ContactPointInfo.UsageEnum usage = contactPointInfo.getUsage();
    return new TelecomDataBuilder()
        .setSystem(
            ContactPoint.ContactPointSystem.fromCode(contactPointInfo.getContactType().getValue()))
        .setValue(contactPointInfo.getValue())
        .setUse(usage == null ? null : ContactPoint.ContactPointUse.fromCode(usage.getValue()))
        .build();
  }
}
