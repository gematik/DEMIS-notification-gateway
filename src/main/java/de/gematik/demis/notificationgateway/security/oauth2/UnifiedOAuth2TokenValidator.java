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

import static de.gematik.demis.notificationgateway.common.constants.WebConstants.ALLOWED_RESOURCE_ACCESS_CLAIM;
import static de.gematik.demis.notificationgateway.common.constants.WebConstants.BEARER_TYPE;
import static de.gematik.demis.notificationgateway.common.constants.WebConstants.PROFESSION_OID_CLAIM;
import static de.gematik.demis.notificationgateway.common.constants.WebConstants.RESOURCE_ACCESS_CLAIM;
import static de.gematik.demis.notificationgateway.common.constants.WebConstants.RESOURCE_ACCESS_ROLES_ENTRY;
import static de.gematik.demis.notificationgateway.common.constants.WebConstants.TYP_CLAIM;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import de.gematik.demis.notificationgateway.security.oauth2.authentication.demis.DemisTokenRole;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Performs two different kind of validations, at two different times. <br>
 * One is executed by WebSecurity OAuth2 checks (= token decoding with JWK Set Url), proofing that
 * the Issuer Claim is one of the allowed ones, as per configuration. <br>
 * The second one is executed by a custom JWT Filter, performed at a later stage, to validated that
 * the JWT Token has the valid resources such as @RESOURCE_ACCESS_CLAIM or @PROFESSION_OID_CLAIM,
 * depending on token.
 */
@Slf4j
@Component
@Getter
public class UnifiedOAuth2TokenValidator implements OAuth2TokenValidator<Jwt> {

  private static final String ROLES_ACCESS_ERROR_CODE = "roles_access_error";
  // Same as specified in application.properties
  private static final String DEMIS_IDP = "demis";
  private static final String IBM_IDP = "ibm";
  private static final String PARSE_ERROR_CODE = "parse_error";
  private static final String INTERNAL_ERROR_CODE = "internal_error";
  private static final String PARSE_ERROR_MESSAGE = "Failed to parse JWT Token";
  private static final String INTERNAL_ERROR_MESSAGE =
      "An internal error has occurred while validating the Token";
  private static final String WRONG_ACCESS_ROLE_ERROR_CODE = "wrong_access_role";
  private static final String WRONG_ACCESS_ROLE_ERROR_MESSAGE =
      "Invalid access role defined in JWT Token";
  private static final String WRONG_ISSUER_ERROR_CODE = "issuer_error";
  private static final String ROLES_ACCESS_ERROR_MESSAGE = "wrong role specified";
  private static final String PROFESSION_ERROR_CODE = "profession_error";
  private static final String PROFESSION_ERROR_MESSAGE = "wrong profession_oid";
  private static final String CLAIM_TYPE_ERROR_CODE = "type_error";
  private static final String CLAIM_TYPE_ERROR_MESSAGE = "wrong type";
  private static final String INVALID_ACCESS_ROLE_FOR = "Invalid access role for {}";

  private final Map<String, List<String>> allowedIssuers = new HashMap<>();

  @NonNull private final OAuthIdpConfiguration idpConfiguration;

  public UnifiedOAuth2TokenValidator(@NonNull final OAuthIdpConfiguration idpConfiguration) {
    this.idpConfiguration = idpConfiguration;
    init();
  }

  /**
   * This function is called during the Spring Security check to validate the token against an
   * Issuer URL. It receives a JWT Token as input and checks that the Issuer URL stored in the claim
   * matches at least one of the known Issuer URLs, defined in the application properties..
   *
   * @param jwt the JWT Token to be validated.
   * @return an instance of {@link OAuth2TokenValidatorResult} containing success or failure.
   */
  @Override
  public OAuth2TokenValidatorResult validate(final Jwt jwt) {
    try {
      log.debug("JWT Issuer Validation - start");
      final JWTClaimsSet claimsSet = extractClaimSetFromToken(jwt);
      return validateTokenIssuer(claimsSet);
    } catch (final ParseException e) {
      return OAuth2TokenValidatorResult.failure(
          new OAuth2Error(PARSE_ERROR_CODE, PARSE_ERROR_MESSAGE, ""));
    } catch (final Exception e) {
      return OAuth2TokenValidatorResult.failure(
          new OAuth2Error(INTERNAL_ERROR_CODE, INTERNAL_ERROR_MESSAGE, ""));
    } finally {
      log.debug("JWT Issuer Validation - end");
    }
  }

