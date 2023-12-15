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

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.PROFILE_IMMUNIZATION_INFORMATION_CVDD;
import static de.gematik.demis.notificationgateway.common.dto.VaccinationInfo.VaccineEnum.INDETERMINATE;
import static de.gematik.demis.notificationgateway.common.dto.VaccinationInfo.VaccineEnum.UNKNOWN;

import de.gematik.demis.notificationgateway.common.dto.VaccinationInfo;
import de.gematik.demis.notificationgateway.common.exceptions.BadRequestException;
import de.gematik.demis.notificationgateway.common.utils.DateUtils;
import de.gematik.demis.notificationgateway.common.utils.ReferenceUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.util.Strings;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Immunization.ImmunizationStatus;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class ImmunizationCreator {

  public List<Immunization> create(
      @Nullable List<VaccinationInfo> vaccinations, Reference notifiedPerson)
      throws BadRequestException {
    List<Immunization> immunizations = new ArrayList<>();
    if (vaccinations == null || vaccinations.isEmpty()) {
      return immunizations;
    }
    for (VaccinationInfo vaccination : vaccinations) {
      final Immunization immunization = new Immunization();
      if (ignoreVaccine(vaccination)) {
        continue;
      }
      immunization
          .setId(UUID.randomUUID().toString())
          .setMeta(new Meta().addProfile(PROFILE_IMMUNIZATION_INFORMATION_CVDD));
      immunization
          .setStatus(ImmunizationStatus.COMPLETED)
          .setVaccineCode(ReferenceUtils.createCodeableConcept(vaccination.getVaccine()))
          .setPatient(notifiedPerson)
          .setOccurrence(DateUtils.createDateTimeType(vaccination))
          .addNote(ReferenceUtils.createAnnotation(vaccination.getAdditionalInfo()));

      immunizations.add(immunization);
    }
    return immunizations;
  }

  private static boolean ignoreVaccine(VaccinationInfo vaccination) {
    final VaccinationInfo.VaccineEnum vaccine = vaccination.getVaccine();
    final String vaccinationDate = vaccination.getVaccinationDate();
    return Strings.isBlank(vaccinationDate)
        && (UNKNOWN.equals(vaccine) || INDETERMINATE.equals(vaccine));
  }
}
