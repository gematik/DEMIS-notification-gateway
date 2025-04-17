package de.gematik.demis.notificationgateway.common.exceptions;

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

import static de.gematik.demis.notificationgateway.common.constants.MessageConstants.VALIDATION_ERROR_OCCURRED;
import static de.gematik.demis.notificationgateway.common.constants.WebConstants.NOT_AVAILABLE;
import static de.gematik.demis.notificationgateway.common.enums.InternalCoreError.NG_100_TOKEN;
import static de.gematik.demis.notificationgateway.common.enums.InternalCoreError.NG_200_VALIDATION;
import static de.gematik.demis.notificationgateway.common.enums.InternalCoreError.NG_300_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.*;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.demis.exceptions.TokenException;
import de.gematik.demis.notificationgateway.common.dto.ErrorResponse;
import de.gematik.demis.notificationgateway.common.enums.InternalCoreError;
import de.gematik.demis.service.base.error.ServiceCallException;
import jakarta.security.auth.message.AuthException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;

@ExtendWith(MockitoExtension.class)
class ErrorResponseControllerTest {
  private final ErrorResponseController responseController =
      new ErrorResponseController(new ObjectMapper());
  private static RandomStringUtils random = RandomStringUtils.secure();
  private static final String REQUEST_URI = random.nextAlphabetic(5);

  @Test
  void givenBadRequestExceptionWhenHandleBadRequestExceptionThen400() {
    BadRequestException exception = new BadRequestException(random.nextAlphabetic(5));
    MockHttpServletRequest request = new MockHttpServletRequest(GET.name(), REQUEST_URI);

    final ResponseEntity<ErrorResponse> responseEntity =
        responseController.handleBadRequestException(exception, request);

    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(responseEntity.getBody()).isNotNull();
    final ErrorResponse errorResponse = responseEntity.getBody();
    assertThat(errorResponse)
        .isNotNull()
        .hasFieldOrPropertyWithValue("statusCode", BAD_REQUEST.value())
        .hasFieldOrPropertyWithValue("message", exception.getMessage())
        .hasFieldOrPropertyWithValue("path", REQUEST_URI);
    assertThat(errorResponse.getValidationErrors()).isNullOrEmpty();
    assertThat(errorResponse.getTimestamp())
        .isCloseTo(OffsetDateTime.now(), within(3, ChronoUnit.SECONDS));
  }

  @Test
  void givenMethodArgumentNotValidExceptionWhenHandleBadRequestExceptionThen400() {
    final FieldError fieldError =
        new FieldError(
            random.nextAlphabetic(5), random.nextAlphabetic(5), random.nextAlphabetic(10));
    MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
    when(exception.getFieldErrors()).thenReturn(List.of(fieldError));

    MockHttpServletRequest request = new MockHttpServletRequest(GET.name(), REQUEST_URI);

    final ResponseEntity<ErrorResponse> responseEntity =
        responseController.handleBadRequestException(exception, request);

    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(responseEntity.getBody()).isNotNull();
    final ErrorResponse errorResponse = responseEntity.getBody();
    assertThat(errorResponse)
        .isNotNull()
        .hasFieldOrPropertyWithValue("statusCode", BAD_REQUEST.value())
        .hasFieldOrPropertyWithValue("message", VALIDATION_ERROR_OCCURRED)
        .hasFieldOrPropertyWithValue("path", REQUEST_URI);
    assertThat(errorResponse.getValidationErrors())
        .isNotEmpty()
        .hasSize(1)
        .first()
        .hasFieldOrPropertyWithValue("field", fieldError.getField())
        .hasFieldOrPropertyWithValue("message", fieldError.getDefaultMessage());
    assertThat(errorResponse.getTimestamp())
        .isCloseTo(OffsetDateTime.now(), within(3, ChronoUnit.SECONDS));
  }

