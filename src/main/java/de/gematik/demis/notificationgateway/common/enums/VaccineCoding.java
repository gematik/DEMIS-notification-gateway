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

package de.gematik.demis.notificationgateway.common.enums;

import static de.gematik.demis.notificationgateway.common.constants.FhirConstants.COMMUNITY_REGISTER;
import static de.gematik.demis.notificationgateway.common.dto.VaccinationInfo.VaccineEnum.COMIRNATY;
import static de.gematik.demis.notificationgateway.common.dto.VaccinationInfo.VaccineEnum.INDETERMINATE;
import static de.gematik.demis.notificationgateway.common.dto.VaccinationInfo.VaccineEnum.JANSSEN;
import static de.gematik.demis.notificationgateway.common.dto.VaccinationInfo.VaccineEnum.MODERNA;
import static de.gematik.demis.notificationgateway.common.dto.VaccinationInfo.VaccineEnum.NUVAXOVID;
import static de.gematik.demis.notificationgateway.common.dto.VaccinationInfo.VaccineEnum.OTHER;
import static de.gematik.demis.notificationgateway.common.dto.VaccinationInfo.VaccineEnum.UNKNOWN;
import static de.gematik.demis.notificationgateway.common.dto.VaccinationInfo.VaccineEnum.VALNEVA;
import static de.gematik.demis.notificationgateway.common.dto.VaccinationInfo.VaccineEnum.VAXZEVRIA;

import de.gematik.demis.notificationgateway.common.dto.VaccinationInfo;
import de.gematik.demis.notificationgateway.common.exceptions.BadRequestException;
import de.gematik.demis.notificationgateway.common.utils.ConfiguredCodeSystems;
import java.util.Arrays;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;

@Getter
public enum VaccineCoding {
  MODERNA_CODING(
      MODERNA,
      new Coding()
          .setSystem(COMMUNITY_REGISTER)
          .setCode("EU/1/20/1507")
          .setDisplay("Spikevax (COVID-19 Vaccine Moderna)")),
  JANSSEN_CODING(
      JANSSEN,
      new Coding()
          .setSystem(COMMUNITY_REGISTER)
          .setCode("EU/1/20/1525")
          .setDisplay("COVID-19 Vaccine Janssen")),
  COMIRNATY_CODING(
      COMIRNATY,
      new Coding().setSystem(COMMUNITY_REGISTER).setCode("EU/1/20/1528").setDisplay("Comirnaty")),
  VAXZEVRIA_CODING(
      VAXZEVRIA,
      new Coding()
          .setSystem(COMMUNITY_REGISTER)
          .setCode("EU/1/21/1529")
          .setDisplay("Vaxzevria (COVID-19 Vaccine AstraZeneca)")),
  NUVAXOVID_CODING(
      NUVAXOVID,
      new Coding()
          .setSystem(COMMUNITY_REGISTER)
          .setCode("EU/1/21/1618")
          .setDisplay("Nuvaxovid (NVX-CoV2373)")),
  VALNEVA_CODING(
      VALNEVA,
      new Coding()
          .setSystem(COMMUNITY_REGISTER)
          .setCode("EU/1/21/1624")
          .setDisplay("COVID-19 Vaccine (inactivated, adjuvanted) Valneva")),
  OTHER_CODING(OTHER, ConfiguredCodeSystems.getInstance().getVaccineCoding("otherVaccine")),
  INDETERMINATE_CODING(INDETERMINATE, ConfiguredCodeSystems.getInstance().getNullFlavor("ASKU")),
  UNKNOWN_CODING(UNKNOWN, ConfiguredCodeSystems.getInstance().getNullFlavor("NASK"));

  private final VaccinationInfo.VaccineEnum vaccine;

  private final Coding coding;

  VaccineCoding(VaccinationInfo.VaccineEnum vaccine, Coding coding) {
    this.vaccine = vaccine;
    this.coding = coding;
  }

  public static VaccineCoding byVaccine(VaccinationInfo.VaccineEnum vaccine)
      throws BadRequestException {
    return Arrays.stream(values())
        .filter(vaccineCode -> vaccineCode.vaccine == vaccine)
        .findFirst()
        .orElseThrow(() -> new BadRequestException("unknown vaccine enum: " + vaccine));
  }
}
