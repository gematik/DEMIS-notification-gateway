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

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.PROFILE_NOTIFICATION_DISEASE;

import de.gematik.demis.notificationgateway.common.services.fhir.CompositionCreationService;
import de.gematik.demis.notificationgateway.common.utils.ConfiguredCodeSystems;
import de.gematik.demis.notificationgateway.common.utils.ReferenceUtils;
import java.util.Date;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Composition.SectionComponent;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Service;

@Service
public class NotificationDiseaseCreationService extends CompositionCreationService {

  public Composition createNotificationDisease(
      Patient notifiedPerson,
      PractitionerRole notifierRole,
      Condition condition,
      QuestionnaireResponse diseaseInformationCommon,
      QuestionnaireResponse diseaseInformationCVDD) {
    final String title = "Meldung gemäß §6 Absatz 1, 2 IfSG";
    final Coding categoryCoding =
        ConfiguredCodeSystems.getInstance().getNotificationTypeCoding("6.1_2");
    final Composition notificationDisease =
        createComposition(
            PROFILE_NOTIFICATION_DISEASE, title, categoryCoding, notifiedPerson, notifierRole);

    notificationDisease.setDate(new Date());

    final ConfiguredCodeSystems codeSystems = ConfiguredCodeSystems.getInstance();
    addSection(notificationDisease, condition, codeSystems.getSectionCodeCoding("diagnosis"));
    addSection(
        notificationDisease,
        diseaseInformationCommon,
        codeSystems.getSectionCodeCoding("generalClinAndEpiInformation"));
    addSection(
        notificationDisease,
        diseaseInformationCVDD,
        codeSystems.getSectionCodeCoding("specificClinAndEpiInformation"));

    return notificationDisease;
  }

  private void addSection(Composition composition, Resource resource, Coding sectionCodeCoding) {
    if (resource == null) {
      return;
    }
    final SectionComponent sectionComponent = composition.addSection();
    sectionComponent.setTitle(sectionCodeCoding.getDisplay());
    sectionComponent.setCode(new CodeableConcept(sectionCodeCoding));
    sectionComponent.addEntry(ReferenceUtils.createReference(resource));
  }
}
