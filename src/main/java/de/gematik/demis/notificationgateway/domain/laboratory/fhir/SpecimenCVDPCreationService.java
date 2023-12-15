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

package de.gematik.demis.notificationgateway.domain.laboratory.fhir;

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.PROFILE_SPECIMEN_CVDP;
import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.SYSTEM_SNOMED;

import de.gematik.demis.notificationgateway.common.dto.Diagnosis;
import de.gematik.demis.notificationgateway.common.utils.DateUtils;
import de.gematik.demis.notificationgateway.common.utils.ReferenceUtils;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Specimen;
import org.hl7.fhir.r4.model.Specimen.SpecimenCollectionComponent;
import org.hl7.fhir.r4.model.Specimen.SpecimenStatus;
import org.springframework.stereotype.Service;

@Service
public class SpecimenCVDPCreationService {

  public Specimen createSpecimenCVDP(
      @NotNull Diagnosis diagnosticContent,
      @NotNull Patient notifiedPerson,
      @NotNull PractitionerRole submittingRole) {
    Specimen specimen = new Specimen();
    specimen.setId(UUID.randomUUID().toString());
    specimen.setMeta(new Meta().addProfile(PROFILE_SPECIMEN_CVDP));

    specimen.setSubject(ReferenceUtils.createReference(notifiedPerson));
    specimen.setReceivedTime(DateUtils.createDate(diagnosticContent.getReceivedDate()));
    specimen.setCollection(
        new SpecimenCollectionComponent()
            .setCollector(ReferenceUtils.createReference(submittingRole)));

    specimen.setStatus(SpecimenStatus.AVAILABLE);
    addType(specimen);
    return specimen;
  }

  private void addType(Specimen specimen) {
    final Coding typeCoding =
        new Coding()
            .setSystem(SYSTEM_SNOMED)
            .setCode("309164002")
            .setDisplay("Upper respiratory swab sample (specimen)");
    specimen.setType(new CodeableConcept(typeCoding));
  }
}
