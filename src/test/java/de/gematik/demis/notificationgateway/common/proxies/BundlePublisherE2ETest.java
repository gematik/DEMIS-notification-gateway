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

package de.gematik.demis.notificationgateway.common.proxies;

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

import static de.gematik.demis.notificationgateway.common.enums.SupportedRealm.HOSPITAL;
import static de.gematik.demis.notificationgateway.common.enums.SupportedRealm.LAB;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import de.gematik.demis.notificationgateway.BaseTestUtils;
import de.gematik.demis.notificationgateway.common.properties.NESProperties;
import de.gematik.demis.notificationgateway.common.properties.RPSProperties;
import de.gematik.demis.notificationgateway.common.request.Metadata;
import de.gematik.demis.notificationgateway.common.utils.Reachability;
import de.gematik.demis.notificationgateway.utils.FileUtils;
import jakarta.security.auth.message.AuthException;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assumptions;
import org.assertj.core.api.ThrowableAssert;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.EnabledIf;

@SpringBootTest(properties = {"feature.flag.specimen.preparation.enabled=false"})
@ActiveProfiles("test")
@EnabledIf(expression = "${testing.enable-e2e}", loadContext = true)
@Slf4j
class BundlePublisherE2ETest implements BaseTestUtils {

  @Autowired private BundlePublisher bundlePublisher;
  @Autowired private NESProperties nesProperties;
  @Autowired private RPSProperties rpsProperties;
  @MockBean private Metadata metadata;

  @BeforeEach
  void assumeReachableNes() {
    String url = this.nesProperties.getBaseUrl();
    Assumptions.assumeThat(new Reachability().test(url))
        .as("DEMIS DEV-stage NES is reachable")
        .isTrue();
  }

  @Test
  void testPostRequestLaboratoryBundleToNotificationApi() throws AuthException {
    final Parameters input =
        FileUtils.createParametersFromFile("parameters_laboratorybundle_v2_testuser.json");

    final Parameters parameters =
        bundlePublisher.postRequest(
            (Bundle) input.getParameter().get(0).getResource(),
            LAB,
            nesProperties.laboratoryUrl(),
            NESProperties.OPERATION_NAME,
            "rki.demis.r4.core",
            "1.24.0",
            metadata);

    assertNotNull(parameters);

    final Optional<Resource> operationOutcomeOptional =
        parameters.getParameter().stream()
            .filter(parameter -> parameter.getName().equals("operationOutcome"))
            .map(ParametersParameterComponent::getResource)
            .findFirst();

    assertNotNull(operationOutcomeOptional);
    assertTrue(operationOutcomeOptional.isPresent());

    final OperationOutcome operationOutcome = (OperationOutcome) operationOutcomeOptional.get();
    final List<OperationOutcomeIssueComponent> issues = operationOutcome.getIssue();

    assertEquals(1, issues.size());
    assertEquals("All OK", issues.get(0).getDetails().getText());
  }

  @Test
  void testPostLaboratoryBundleWithUnsupportedPostalCodeThrowsException() {
    final Parameters input =
        FileUtils.createParametersFromFile(
            "parameters_laboratorybundle_v2_unsupported_postal_code.json");
    final ThrowableAssert.ThrowingCallable throwingCallable =
        () ->
            bundlePublisher.postRequest(
                (Bundle) input.getParameter().get(0).getResource(),
                LAB,
                nesProperties.laboratoryUrl(),
                NESProperties.OPERATION_NAME,
                "rki.demis.r4.core",
                "1.24.0",
                metadata);

    assertThatThrownBy(throwingCallable)
        .isInstanceOf(UnprocessableEntityException.class)
        .hasMessage("HTTP 422 Unprocessable Entity: Notifier and notifier facility not available");
  }

  @Test
  void testPostRequestDiseaseBundleToNES() throws AuthException {
    final Parameters input =
        FileUtils.createParametersFromFile("parameters_diseasebundle_v2_testuser.json");

    final Parameters parameters =
        bundlePublisher.postRequest(
            (Bundle) input.getParameter().get(0).getResource(),
            LAB,
            nesProperties.hospitalizationUrl(),
            NESProperties.OPERATION_NAME,
            "rki.demis.r4.core",
            "1.24.0",
            metadata);

    assertNotNull(parameters);

    final Optional<Resource> operationOutcomeOptional =
        parameters.getParameter().stream()
            .filter(parameter -> parameter.getName().equals("operationOutcome"))
            .map(ParametersParameterComponent::getResource)
            .findFirst();

    assertNotNull(operationOutcomeOptional);
    assertTrue(operationOutcomeOptional.isPresent());

    final OperationOutcome operationOutcome = (OperationOutcome) operationOutcomeOptional.get();
    final List<OperationOutcomeIssueComponent> issues = operationOutcome.getIssue();

    assertEquals(1, issues.size());
    assertEquals("All OK", issues.get(0).getDetails().getText());
  }

  @Test
  void testPostRequestBedOccupancyReportToRPS() throws AuthException {
    final Parameters input =
        FileUtils.createParametersFromFile("parameters_report_bedoccupancy.json");
    final Parameters parameters =
        bundlePublisher.postRequest(
            (Bundle) input.getParameter().get(0).getResource(),
            HOSPITAL,
            rpsProperties.bedOccupancyUrl(),
            RPSProperties.OPERATION_NAME,
            "rki.demis.r4.core",
            "1.24.0",
            metadata);

    assertNotNull(parameters);

    final Optional<Resource> operationOutcomeOptional =
        parameters.getParameter().stream()
            .filter(parameter -> parameter.getName().equals("operationOutcome"))
            .map(ParametersParameterComponent::getResource)
            .findFirst();

    assertNotNull(operationOutcomeOptional);
    assertTrue(operationOutcomeOptional.isPresent());

    final OperationOutcome operationOutcome = (OperationOutcome) operationOutcomeOptional.get();
    final List<OperationOutcomeIssueComponent> issues = operationOutcome.getIssue();

    assertEquals(1, issues.size());
    assertEquals("All OK", issues.get(0).getDetails().getText());
  }
}
