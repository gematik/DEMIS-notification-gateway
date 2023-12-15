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

package de.gematik.demis.notificationgateway.domain.laboratory;

import static de.gematik.demis.notificationgateway.common.constants.MessageConstants.CLIENT_ADDRESS_EMPTY;
import static de.gematik.demis.notificationgateway.common.constants.MessageConstants.INSTANTIATION_ERROR_OCCURRED;
import static de.gematik.demis.notificationgateway.common.constants.MessageConstants.VALIDATION_ERROR_OCCURRED;
import static de.gematik.demis.notificationgateway.common.constants.WebConstants.HEADER_X_REAL_IP;
import static de.gematik.demis.notificationgateway.common.constants.WebConstants.LABORATORY_PATH;
import static de.gematik.demis.notificationgateway.common.constants.WebConstants.NOT_AVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;
import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE;
import static org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.demis.notificationgateway.BaseTestUtils;
import de.gematik.demis.notificationgateway.common.constants.MessageConstants;
import de.gematik.demis.notificationgateway.common.dto.ErrorResponse;
import de.gematik.demis.notificationgateway.common.dto.ValidationError;
import de.gematik.demis.notificationgateway.common.enums.SupportedRealm;
import de.gematik.demis.notificationgateway.common.proxies.BundlePublisher;
import de.gematik.demis.notificationgateway.common.utils.Feature;
import de.gematik.demis.notificationgateway.utils.FileUtils;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@Slf4j
@AutoConfigureMockMvc
@ActiveProfiles("test")
@SpringBootTest
class LaboratoryRestControllerIT implements BaseTestUtils {

  private static final String BASE_PATH = "portal/laboratory/invalid/";
  @MockBean BundlePublisher bundlePublisher;
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void givenValidContentTypeWhenPostLaboratoryThen200() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.put(HEADER_X_REAL_IP, List.of("62.23.239.123"));
    when(bundlePublisher.postRequest(
            any(), any(), any(), any(), any(), eq("rki.demis.r4.core"), eq("1.23.0")))
        .thenReturn(createJsonOkParameters("nes/nes_response_OK.json"));

    this.mockMvc
        .perform(post(LABORATORY_PATH).content(getLaboratoryString()).headers(headers).with(csrf()))
        .andExpect(MockMvcResultMatchers.status().isOk());

