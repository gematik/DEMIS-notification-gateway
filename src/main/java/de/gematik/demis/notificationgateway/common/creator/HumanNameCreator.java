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

import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.HumanNameDataBuilder;
import de.gematik.demis.notificationgateway.common.dto.ContactPointInfo;
import de.gematik.demis.notificationgateway.common.dto.NotifiedPersonBasicInfo;
import de.gematik.demis.notificationgateway.common.dto.PractitionerInfo;
import java.util.Optional;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.HumanName;

/**
 * Creates a FHIR {@link ContactPoint} object using the provided {@link ContactPointInfo}.
 *
 * @param contactPointInfo The information required to create the ContactPoint, including contact
 *     type, value, and usage.
 * @return A {@link ContactPoint} object populated with the provided data.
 */
public class HumanNameCreator {

  /**
   * Private constructor to prevent instantiation of this utility class.
   *
   * <p>This class is designed as a utility class and should not be instantiated.
   */
  private HumanNameCreator() {}

  /**
   * Creates a FHIR {@link HumanName} object using the provided {@link PractitionerInfo}.
   *
   * @param practitionerInfo The information required to create the HumanName, including first name,
   *     last name, and optional prefix or salutation.
   * @return A {@link HumanName} object populated with the provided data.
   */
  public static HumanName createHumanName(final PractitionerInfo practitionerInfo) {
    final HumanNameDataBuilder humanNameDataBuilder =
        new HumanNameDataBuilder()
            .setFamilyName(practitionerInfo.getLastname())
            .addGivenName(practitionerInfo.getFirstname())
            .addPrefix(practitionerInfo.getPrefix());
    findSalutation(practitionerInfo).ifPresent(humanNameDataBuilder::setSalutation);
    return humanNameDataBuilder.build();
  }

  /**
   * Determines the salutation for the given {@link PractitionerInfo}.
   *
   * @param contact The practitioner information containing the salutation enum.
   * @return An {@link Optional} containing the corresponding {@link
   *     HumanNameDataBuilder.Salutation}, or an empty {@link Optional} if no salutation is
   *     provided.
   */
  public static Optional<HumanNameDataBuilder.Salutation> findSalutation(PractitionerInfo contact) {
    final PractitionerInfo.SalutationEnum salutation = contact.getSalutation();
    if (salutation != null) {
      return switch (salutation) {
        case MR -> Optional.of(HumanNameDataBuilder.Salutation.MR);
        case MRS -> Optional.of(HumanNameDataBuilder.Salutation.MRS);
      };
    }
    return Optional.empty();
  }

  /**
   * Creates a FHIR {@link HumanName} object for the notified person.
   *
   * @param personInfo The {@link NotifiedPersonBasicInfo} object containing the person's name
   *     details.
   * @return A {@link HumanName} object populated with the provided data.
   */
  public static HumanName createHumanName(NotifiedPersonBasicInfo personInfo) {
    if (personInfo == null) {
      throw new IllegalArgumentException("PersonInfo cannot be null");
    }
    return new HumanNameDataBuilder()
        .addGivenName(personInfo.getFirstname())
        .setFamilyName(personInfo.getLastname())
        .build();
  }
}
