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
 * #L%
 */

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.NAMING_SYSTEM_NOTIFICATION_ID;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.SYSTEM_LOINC;

import de.gematik.demis.notification.builder.demis.fhir.notification.utils.Utils;
import de.gematik.demis.notificationgateway.common.utils.ReferenceUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Composition.CompositionStatus;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;

public abstract class CompositionCreationService {

  protected Composition createComposition(
      String profile,
      String title,
      Coding categoryCoding,
      Patient notifiedPerson,
      PractitionerRole notifierRole) {
    final Composition composition = new Composition();
    composition.setId(Utils.generateUuidString());
    composition.setMeta(new Meta().addProfile(profile));

    addIdentifier(composition);
    composition.setStatus(CompositionStatus.FINAL);
    addType(composition);
    composition.addCategory(new CodeableConcept(categoryCoding));
    composition.setSubject(ReferenceUtils.createReference(notifiedPerson));
    composition.addAuthor(ReferenceUtils.createReference(notifierRole));
    composition.setTitle(title);

    return composition;
  }

  private void addIdentifier(Composition composition) {
    final Identifier notificationID = new Identifier();
    notificationID.setSystem(NAMING_SYSTEM_NOTIFICATION_ID).setValue(Utils.generateUuidString());
    composition.setIdentifier(notificationID);
  }

  private void addType(Composition composition) {
    final Coding typeCoding = new Coding();
    typeCoding.setSystem(SYSTEM_LOINC).setCode("34782-3").setDisplay("Infectious disease Note");
    composition.setType(new CodeableConcept(typeCoding));
  }
}