    Mockito.verify(bundlePublisher, Mockito.times(1))
        .postRequest(
            any(Bundle.class),
            eq(SupportedRealm.LAB),
            any(String.class),
            any(String.class),
            eq("62.23.239.123"),
            eq("rki.demis.r4.core"),
            eq("1.23.0"));
  }

  @Test
  void givenInvalidContentTypeWhenPostLaboratoryThen415() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.put(HttpHeaders.CONTENT_TYPE, List.of("application/fhir+json"));

    final MockHttpServletResponse response =
        this.mockMvc
            .perform(
                post(LABORATORY_PATH).content(getLaboratoryString()).headers(headers).with(csrf()))
            .andReturn()
            .getResponse();

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(UNSUPPORTED_MEDIA_TYPE.value());
    assertThat(response.getContentAsString()).isNotBlank();
    final ErrorResponse errorResponse =
        objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
    assertThat(errorResponse)
        .isNotNull()
        .hasFieldOrPropertyWithValue(
            "message", "Content type 'application/fhir+json' not supported")
        .hasFieldOrPropertyWithValue("path", LABORATORY_PATH)
        .extracting("validationErrors")
        .isNull();

    Mockito.verify(bundlePublisher, Mockito.never())
        .postRequest(
            any(Bundle.class),
            any(SupportedRealm.class),
            any(String.class),
            any(String.class),
            anyString(),
            eq("rki.demis.r4.core"),
            eq("1.23.0"));
  }

  @Test
  void givenInvalidMethodWhenRequestLaboratoryThen405() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    final MockHttpServletResponse response =
        this.mockMvc.perform(get(LABORATORY_PATH).headers(headers)).andReturn().getResponse();

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(METHOD_NOT_ALLOWED.value());
    assertThat(response.getContentAsString()).isNotBlank();
    final ErrorResponse errorResponse =
        objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
    assertThat(errorResponse)
        .isNotNull()
        .hasFieldOrPropertyWithValue("message", "Request method 'GET' not supported")
        .hasFieldOrPropertyWithValue("path", LABORATORY_PATH)
        .extracting("validationErrors")
        .isNull();

    Mockito.verify(bundlePublisher, Mockito.never())
        .postRequest(
            any(Bundle.class),
            any(SupportedRealm.class),
            any(String.class),
            any(String.class),
            anyString(),
            eq("rki.demis.r4.core"),
            eq("1.23.0"));
  }

  @Test
  void givenAllowedOriginWhenPostLaboratoryThen200() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setOrigin("http://localhost:4200");
    headers.put(HEADER_X_REAL_IP, List.of("62.23.239.123"));
    when(bundlePublisher.postRequest(
            any(), any(), any(), any(), any(), eq("rki.demis.r4.core"), eq("1.23.0")))
        .thenReturn(createJsonOkParameters("nes/nes_response_OK.json"));

    this.mockMvc
        .perform(post(LABORATORY_PATH).content(getLaboratoryString()).headers(headers).with(csrf()))
        .andExpect(MockMvcResultMatchers.status().isOk());

    Mockito.verify(bundlePublisher, Mockito.times(1))
        .postRequest(
            any(Bundle.class),
            eq(SupportedRealm.LAB),
            any(String.class),
            any(String.class),
            eq("62.23.239.123"),
            eq("rki.demis.r4.core"),
            eq("1.23.0"));
  }

  @Test
  void givenNotAllowedOriginWhenPostLaboratoryThen403() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setOrigin("http://www.not-allowed.example");

    MockHttpServletResponse errorResponse =
        this.mockMvc
            .perform(post(LABORATORY_PATH).content(getLaboratoryString()).headers(headers))
            .andReturn()
            .getResponse();

    assertThat(errorResponse).isNotNull();
    assertThat(errorResponse.getStatus()).isEqualTo(FORBIDDEN.value());
    assertThat(errorResponse.getContentAsString()).isEqualTo("Invalid CORS request");

    Mockito.verify(bundlePublisher, Mockito.never())
        .postRequest(
            any(Bundle.class),
            any(SupportedRealm.class),
            any(String.class),
            any(String.class),
            anyString(),
            eq("rki.demis.r4.core"),
            eq("1.23.0"));
  }

  @Test
  void givenMissingRealIpHeaderWhenPostLaboratoryThen400() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    final MockHttpServletResponse response =
        this.mockMvc
            .perform(
                post(LABORATORY_PATH).content(getLaboratoryString()).headers(headers).with(csrf()))
            .andReturn()
            .getResponse();

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.value());
    assertThat(response.getContentAsString()).isNotBlank();
    final ErrorResponse errorResponse =
        objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
    assertThat(errorResponse)
        .isNotNull()
        .hasFieldOrPropertyWithValue("message", CLIENT_ADDRESS_EMPTY + " Header: x-real-ip")
        .hasFieldOrPropertyWithValue("path", LABORATORY_PATH)
        .extracting("validationErrors")
        .isNull();

    Mockito.verify(bundlePublisher, Mockito.never())
        .postRequest(
            any(Bundle.class),
            any(SupportedRealm.class),
            any(String.class),
            any(String.class),
            anyString(),
            eq("rki.demis.r4.core"),
            eq("1.23.0"));
  }

  @Test
  void givenEmptyXRealIPWhenPostLaboratoryThen400() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.put(HEADER_X_REAL_IP, List.of(""));

    final MockHttpServletResponse response =
        this.mockMvc
            .perform(
                post(LABORATORY_PATH).content(getLaboratoryString()).headers(headers).with(csrf()))
            .andReturn()
            .getResponse();

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.value());
    assertThat(response.getContentAsString()).isNotBlank();
    final ErrorResponse errorResponse =
        objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
    assertThat(errorResponse)
        .isNotNull()
        .hasFieldOrPropertyWithValue("message", CLIENT_ADDRESS_EMPTY + " Header: x-real-ip")
        .hasFieldOrPropertyWithValue("path", LABORATORY_PATH)
        .extracting("validationErrors")
        .isNull();

    Mockito.verify(bundlePublisher, Mockito.never())
        .postRequest(
            any(Bundle.class),
            any(SupportedRealm.class),
            any(String.class),
            any(String.class),
            anyString(),
            eq("rki.demis.r4.core"),
            eq("1.23.0"));
  }

  @Test
  void givenXRealIPWhenPostLaboratoryThen200() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.put(HEADER_X_REAL_IP, List.of("62.23.239.123"));
    when(bundlePublisher.postRequest(
            any(), any(), any(), any(), any(), eq("rki.demis.r4.core"), eq("1.23.0")))
        .thenReturn(createJsonOkParameters("nes/nes_response_OK.json"));

    this.mockMvc
        .perform(post(LABORATORY_PATH).content(getLaboratoryString()).headers(headers).with(csrf()))
        .andExpect(MockMvcResultMatchers.status().isOk());

    Mockito.verify(bundlePublisher, Mockito.times(1))
        .postRequest(
            any(Bundle.class),
            eq(SupportedRealm.LAB),
            any(String.class),
            any(String.class),
            eq("62.23.239.123"),
            eq("rki.demis.r4.core"),
            eq("1.23.0"));
  }

  @ParameterizedTest
  @CsvSource({
    "nes/nes_operationoutcome_errors_unfiltered.json,15",
    "nes/nes_operationoutcome_errors_filtered.json,3"
  })
  @Feature(files = "NG-003")
  void givenInvalidNameInNotificationWhenPostLaboratoryThen422(
      String errorJsonPath, int nesErrorCount) throws Exception {
    // given
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.put(HEADER_X_REAL_IP, List.of("62.23.239.123"));

    final UnprocessableEntityException exception = new UnprocessableEntityException();
    exception.setOperationOutcome(FileUtils.createOperationOutcomeFromFile(errorJsonPath));
    exception.setResponseBody(FileUtils.loadJsonFromFile(errorJsonPath));

    Mockito.when(
            bundlePublisher.postRequest(
                any(Bundle.class),
                eq(SupportedRealm.LAB),
                any(String.class),
                any(String.class),
                eq("62.23.239.123"),
                eq("rki.demis.r4.core"),
                eq("1.23.0")))
        .thenThrow(exception);

    // when
    MockHttpServletResponse response =
        mockMvc
            .perform(
                post(LABORATORY_PATH)
                    .content(
                        Objects.requireNonNull(
                            FileUtils.loadJsonFromFile(
                                "/portal/laboratory/invalid/notification_content_invalid_name.json")))
                    .headers(headers)
                    .with(csrf()))
            .andReturn()
            .getResponse();

    assertThat(response.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
    assertThat(response.getContentAsString()).isNotBlank();
    final ErrorResponse errorResponse =
        objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
    assertThat(errorResponse).isNotNull();
    final List<ValidationError> validationErrors = errorResponse.getValidationErrors();
    assertThat(validationErrors).hasSize(nesErrorCount);
    assertThat(validationErrors)
        .map(ValidationError::getField)
        .allMatch(field -> field.equals(NOT_AVAILABLE));
    assertThat(validationErrors)
        .map(ValidationError::getMessage)
        .contains(
            "Composition.section:laboratoryReport: max allowed = 0, but found 1 (from https://demis.rki.de/fhir/StructureDefinition/NotificationLaboratorySARSCoV2) (validating against https://demis.rki.de/fhir/StructureDefinition/NotificationLaboratorySARSCoV2|1.0.1 [NotificationLaboratorySARSCoV2])",
            "noNumbersInGivenName: 'Die Vornamen der betroffenen Person dürfen keine Zahlen enthalten.' Rule 'Die Vornamen der betroffenen Person dürfen keine Zahlen enthalten.' Failed",
            "noNumbersInFamilyName: 'Der Nachname der betroffenen Person darf keine Zahlen enthalten.' Rule 'Der Nachname der betroffenen Person darf keine Zahlen enthalten.' Failed");
  }

  @ParameterizedTest
  @CsvSource({
    "no_contacts_in_facility.json,"
        + VALIDATION_ERROR_OCCURRED
        + ",notifierFacility.contacts,size must be between 1 and 2147483647",
    "no_city_in_facility_address.json,"
        + VALIDATION_ERROR_OCCURRED
        + ",notifierFacility.address.city,size must be between 1 and 2147483647",
    "no_street_in_facility_address.json,"
        + VALIDATION_ERROR_OCCURRED
        + ",notifierFacility.address.street,size must be between 1 and 2147483647",
    "no_housenumber_in_facility_address.json,"
        + VALIDATION_ERROR_OCCURRED
        + ",notifierFacility.address.houseNumber,size must be between 1 and 2147483647",
    "empty_addresstype_in_notifier_facility.json,"
        + INSTANTIATION_ERROR_OCCURRED
        + ",notifierFacility.address.addressType,Unexpected value ''",
    "empty_addresstype_in_notified_person.json,"
        + INSTANTIATION_ERROR_OCCURRED
        + ",notifiedPerson.currentAddress.addressType,Unexpected value ''",
    "invalid_addresstype_in_notifier_facility.json,"
        + INSTANTIATION_ERROR_OCCURRED
        + ",notifierFacility.address.addressType,Unexpected value 'INVALID'",
    "empty_salutation.json,"
        + INSTANTIATION_ERROR_OCCURRED
        + ",notifierFacility.contact.salutation,Unexpected value ''"
  })
  @Feature(files = "NG-004")
  void givenInvalidContentWhenPostLaboratoryThen400(
      String fileName, String message, String field, String validationError) throws Exception {
    String path = BASE_PATH + fileName;
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.put(HEADER_X_REAL_IP, List.of("62.23.239.123"));

    final MockHttpServletResponse response =
        this.mockMvc
            .perform(
                post(LABORATORY_PATH).content(getJsonContent(path)).headers(headers).with(csrf()))
            .andReturn()
            .getResponse();

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.value());
    assertThat(response.getContentAsString()).isNotBlank();
    final ErrorResponse errorResponse =
        objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
    assertThat(errorResponse)
        .isNotNull()
        .hasFieldOrPropertyWithValue("message", message)
        .hasFieldOrPropertyWithValue("path", LABORATORY_PATH)
        .extracting(ErrorResponse::getValidationErrors)
        .asList()
        .hasSize(1)
        .first()
        .hasFieldOrPropertyWithValue("field", field)
        .hasFieldOrPropertyWithValue("message", validationError);
  }

  @Test
  void givenHoneypotContentWhenPostLaboratoryThen406() throws Exception {
    String path = "portal/laboratory/invalid/honeypot_quickTest.json";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.put(HEADER_X_REAL_IP, List.of("62.23.239.123"));

    final MockHttpServletResponse response =
        this.mockMvc
            .perform(
                post(LABORATORY_PATH).content(getJsonContent(path)).headers(headers).with(csrf()))
            .andReturn()
            .getResponse();

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(NOT_ACCEPTABLE.value());
    assertThat(response.getContentAsString()).isNotBlank();
    final ErrorResponse errorResponse =
        objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
    assertThat(errorResponse)
        .isNotNull()
        .hasFieldOrPropertyWithValue("message", MessageConstants.CONTENT_NOT_ACCEPTED)
        .hasFieldOrPropertyWithValue("path", LABORATORY_PATH)
        .extracting("validationErrors")
        .isNull();

    Mockito.verify(bundlePublisher, Mockito.never())
        .postRequest(
            any(Bundle.class),
            any(SupportedRealm.class),
            any(String.class),
            any(String.class),
            anyString(),
            eq("rki.demis.r4.core"),
            eq("1.23.0"));
  }
}
