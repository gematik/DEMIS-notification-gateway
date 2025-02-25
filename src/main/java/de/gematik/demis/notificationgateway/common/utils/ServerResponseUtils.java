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
 * #L%
 */

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.demis.notificationgateway.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import org.springframework.http.MediaType;

/** Class to create easily server response objects. */
public final class ServerResponseUtils {
  private ServerResponseUtils() {
    throw new UnsupportedOperationException("Unsupported");
  }

  private static final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  /**
   * Creates an {@link ErrorResponse} object given path and message.
   *
   * @param requestPath the request URI path given, generating the error
   * @param errorMessage the content of the error
   * @return an {@link ErrorResponse} object
   */
  public static ErrorResponse createErrorResponse(
      final String requestPath, final String errorMessage) {

    final ErrorResponse errorResponse = new ErrorResponse();
    errorResponse.setPath(requestPath);
    errorResponse.setStatusCode(UNAUTHORIZED.value());
    errorResponse.setMessage(errorMessage);
    errorResponse.setTimestamp(OffsetDateTime.now());
    return errorResponse;
  }

  /**
   * Writes an {@link ErrorResponse} object into a servlet response as JSON object.
   *
   * @param httpServletResponse the {@link HttpServletResponse} to use for responding
   * @param errorResponse the {@link ErrorResponse} object to be written as JSON
   * @throws IOException if the message could not be written
   */
  public static void writeErrorResponse(
      final HttpServletResponse httpServletResponse, final ErrorResponse errorResponse)
      throws IOException {
    httpServletResponse.setStatus(UNAUTHORIZED.value());
    httpServletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(httpServletResponse.getWriter(), errorResponse);
  }

  /**
   * Writes an {@link ErrorResponse} object, by creating it internally, into a servlet response as
   * JSON object.
   *
   * @param httpServletResponse the {@link HttpServletResponse} to use for responding
   * @param requestPath the request URI path given, generating the error
   * @param errorMessage the content of the error
   * @throws IOException if the message could not be written
   */
  public static void writeErrorResponse(
      final HttpServletResponse httpServletResponse,
      final String requestPath,
      final String errorMessage)
      throws IOException {
    httpServletResponse.setStatus(UNAUTHORIZED.value());
    httpServletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);

    final var errorResponse = createErrorResponse(requestPath, errorMessage);
    objectMapper.writeValue(httpServletResponse.getWriter(), errorResponse);
  }
}