  /**
   * Performs checks to validate that the JWT Token contains the correct role for the given URI,
   * depending on the configured IDP. If the @requestUri parameter is an empty string, the role-base
   * check will be skipped.
   *
   * @param authToken the raw JWT Token to be checked.
   * @param requestUri the URI for which the role-base access will be performed.
   * @return an instance of {@link OAuth2TokenValidatorResult} containing success or failure.
   */
  public OAuth2TokenValidatorResult validateClaimContent(
      final String authToken, final String requestUri) {
    try {
      final var jwtToken = JWTParser.parse(authToken);
      final var jwtClaims = jwtToken.getJWTClaimsSet();
      return validateAccessRights(jwtClaims, requestUri);
    } catch (final ParseException e) {
      log.error(e.getLocalizedMessage());
      return OAuth2TokenValidatorResult.failure(
          new OAuth2Error(PARSE_ERROR_CODE, PARSE_ERROR_MESSAGE, requestUri));
    } catch (final IllegalAccessException e) {
      log.error(e.getLocalizedMessage());
      return OAuth2TokenValidatorResult.failure(
          new OAuth2Error(
              WRONG_ACCESS_ROLE_ERROR_CODE, WRONG_ACCESS_ROLE_ERROR_MESSAGE, requestUri));
    }
  }

  private OAuth2TokenValidatorResult validateTokenIssuer(@NonNull final JWTClaimsSet jwtClaims)
      throws ParseException, NullPointerException {
    final var issuerClaim = jwtClaims.getIssuer();
    if (isClaimAllowedForDemis(issuerClaim)) {
      return OAuth2TokenValidatorResult.success();
    }

    if (isClaimAllowedForIbm(issuerClaim)) {
      return OAuth2TokenValidatorResult.success();
    }

    log.warn("Wrong Token Issuer detected: {}", issuerClaim);
    return OAuth2TokenValidatorResult.failure(
        new OAuth2Error(WRONG_ISSUER_ERROR_CODE, "wrong issuer: " + issuerClaim, ""));
  }

  private OAuth2TokenValidatorResult validateAccessRights(
      @NonNull final JWTClaimsSet jwtClaims, @NonNull final String requestUri)
      throws ParseException, IllegalAccessException {
    final var issuerClaim = jwtClaims.getIssuer();

    if (isClaimAllowedForDemis(issuerClaim)) {
      return validateDemisToken(jwtClaims, requestUri);
    }

    if (isClaimAllowedForIbm(issuerClaim)) {
      return validateIbmToken(jwtClaims);
    }

    log.warn("Validation failed: {}", issuerClaim);
    return OAuth2TokenValidatorResult.failure(
        new OAuth2Error(WRONG_ISSUER_ERROR_CODE, "wrong issuer: " + issuerClaim, requestUri));
  }

  private boolean isClaimAllowedForDemis(@NonNull final String issuerClaim) {
    return isClaimAllowedForIdp(issuerClaim, DEMIS_IDP);
  }

  private boolean isClaimAllowedForIbm(@NonNull final String issuerClaim) {
    return isClaimAllowedForIdp(issuerClaim, IBM_IDP);
  }

  private boolean isClaimAllowedForIdp(
      @NonNull final String issuerClaim, @NonNull final String idpName) {
    if (!allowedIssuers.containsKey(idpName)) {
      return false;
    }

    return allowedIssuers.get(idpName).stream().anyMatch(issuerClaim::contentEquals);
  }