  @Test
  void givenHttpMessageNotReadableExceptionWhenHandleBadRequestExceptionThen400() {
    HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);
    when(exception.getMessage()).thenReturn(random.nextAlphabetic(10));
    final String message = random.nextAlphabetic(10);
    when(exception.getMostSpecificCause()).thenReturn(new Throwable(message));
    MockHttpServletRequest request = new MockHttpServletRequest(GET.name(), REQUEST_URI);

    final ResponseEntity<ErrorResponse> responseEntity =
        responseController.handleBadRequestException(exception, request);

    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(responseEntity.getBody()).isNotNull();
    final ErrorResponse errorResponse = responseEntity.getBody();
    assertThat(errorResponse)
        .isNotNull()
        .hasFieldOrPropertyWithValue("statusCode", BAD_REQUEST.value())
        .hasFieldOrPropertyWithValue("message", message)
        .hasFieldOrPropertyWithValue("path", REQUEST_URI);
    assertThat(errorResponse.getValidationErrors()).isNullOrEmpty();
    assertThat(errorResponse.getTimestamp())
        .isCloseTo(OffsetDateTime.now(), within(3, ChronoUnit.SECONDS));
  }

  @Test
  void givenAuthExceptionWhenHandleAuthExceptionThen401() {
    AuthException exception = mock(AuthException.class);
    final String message = random.nextAlphabetic(10);
    when(exception.getMessage()).thenReturn(message);
    MockHttpServletRequest request = new MockHttpServletRequest(GET.name(), REQUEST_URI);

    final ResponseEntity<ErrorResponse> responseEntity =
        responseController.handleAuthException(exception, request);

    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(responseEntity.getBody()).isNotNull();
    final ErrorResponse errorResponse = responseEntity.getBody();
    assertThat(errorResponse)
        .isNotNull()
        .hasFieldOrPropertyWithValue("statusCode", UNAUTHORIZED.value())
        .hasFieldOrPropertyWithValue("message", message)
        .hasFieldOrPropertyWithValue("path", REQUEST_URI);
    assertThat(errorResponse.getValidationErrors()).isNullOrEmpty();
    assertThat(errorResponse.getTimestamp())
        .isCloseTo(OffsetDateTime.now(), within(3, ChronoUnit.SECONDS));
  }

  @Test
  void givenConstraintViolationExceptionWhenHandleBadRequestExceptionThen400() {
    ConstraintViolationException exception = mock(ConstraintViolationException.class);
    when(exception.getMessage()).thenReturn(random.nextAlphabetic(10));
    ConstraintViolation<String> constraintViolation = mock(ConstraintViolation.class);
    final String message = random.nextAlphabetic(10);
    when(constraintViolation.getMessage()).thenReturn(message);
    when(exception.getConstraintViolations()).thenReturn(Set.of(constraintViolation));
    MockHttpServletRequest request = new MockHttpServletRequest(GET.name(), REQUEST_URI);

    final ResponseEntity<ErrorResponse> responseEntity =
        responseController.handleBadRequestException(exception, request);

    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(responseEntity.getBody()).isNotNull();
    final ErrorResponse errorResponse = responseEntity.getBody();
    assertThat(errorResponse)
        .isNotNull()
        .hasFieldOrPropertyWithValue("statusCode", BAD_REQUEST.value())
        .hasFieldOrPropertyWithValue("message", message)
        .hasFieldOrPropertyWithValue("path", REQUEST_URI);
    assertThat(errorResponse.getValidationErrors()).isNullOrEmpty();
    assertThat(errorResponse.getTimestamp())
        .isCloseTo(OffsetDateTime.now(), within(3, ChronoUnit.SECONDS));
  }

  @Test
  void givenHttpRequestMethodNotSupportedExceptionWhenHandleMethodNotAllowedExceptionThen405() {
    HttpRequestMethodNotSupportedException exception =
        mock(HttpRequestMethodNotSupportedException.class);
    final String message = random.nextAlphabetic(10);
    when(exception.getMessage()).thenReturn(message);
    MockHttpServletRequest request = new MockHttpServletRequest(GET.name(), REQUEST_URI);

    final ResponseEntity<ErrorResponse> responseEntity =
        responseController.handleMethodNotAllowedException(exception, request);

    assertThat(responseEntity.getStatusCode()).isEqualTo(METHOD_NOT_ALLOWED);
    assertThat(responseEntity.getBody()).isNotNull();
    final ErrorResponse errorResponse = responseEntity.getBody();
    assertThat(errorResponse)
        .isNotNull()
        .hasFieldOrPropertyWithValue("statusCode", METHOD_NOT_ALLOWED.value())
        .hasFieldOrPropertyWithValue("message", message)
        .hasFieldOrPropertyWithValue("path", REQUEST_URI);
    assertThat(errorResponse.getValidationErrors()).isNullOrEmpty();
    assertThat(errorResponse.getTimestamp())
        .isCloseTo(OffsetDateTime.now(), within(3, ChronoUnit.SECONDS));
  }

  @Test
  void
      givenHttpMediaTypeNotSupportedExceptionWhenHandleHttpMediaTypeNotSupportedExceptionThen415() {
    HttpMediaTypeNotSupportedException exception = mock(HttpMediaTypeNotSupportedException.class);
    final String message = random.nextAlphabetic(10);
    when(exception.getMessage()).thenReturn(message);
    MockHttpServletRequest request = new MockHttpServletRequest(GET.name(), REQUEST_URI);

    final ResponseEntity<ErrorResponse> responseEntity =
        responseController.handleUnsupportedMediaTypeException(exception, request);

    assertThat(responseEntity.getStatusCode()).isEqualTo(UNSUPPORTED_MEDIA_TYPE);
    assertThat(responseEntity.getBody()).isNotNull();
    final ErrorResponse errorResponse = responseEntity.getBody();
    assertThat(errorResponse)
        .isNotNull()
        .hasFieldOrPropertyWithValue("statusCode", UNSUPPORTED_MEDIA_TYPE.value())
        .hasFieldOrPropertyWithValue("message", message)
        .hasFieldOrPropertyWithValue("path", REQUEST_URI);
    assertThat(errorResponse.getValidationErrors()).isNullOrEmpty();
    assertThat(errorResponse.getTimestamp())
        .isCloseTo(OffsetDateTime.now(), within(3, ChronoUnit.SECONDS));
  }

  @ParameterizedTest
  @MethodSource("provideInternalCoreError")
  void givenBaseServerResponseExceptionWhenHandleCoreExceptionThenCoreStatus(
      InternalCoreError internalCoreError, int status) {
    BaseServerResponseException exception = mock(BaseServerResponseException.class);
    when(exception.getStatusCode()).thenReturn(status);
    when(exception.getMessage()).thenReturn(internalCoreError.reason());
    MockHttpServletRequest request = new MockHttpServletRequest(GET.name(), REQUEST_URI);

    final ResponseEntity<ErrorResponse> responseEntity =
        responseController.handleCoreException(exception, request);

    assertThat(responseEntity.getStatusCode().value()).isEqualTo(status);
    assertThat(responseEntity.getBody()).isNotNull();
    final ErrorResponse errorResponse = responseEntity.getBody();
    assertThat(errorResponse)
        .isNotNull()
        .hasFieldOrPropertyWithValue("statusCode", status)
        .hasFieldOrPropertyWithValue("message", internalCoreError.reason())
        .hasFieldOrPropertyWithValue("path", REQUEST_URI);
    assertThat(errorResponse.getValidationErrors()).isNullOrEmpty();
    assertThat(errorResponse.getTimestamp())
        .isCloseTo(OffsetDateTime.now(), within(3, ChronoUnit.SECONDS));
  }

  @Test
  void givenBaseServerResponseExceptionWhenHandleCoreExceptionThenCore500() {
    final OperationOutcome.OperationOutcomeIssueComponent issueComponent =
        new OperationOutcome.OperationOutcomeIssueComponent()
            .setSeverity(OperationOutcome.IssueSeverity.ERROR)
            .setDiagnostics(random.nextAlphabetic(10))
            .setCode(OperationOutcome.IssueType.EXCEPTION);
    OperationOutcome outcome = new OperationOutcome().addIssue(issueComponent);
    BaseServerResponseException exception = mock(BaseServerResponseException.class);
    when(exception.getStatusCode()).thenReturn(-1);
    when(exception.getOperationOutcome()).thenReturn(outcome);
    MockHttpServletRequest request = new MockHttpServletRequest(GET.name(), REQUEST_URI);

    final ResponseEntity<ErrorResponse> responseEntity =
        responseController.handleCoreException(exception, request);

    assertThat(responseEntity.getStatusCode()).isEqualTo(INTERNAL_SERVER_ERROR);
    assertThat(responseEntity.getBody()).isNotNull();
    final ErrorResponse errorResponse = responseEntity.getBody();
    assertThat(errorResponse)
        .isNotNull()
        .hasFieldOrPropertyWithValue("statusCode", INTERNAL_SERVER_ERROR.value())
        .hasFieldOrPropertyWithValue("message", NG_300_REQUEST.reason())
        .hasFieldOrPropertyWithValue("path", REQUEST_URI);
    assertThat(errorResponse.getValidationErrors())
        .isNotEmpty()
        .hasSize(1)
        .first()
        .hasFieldOrPropertyWithValue("field", NOT_AVAILABLE)
        .hasFieldOrPropertyWithValue("message", issueComponent.getDiagnostics());
    assertThat(errorResponse.getTimestamp())
        .isCloseTo(OffsetDateTime.now(), within(3, ChronoUnit.SECONDS));
  }

  @ParameterizedTest
  @MethodSource("provideInternalServerException")
  void givenExceptionWhenHandleInternalServerErrorThen500(Exception exception) {
    MockHttpServletRequest request = new MockHttpServletRequest(GET.name(), REQUEST_URI);

    final ResponseEntity<ErrorResponse> responseEntity =
        responseController.handleInternalServerError(exception, request);

    assertThat(responseEntity.getStatusCode()).isEqualTo(INTERNAL_SERVER_ERROR);
    assertThat(responseEntity.getBody()).isNotNull();
    final ErrorResponse errorResponse = responseEntity.getBody();
    assertThat(errorResponse)
        .isNotNull()
        .hasFieldOrPropertyWithValue("statusCode", INTERNAL_SERVER_ERROR.value())
        .hasFieldOrPropertyWithValue("message", exception.getMessage())
        .hasFieldOrPropertyWithValue("path", REQUEST_URI);
    assertThat(errorResponse.getValidationErrors()).isNullOrEmpty();
    assertThat(errorResponse.getTimestamp())
        .isCloseTo(OffsetDateTime.now(), within(3, ChronoUnit.SECONDS));
  }

  @Test
  void givenServiceCallExceptionWhenHandleNotImplementedError() {
    int httpStatus = 501;
    ResponseEntity<ErrorResponse> response =
        this.responseController.handleCoreException(
            new ServiceCallException("my message", "e-42", httpStatus, null), null);
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
    ErrorResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getStatusCode()).isEqualTo(httpStatus);
    assertThat(body.getMessage()).isEqualTo(NG_300_REQUEST.reason());
  }

  private static Stream<Arguments> provideInternalServerException() {
    return Stream.of(
        Arguments.of(new Exception(random.nextAlphabetic(10))),
        Arguments.of(new TokenException(NG_100_TOKEN.reason())));
  }

  private static Stream<Arguments> provideInternalCoreError() {
    return Stream.of(
        Arguments.of(NG_100_TOKEN, 401),
        Arguments.of(NG_100_TOKEN, 403),
        Arguments.of(NG_200_VALIDATION, 400),
        Arguments.of(NG_200_VALIDATION, 422),
        Arguments.of(NG_300_REQUEST, 500));
  }
}
