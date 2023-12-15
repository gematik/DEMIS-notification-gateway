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

package de.gematik.demis.notificationgateway.security.oauth2;

import static de.gematik.demis.notificationgateway.common.constants.MessageConstants.JWT_TOKEN_AUTHENTICATION_FAILED;

import de.gematik.demis.notificationgateway.common.utils.ServerResponseUtils;
import de.gematik.demis.notificationgateway.security.fidp.FederatedIdentityTokenChecker;
import java.io.IOException;
import java.util.Objects;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.GenericFilterBean;

@Slf4j
public class JwtAuthenticationFilter extends GenericFilterBean {

  private static final String AUTH_HEADER = "Authorization";
  private static final String TOKEN_PREFIX = "Bearer ";
  private final UnifiedOAuth2TokenValidator tokenValidator;
  private final FederatedIdentityTokenChecker federatedIdentityTokenChecker;

  public JwtAuthenticationFilter(
      final UnifiedOAuth2TokenValidator tokenValidator,
      final FederatedIdentityTokenChecker federatedIdentityTokenValidator) {
    this.tokenValidator = tokenValidator;
    this.federatedIdentityTokenChecker = federatedIdentityTokenValidator;
  }

  @Override
  public void doFilter(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws IOException, ServletException {

    final var request = (HttpServletRequest) servletRequest;
    final var response = (HttpServletResponse) servletResponse;

    final var header = request.getHeader(AUTH_HEADER);

    // Skip the evaluation of the JWT Token
    if (Objects.isNull(header) || !header.startsWith(TOKEN_PREFIX)) {
      filterChain.doFilter(request, response);
      return;
    }

    final var authToken = header.replace(TOKEN_PREFIX, "");

    if (federatedIdentityTokenChecker.isFederatedIdpToken(authToken)) {
      filterChain.doFilter(request, response);
      return;
    }

    final var requestUri = request.getRequestURI();

    log.info("Performing check for URI {}", requestUri);
    final var validationResult = tokenValidator.validateClaimContent(authToken, requestUri);

    if (!validationResult.hasErrors()) {
      filterChain.doFilter(request, response);

      return;
    }

    final var foundError = validationResult.getErrors().stream().findFirst();
    String errorDescription = JWT_TOKEN_AUTHENTICATION_FAILED;
    if (foundError.isPresent() && Objects.nonNull(foundError.get().getDescription())) {
      errorDescription = foundError.get().getDescription();
    }

    log.error(errorDescription);
    ServerResponseUtils.writeErrorResponse(response, requestUri, errorDescription);
  }
}