  private OAuth2TokenValidatorResult validateDemisToken(
      @NonNull final JWTClaimsSet jwtClaimsSet, @NonNull final String requestUri)
      throws ParseException, IllegalAccessException, NullPointerException {

    final var accessRoles = getAccessRolesFromTokenClaim(jwtClaimsSet);

    if (requestUri.contains("laboratory")
        && !accessRoles.contains(DemisTokenRole.LAB_SENDER.getRoleName())) {
      log.warn(INVALID_ACCESS_ROLE_FOR, requestUri);
      return OAuth2TokenValidatorResult.failure(
          new OAuth2Error(ROLES_ACCESS_ERROR_CODE, ROLES_ACCESS_ERROR_MESSAGE, requestUri));
    }

    if (requestUri.contains("hospitalization")
        && !accessRoles.contains(DemisTokenRole.DISEASE_NOTIFICATION.getRoleName())) {
      log.warn(INVALID_ACCESS_ROLE_FOR, requestUri);
      return OAuth2TokenValidatorResult.failure(
          new OAuth2Error(ROLES_ACCESS_ERROR_CODE, ROLES_ACCESS_ERROR_MESSAGE, requestUri));
    }

    if (requestUri.contains("bedOccupancy")
        && !accessRoles.contains(DemisTokenRole.DISEASE_NOTIFICATION.getRoleName())) {
      log.warn(INVALID_ACCESS_ROLE_FOR, requestUri);
      return OAuth2TokenValidatorResult.failure(
          new OAuth2Error(ROLES_ACCESS_ERROR_CODE, ROLES_ACCESS_ERROR_MESSAGE, requestUri));
    }

    // No check for roles for other endpoints
    log.info("No access-role check necessary for URI {}", requestUri);
    return OAuth2TokenValidatorResult.success();
  }

  private OAuth2TokenValidatorResult validateIbmToken(@NonNull final JWTClaimsSet jwtClaimsSet)
      throws ParseException {
    final var ibmConfiguration =
        Objects.requireNonNull(idpConfiguration.getIbmIssuerConfiguration());

    final String professionOid = jwtClaimsSet.getStringClaim(PROFESSION_OID_CLAIM);
    if (Objects.nonNull(ibmConfiguration.getProfessionOIDs())
        && !ibmConfiguration.getProfessionOIDs().contains(professionOid)) {
      log.warn("Invalid Profession OID detected: {}", professionOid);
      final var professionError =
          new OAuth2Error(PROFESSION_ERROR_CODE, PROFESSION_ERROR_MESSAGE, professionOid);
      return OAuth2TokenValidatorResult.failure(professionError);
    }

    final String claimType = jwtClaimsSet.getStringClaim(TYP_CLAIM);
    if (!BEARER_TYPE.equalsIgnoreCase(claimType)) {
      log.warn("Invalid claim Type detected: {}", claimType);
      final var typeError =
          new OAuth2Error(CLAIM_TYPE_ERROR_CODE, CLAIM_TYPE_ERROR_MESSAGE, claimType);
      return OAuth2TokenValidatorResult.failure(typeError);
    }

    return OAuth2TokenValidatorResult.success();
  }

  private List<String> getAccessRolesFromTokenClaim(final JWTClaimsSet claimsSet)
      throws ParseException, IllegalAccessException {
    final var resourceAccess = claimsSet.getJSONObjectClaim(RESOURCE_ACCESS_CLAIM);
    if (!resourceAccess.containsKey(ALLOWED_RESOURCE_ACCESS_CLAIM)) {
      throw new IllegalAccessException(
          "Notification Gateway has no resource_access defined in claim");
    }

    final var accessRoles =
        (Map<String, List<String>>) resourceAccess.get(ALLOWED_RESOURCE_ACCESS_CLAIM);
    return accessRoles.get(RESOURCE_ACCESS_ROLES_ENTRY);
  }

  private static JWTClaimsSet extractClaimSetFromToken(final Jwt jwt) {
    final var claimBuilder = new JWTClaimsSet.Builder();
    jwt.getClaims().forEach(claimBuilder::claim);
    return claimBuilder.build();
  }

  /** Extracts all the allowed issuers from the configuration and stores them in a map. */
  private void init() {
    // short-naming
    final var ibmConfiguration = idpConfiguration.getIbmIssuerConfiguration();
    if (Objects.nonNull(ibmConfiguration)
        && Objects.nonNull(ibmConfiguration.getAllowedIssuers())
        && !ibmConfiguration.getAllowedIssuers().isEmpty()) {
      allowedIssuers.put(IBM_IDP, ibmConfiguration.getAllowedIssuers());
    }
    // short-naming
    final var demisConfiguration = idpConfiguration.getDemisIssuerConfiguration();
    if (Objects.nonNull(demisConfiguration)
        && Objects.nonNull(demisConfiguration.getAllowedIssuers())
        && !demisConfiguration.getAllowedIssuers().isEmpty()) {
      allowedIssuers.put(DEMIS_IDP, demisConfiguration.getAllowedIssuers());
    }
  }
}
