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

package de.gematik.demis.notificationgateway.common.utils;

import de.gematik.demis.notificationgateway.common.dto.BedOccupancy;
import de.gematik.demis.notificationgateway.common.dto.Hospitalization;
import de.gematik.demis.notificationgateway.common.dto.PathogenTest;
import de.gematik.demis.notificationgateway.common.dto.QuickTest;
import java.util.Arrays;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
public class HoneypotUtils {

  public boolean isQuickTestSpammer(QuickTest quicktest) {
    final String oneTimeCodeFacility = quicktest.getNotifierFacility().getOneTimeCode();
    final String oneTimeCodePerson = quicktest.getNotifiedPerson().getOneTimeCode();
    final String oneTimeCodeDiagnostic = quicktest.getDiagnostic().getOneTimeCode();

    return checkIfStringIsNotBlack(oneTimeCodeFacility, oneTimeCodePerson, oneTimeCodeDiagnostic);
  }

  public boolean isHospitalizationSpammer(Hospitalization hospitalization) {
    final String oneTimeCodeFacility = hospitalization.getNotifierFacility().getOneTimeCode();
    final String oneTimeCodePerson = hospitalization.getNotifiedPerson().getOneTimeCode();
    final String oneTimeCodeDisease =
        hospitalization.getDisease().getConditionInfo().getOneTimeCode();
    final String oneTimeCodeCommon =
        hospitalization.getDisease().getDiseaseInfoCommon().getOneTimeCode();
    final String oneTimeCodeCVVD =
        hospitalization.getDisease().getDiseaseInfoCVDD().getOneTimeCode();

    return checkIfStringIsNotBlack(
        oneTimeCodeFacility,
        oneTimeCodePerson,
        oneTimeCodeDisease,
        oneTimeCodeCommon,
        oneTimeCodeCVVD);
  }

  public boolean isBedOccupancySpammer(BedOccupancy bedOccupancy) {
    final String oneTimeCodeFacility = bedOccupancy.getNotifierFacility().getOneTimeCode();
    final String oneTimeCodeQuestion = bedOccupancy.getBedOccupancyQuestion().getOneTimeCode();

    return checkIfStringIsNotBlack(oneTimeCodeFacility, oneTimeCodeQuestion);
  }

  public boolean isPathogenSpammer(PathogenTest pathogenTest) {
    final String oneTimeCodeFacility = pathogenTest.getNotifierFacility().getOneTimeCode();
    final String oneTimeCodePerson = pathogenTest.getNotifiedPerson().getOneTimeCode();
    final String oneTimeCodePathogen = pathogenTest.getPathogenDTO().getOneTimeCode();
    return checkIfStringIsNotBlack(oneTimeCodeFacility, oneTimeCodePerson, oneTimeCodePathogen);
  }

  private boolean checkIfStringIsNotBlack(String... a) {
    return Arrays.stream(a).anyMatch(StringUtils::isNotBlank);
  }
}
