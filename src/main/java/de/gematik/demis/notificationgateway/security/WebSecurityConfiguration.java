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

package de.gematik.demis.notificationgateway.security;

import static de.gematik.demis.notificationgateway.common.constants.WebConstants.API_NG_CONFIGS;
import static de.gematik.demis.notificationgateway.common.constants.WebConstants.API_NG_NOTIFICATION;
import static de.gematik.demis.notificationgateway.common.constants.WebConstants.API_NG_REPORTS;
import static de.gematik.demis.notificationgateway.common.constants.WebConstants.API_NG_SERVICES;
import static de.gematik.demis.notificationgateway.common.constants.WebConstants.CONFIG_PORTAL_PATH;
import static de.gematik.demis.notificationgateway.common.constants.WebConstants.LABORATORY_PATH;
import static de.gematik.demis.notificationgateway.common.constants.WebConstants.PATHOGEN_PATH;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

import de.gematik.demis.notificationgateway.security.fidp.FederatedIdentityTokenChecker;
import de.gematik.demis.notificationgateway.security.oauth2.JwtAuthenticationFilter;
import de.gematik.demis.notificationgateway.security.oauth2.UnifiedOAuth2TokenValidator;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@Slf4j
public class WebSecurityConfiguration {

  private final GatewayAuthenticationEntryPoint unauthorizedHandler;

  private final List<String> allowedOrigins;

  private final List<String> allowedHeaders;

  private final boolean activateCsrf;

  private final AuthenticationManagerResolver authenticationManagerResolver;

  private final UnifiedOAuth2TokenValidator tokenValidator;
  private final FederatedIdentityTokenChecker federatedIdentityTokenChecker;

  public WebSecurityConfiguration(
      final GatewayAuthenticationEntryPoint unauthorizedHandler,
      @Value("#{'${allowed.origins}'.split(',')}") final List<String> allowedOrigins,
      @Value("#{'${allowed.headers}'.split(',')}") final List<String> allowedHeaders,
      @Value("#{'${activate.csrf}'}") final boolean activateCsrf,
      final AuthenticationManagerResolver authenticationManagerResolver,
      final UnifiedOAuth2TokenValidator tokenValidator,
      final FederatedIdentityTokenChecker federatedIdentityTokenChecker) {
    this.unauthorizedHandler = unauthorizedHandler;
    this.allowedOrigins = allowedOrigins;
    this.allowedHeaders = allowedHeaders;
    this.activateCsrf = activateCsrf;
    this.authenticationManagerResolver = authenticationManagerResolver;
    this.tokenValidator = tokenValidator;
    this.federatedIdentityTokenChecker = federatedIdentityTokenChecker;
  }

  @Bean
  public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
    if (activateCsrf) {
      return http.cors()
          .and()
          .authorizeHttpRequests()
          .antMatchers(LABORATORY_PATH, PATHOGEN_PATH + "/**", "/actuator/**", CONFIG_PORTAL_PATH)
          .permitAll()
          .anyRequest()
          .authenticated()
          .and()
          .oauth2ResourceServer()
          .authenticationManagerResolver(jwtIssuerAuthenticationManagerResolver())
          .authenticationEntryPoint(unauthorizedHandler)
          .and()
          .sessionManagement()
          .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
          .and()
          .addFilterAfter(authenticationTokenFilter(), BasicAuthenticationFilter.class)
          .build();
    } else {
      return http.csrf()
          .disable()
          .cors()
          .and()
          .authorizeHttpRequests()
          .antMatchers(LABORATORY_PATH, PATHOGEN_PATH + "/**", "/actuator/**", CONFIG_PORTAL_PATH)
          .permitAll()
          .anyRequest()
          .authenticated()
          .and()
          .oauth2ResourceServer()
          .authenticationManagerResolver(jwtIssuerAuthenticationManagerResolver())
          .authenticationEntryPoint(unauthorizedHandler)
          .and()
          .sessionManagement()
          .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
          .and()
          .addFilterAfter(authenticationTokenFilter(), BasicAuthenticationFilter.class)
          .build();
    }
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    final var corsConfiguration = new CorsConfiguration();
    corsConfiguration.setAllowedMethods(List.of(POST.name(), GET.name()));
    corsConfiguration.setAllowedOrigins(allowedOrigins);
    corsConfiguration.setAllowedHeaders(allowedHeaders);

    final var configurationSource = new UrlBasedCorsConfigurationSource();
    configurationSource.registerCorsConfiguration(API_NG_NOTIFICATION + "/**", corsConfiguration);
    configurationSource.registerCorsConfiguration(API_NG_SERVICES + "/**", corsConfiguration);
    configurationSource.registerCorsConfiguration(API_NG_REPORTS + "/**", corsConfiguration);
    configurationSource.registerCorsConfiguration(API_NG_CONFIGS + "/**", corsConfiguration);

    return configurationSource;
  }

  public JwtAuthenticationFilter authenticationTokenFilter() {
    return new JwtAuthenticationFilter(tokenValidator, federatedIdentityTokenChecker);
  }

  @Bean
  public JwtIssuerAuthenticationManagerResolver jwtIssuerAuthenticationManagerResolver() {
    final Map<String, AuthenticationManager> authenticationManagers =
        authenticationManagerResolver.getAuthenticationManager();
    return new JwtIssuerAuthenticationManagerResolver(authenticationManagers::get);
  }
}
