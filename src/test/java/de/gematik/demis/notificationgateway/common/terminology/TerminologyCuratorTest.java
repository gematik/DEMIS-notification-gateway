package de.gematik.demis.notificationgateway.common.terminology;

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

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import de.gematik.demis.notificationgateway.common.dto.TerminologyVersion;
import de.gematik.demis.notificationgateway.utils.FileUtils;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TerminologyCuratorTest {

  private static final String SYSTEM = "https://demis.rki.de/fhir/CodeSystem/yesOrNoAnswer";
  private static final String VERSION = "1.2.3";

  private Bundle bundle;

  @BeforeEach
  void readBundleFromFile() {
    final String json =
        FileUtils.loadJsonFromFile("portal/disease/notification-formly-output.json");
    bundle = (Bundle) FhirContext.forR4Cached().newJsonParser().parseResource(json);
  }

  @Test
  void givenEmptyTerminologyVersionsWhenSetCodeSystemVersionsBundleThenDefaultVersionIsSet() {
    verifyPregnancyAnswerCodingVersion(bundle, null);
    new TerminologyCurator(null).setCodeSystemVersions(bundle);
    verifyPregnancyAnswerCodingVersion(bundle, TerminologyVersionsVisitor.VERSION_DEFAULT);
  }

  @Test
  void givenSpecificTerminologyVersionWhenSetCodeSystemVersionsBundleThenThatVersionIsSet() {
    verifyPregnancyAnswerCodingVersion(bundle, null);

    final TerminologyVersion version = new TerminologyVersion();
    version.setSystem(SYSTEM);
    version.setVersion(VERSION);
    new TerminologyCurator(List.of(version)).setCodeSystemVersions(bundle);

    verifyPregnancyAnswerCodingVersion(bundle, VERSION);
  }

  private void verifyPregnancyAnswerCodingVersion(Bundle bundle, String expectedVersion) {
    final List<Bundle.BundleEntryComponent> entries = bundle.getEntry();
    final QuestionnaireResponse msvdQuestionnaire =
        (QuestionnaireResponse) entries.getLast().getResource();
    final QuestionnaireResponse.QuestionnaireResponseItemComponent pregnancyItem =
        msvdQuestionnaire.getItem().get(2);
    final Coding pregnancyAnswer = pregnancyItem.getAnswer().getFirst().getValueCoding();
    assertThat(pregnancyAnswer).isNotNull();
    assertThat(pregnancyAnswer.getSystem()).isEqualTo(SYSTEM);
    assertThat(pregnancyAnswer.getCode()).isEqualTo("yes");
    if (StringUtils.isBlank(expectedVersion)) {
      assertThat(pregnancyAnswer.getVersion()).isNull();
    } else {
      assertThat(pregnancyAnswer.getVersion()).isEqualTo(expectedVersion);
    }
  }
}
