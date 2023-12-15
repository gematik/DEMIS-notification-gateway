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

package de.gematik.demis.notificationgateway.common.exceptions;

import static de.gematik.demis.notificationgateway.common.constants.MessageConstants.INSTANTIATION_ERROR_OCCURRED;
import static de.gematik.demis.notificationgateway.common.constants.MessageConstants.VALIDATION_ERROR_OCCURRED;
import static de.gematik.demis.notificationgateway.common.constants.WebConstants.NOT_AVAILABLE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;
import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE;
import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import com.fasterxml.jackson.databind.JsonMappingException.Reference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import de.gematik.demis.exceptions.TokenException;
import de.gematik.demis.notificationgateway.common.dto.ErrorResponse;
import de.gematik.demis.notificationgateway.common.dto.ValidationError;
import de.gematik.demis.notificationgateway.common.enums.InternalCoreError;
import feign.FeignException;
import feign.Request;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class ErrorResponseController {

  private final ObjectMapper objectMapper;

  @ExceptionHandler({FeignException.class, BaseServerResponseException.class})
  public ResponseEntity<ErrorResponse> handleCoreException(
      final Exception exception, final HttpServletRequest request) {
    ErrorResponse errorResponse;
    if (exception instanceof FeignException) {
      errorResponse = handleFeignException((FeignException) exception);
    } else {
      errorResponse =
          handleBaseServerResponseException((BaseServerResponseException) exception, request);
    }
    logResponseStatusCodeAndErrorMessage(errorResponse.getStatusCode(), exception);

    return ResponseEntity.status(errorResponse.getStatusCode())
        .contentType(MediaType.APPLICATION_JSON)
        .body(errorResponse);
  }

  @ExceptionHandler(
      value = {
        ConstraintViolationException.class,
        MissingRequestHeaderException.class,
        HttpMessageNotReadableException.class,
        MethodArgumentNotValidException.class,
        BadRequestException.class,
        ConversionFailedException.class
      })
  public ResponseEntity<ErrorResponse> handleBadRequestException(
      final Exception exception, final HttpServletRequest request) {
    String message = exception.getMessage();
    final String path = request.getRequestURI();
    final int statusCode = BAD_REQUEST.value();
    final ErrorResponse errorResponse = createErrorResponse(path, statusCode, message);
    if (exception instanceof MethodArgumentNotValidException) {
      message = VALIDATION_ERROR_OCCURRED;
      handleMethodArgumentNotValidException(
          (MethodArgumentNotValidException) exception, errorResponse);
    } else if (exception instanceof HttpMessageNotReadableException) {
      message = handleHttpMessageNotReadableException(exception, errorResponse);
    } else if (exception instanceof ConstraintViolationException) {
      message = handleConstraintViolationException((ConstraintViolationException) exception);
    }
    logResponseStatusCodeAndErrorMessage(statusCode, exception);
    errorResponse.setMessage(message);

    return ResponseEntity.status(BAD_REQUEST)
        .contentType(MediaType.APPLICATION_JSON)
        .body(errorResponse);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotAllowedException(
      final Exception exception, final HttpServletRequest request) {
    String message = exception.getMessage();
    final int statusCode = METHOD_NOT_ALLOWED.value();
    final String path = request.getRequestURI();
    logResponseStatusCodeAndErrorMessage(statusCode, exception);
    final ErrorResponse errorResponse = createErrorResponse(path, statusCode, message);

    return ResponseEntity.status(METHOD_NOT_ALLOWED)
        .contentType(MediaType.APPLICATION_JSON)
        .body(errorResponse);
  }

  @ExceptionHandler(HoneypotException.class)
  public ResponseEntity<ErrorResponse> handleHoneypotException(
      final Exception exception, final HttpServletRequest request) {
    String message = exception.getMessage();
    final int statusCode = NOT_ACCEPTABLE.value();
    final String path = request.getRequestURI();
    logResponseStatusCodeAndErrorMessage(statusCode, exception);
    final ErrorResponse errorResponse = createErrorResponse(path, statusCode, message);

    return ResponseEntity.status(NOT_ACCEPTABLE)
        .contentType(MediaType.APPLICATION_JSON)
        .body(errorResponse);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleUnsupportedMediaTypeException(
      final Exception exception, final HttpServletRequest request) {
    String message = exception.getMessage();
    final int statusCode = UNSUPPORTED_MEDIA_TYPE.value();
    final String path = request.getRequestURI();
    logResponseStatusCodeAndErrorMessage(statusCode, exception);
    final ErrorResponse errorResponse = createErrorResponse(path, statusCode, message);

    return ResponseEntity.status(UNSUPPORTED_MEDIA_TYPE)
        .contentType(MediaType.APPLICATION_JSON)
        .body(errorResponse);
  }

  @ExceptionHandler(value = {Exception.class, TokenException.class})
  public ResponseEntity<ErrorResponse> handleInternalServerError(
      final Exception exception, final HttpServletRequest request) {
    String message = exception.getMessage();
    final int statusCode = INTERNAL_SERVER_ERROR.value();
    final String path = request.getRequestURI();
    logResponseStatusCodeAndErrorMessage(statusCode, exception);
    if (exception instanceof TokenException) {
      message = InternalCoreError.NG_100_TOKEN.reason();
    }
    final ErrorResponse errorResponse = createErrorResponse(path, statusCode, message);

    return ResponseEntity.status(INTERNAL_SERVER_ERROR)
        .contentType(MediaType.APPLICATION_JSON)
        .body(errorResponse);
  }

  private ErrorResponse handleBaseServerResponseException(
      BaseServerResponseException exception, HttpServletRequest request) {
    final HttpStatus httpStatus = resolveCoreExceptionStatus(exception.getStatusCode());
    final String message = resolveCoreExceptionMessage(httpStatus, exception.getMessage());
    final String path = request.getRequestURI();
    ErrorResponse errorResponse = createErrorResponse(path, httpStatus.value(), message);

    final OperationOutcome outcome = (OperationOutcome) exception.getOperationOutcome();
    if (outcome != null) {
      final List<ValidationError> validationErrors =
          outcome.getIssue().stream().map(this::createValidationError).collect(Collectors.toList());
      errorResponse.setValidationErrors(validationErrors);
    }

    return errorResponse;
  }

  private ErrorResponse createErrorResponse(String path, Integer statusCode, String message) {
    return new ErrorResponse()
        .path(path)
        .statusCode(statusCode)
        .message(message)
        .timestamp(OffsetDateTime.now());
  }

  private void logResponseStatusCodeAndErrorMessage(int statusCode, Exception exception) {
    log.error("Sending response to portal with status code: {}", statusCode);
    log.error("Error message: {}", exception.getMessage());
  }

  private String handleHttpMessageNotReadableException(
      Exception exception, ErrorResponse errorResponse) {
    final String mostSpecificMessage =
        ((HttpMessageNotReadableException) exception).getMostSpecificCause().getLocalizedMessage();

    final Throwable cause = exception.getCause();
    if (cause instanceof ValueInstantiationException) {
      final ValueInstantiationException valueInstantiationException =
          (ValueInstantiationException) cause;
      final List<Reference> path = valueInstantiationException.getPath();
      final String field =
          path.isEmpty()
              ? NOT_AVAILABLE
              : path.stream().map(Reference::getFieldName).collect(Collectors.joining("."));

      List<ValidationError> validationErrors = new ArrayList<>();
      validationErrors.add(new ValidationError().field(field).message(mostSpecificMessage));
      errorResponse.setValidationErrors(validationErrors);
      return INSTANTIATION_ERROR_OCCURRED;
    }

    return mostSpecificMessage;
  }

  private String handleConstraintViolationException(ConstraintViolationException exception) {
    return exception.getConstraintViolations().stream()
        .map(ConstraintViolation::getMessage)
        .collect(Collectors.joining(","));
  }

  private void handleMethodArgumentNotValidException(
      MethodArgumentNotValidException exception, ErrorResponse errorResponse) {
    List<ValidationError> validationErrors =
        exception.getFieldErrors().stream()
            .map(this::createValidationError)
            .collect(Collectors.toList());
    errorResponse.setValidationErrors(validationErrors);
  }

  private ErrorResponse handleFeignException(FeignException feignException) {
    final HttpStatus httpStatus = resolveCoreExceptionStatus(feignException.status());
    String message = resolveCoreExceptionMessage(httpStatus, feignException.getMessage());
    final Optional<ByteBuffer> responseOptional = feignException.responseBody();
    final Request request = feignException.request();
    ErrorResponse defaultErrorResponse =
        createErrorResponse(request.url(), httpStatus.value(), message);
    if (responseOptional.isEmpty()) {
      return defaultErrorResponse;
    }
    try {
      final ByteBuffer byteBuffer = responseOptional.get();
      final JsonNode jsonNode = objectMapper.readTree(byteBuffer.array());
      return createErrorResponse(
          jsonNode.findPath("path").asText(),
          httpStatus.value(),
          jsonNode.findPath("error").asText());
    } catch (IOException e) {
      message = new String(responseOptional.get().array());
      return defaultErrorResponse.message(message);
    }
  }

  private ValidationError createValidationError(FieldError fieldError) {
    ValidationError validationError = new ValidationError();
    validationError.setField(fieldError.getField());
    validationError.setMessage(safeDefaultMessage().apply(fieldError));
    return validationError;
  }

  private ValidationError createValidationError(
      OperationOutcome.OperationOutcomeIssueComponent issueComponent) {
    ValidationError validationError = new ValidationError();
    validationError.setField(NOT_AVAILABLE);
    validationError.setMessage(issueComponent.getDiagnostics());
    return validationError;
  }

  private Function<FieldError, String> safeDefaultMessage() {
    return fieldError ->
        Strings.isBlank(fieldError.getDefaultMessage())
            ? "unknown error"
            : fieldError.getDefaultMessage();
  }

  private static HttpStatus resolveCoreExceptionStatus(int exceptionStatus) {
    final HttpStatus coreStatus = HttpStatus.resolve(exceptionStatus);
    if (coreStatus != null && coreStatus.value() == 501) {
      return NOT_IMPLEMENTED;
    } else if (coreStatus == null || coreStatus.is5xxServerError()) {
      return INTERNAL_SERVER_ERROR;
    } else {
      return coreStatus;
    }
  }

  private String resolveCoreExceptionMessage(HttpStatus httpStatus, String defaultMessage) {
    if (httpStatus.is5xxServerError()) {
      return InternalCoreError.NG_300_REQUEST.reason();
    } else if (BAD_REQUEST == httpStatus || UNPROCESSABLE_ENTITY == httpStatus) {
      return InternalCoreError.NG_200_VALIDATION.reason();
    } else if (UNAUTHORIZED == httpStatus || FORBIDDEN == httpStatus) {
      return InternalCoreError.NG_100_TOKEN.reason();
    }
    return defaultMessage;
  }
}
