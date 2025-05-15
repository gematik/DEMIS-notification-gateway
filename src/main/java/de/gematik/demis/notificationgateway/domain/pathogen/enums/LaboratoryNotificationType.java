package de.gematik.demis.notificationgateway.domain.pathogen.enums;

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

/**
 * Enumeration representing the types of laboratory notifications.
 *
 * <p>This enum is used to categorize different types of notifications that can be sent by
 * laboratories.
 */
public enum LaboratoryNotificationType {

  /** Represents a standard laboratory notification. */
  LAB,

  /**
   * Represents a non-nominal laboratory notification. This type of notification does not include
   * personal identifiers when delivered to a health office or other receivers. The personal
   * identifiers are removed while processing the notification in the DEMIS core system.
   */
  NON_NOMINAL,

  /**
   * Represents an anonymous laboratory notification. This type of notification is completely
   * anonymized.
   */
  ANONYMOUS
}
