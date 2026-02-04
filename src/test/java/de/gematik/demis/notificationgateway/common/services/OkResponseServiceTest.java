package de.gematik.demis.notificationgateway.common.services;

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

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import de.gematik.demis.notificationgateway.BaseTestUtils;
import de.gematik.demis.notificationgateway.common.dto.OkResponse;
import java.io.IOException;
import java.io.InputStream;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.Test;

class OkResponseServiceTest implements BaseTestUtils {

  @Test
  void shouldCreateNotificationResponseWithJsonResponseFromNotificationEntryService()
      throws IOException {
    IParser parser = FhirContext.forR4().newJsonParser();
    InputStream input = getClass().getResourceAsStream("/nes/nes_response_OK.json");
    Parameters parameters = parser.parseResource(Parameters.class, input);
    OkResponseService responseService = new OkResponseService();

    OkResponse response =
        responseService.addOperationOutcomeInformation(new OkResponse(), parameters);

    assertThat(response)
        .isNotNull()
        .hasNoNullFieldsOrProperties()
        .hasFieldOrPropertyWithValue("timestamp", "07.07.2023 13:50:55")
        .hasFieldOrPropertyWithValue("status", "All OK")
        .hasFieldOrPropertyWithValue("title", "Meldevorgangsquittung")
        .hasFieldOrPropertyWithValue("notificationId", "7f562b87-f2c2-4e9d-b3fc-37f6b5dca3a5")
        .hasFieldOrPropertyWithValue("authorName", "DEMIS")
        .hasFieldOrPropertyWithValue("authorEmail", "demis@rki.de")
        .hasFieldOrPropertyWithValue("contentType", "application/pdf")
        .extracting("content")
        .isNotNull()
        .isInstanceOf(byte[].class);

    String pdfText = pdfToText(response.getContent());
    assertThat(pdfText).isNotBlank().contains("Meldungs-ID " + response.getNotificationId());
  }

  @Test
  void givenValidResponseAndCompositionHasValueIdentifierWhenAddOutcomeThenPDFHasIdFromIdentifier()
      throws IOException {
    IParser jsonParser = FhirContext.forR4().newJsonParser();
    InputStream input = getClass().getResourceAsStream("/rps_response_with_valueIdentifier.json");
    Parameters parameters = jsonParser.parseResource(Parameters.class, input);
    OkResponseService responseService = new OkResponseService();

    OkResponse response =
        responseService.addOperationOutcomeInformation(new OkResponse(), parameters);

    assertThat(response)
        .isNotNull()
        .hasNoNullFieldsOrProperties()
        .hasFieldOrPropertyWithValue("timestamp", "07.07.2023 12:50:39")
        .hasFieldOrPropertyWithValue("status", "All OK")
        .hasFieldOrPropertyWithValue("title", "Meldevorgangsquittung")
        .hasFieldOrPropertyWithValue("notificationId", "e7d64c78-f2ba-44d6-80b5-5ccd864169e2")
        .hasFieldOrPropertyWithValue("authorName", "DEMIS")
        .hasFieldOrPropertyWithValue("authorEmail", "demis@rki.de")
        .hasFieldOrPropertyWithValue("contentType", "application/pdf")
        .extracting("content")
        .isNotNull()
        .isInstanceOf(byte[].class);

    String pdfText = pdfToText(response.getContent());
    assertThat(pdfText).isNotBlank().contains("Meldungs-ID " + response.getNotificationId());
  }

  @Test
  void givenResponseAndCompositionHasNoValueIdentifierWhenAddOutcomeThenPDFHasIdFromComposition() {
    IParser jsonParser = FhirContext.forR4().newJsonParser();
    InputStream input =
        getClass().getResourceAsStream("/rps_response_without_ValueIdentifier.json");
    Parameters parameters = jsonParser.parseResource(Parameters.class, input);
    OkResponseService responseService = new OkResponseService();

    OkResponse response =
        responseService.addOperationOutcomeInformation(new OkResponse(), parameters);

    assertThat(response)
        .isNotNull()
        .hasNoNullFieldsOrPropertiesExcept("content")
        .hasFieldOrPropertyWithValue("timestamp", "09.09.2022 10:47:09")
        .hasFieldOrPropertyWithValue("status", "All OK")
        .hasFieldOrPropertyWithValue("title", "Meldevorgangsquittung")
        .hasFieldOrPropertyWithValue("notificationId", "")
        .hasFieldOrPropertyWithValue("authorName", "DEMIS")
        .hasFieldOrPropertyWithValue("authorEmail", "demis@rki.de")
        .hasFieldOrPropertyWithValue("contentType", "application/pdf");
  }

  @Test
  void givenResponseAndNoCompositionIdAndNoValueIdentifierWhenAddOutcomeThenPDFHasNoId() {
    IParser jsonParser = FhirContext.forR4().newJsonParser();
    InputStream input =
        getClass()
            .getResourceAsStream(
                "/rps_response_without_ValueIdentifier_without_compositionId.json");
    Parameters parameters = jsonParser.parseResource(Parameters.class, input);
    OkResponseService responseService = new OkResponseService();

    OkResponse response =
        responseService.addOperationOutcomeInformation(new OkResponse(), parameters);

    assertThat(response)
        .isNotNull()
        .hasNoNullFieldsOrPropertiesExcept("content")
        .hasFieldOrPropertyWithValue("timestamp", "09.09.2022 10:47:09")
        .hasFieldOrPropertyWithValue("status", "All OK")
        .hasFieldOrPropertyWithValue("title", "Meldevorgangsquittung")
        .hasFieldOrPropertyWithValue("notificationId", "")
        .hasFieldOrPropertyWithValue("authorName", "DEMIS")
        .hasFieldOrPropertyWithValue("authorEmail", "demis@rki.de")
        .hasFieldOrPropertyWithValue("contentType", "application/pdf");
  }
}
