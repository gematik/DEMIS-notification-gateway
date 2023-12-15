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

package de.gematik.demis.notificationgateway.integration.stepdefs;

import static net.serenitybdd.rest.SerenityRest.restAssuredThat;
import static net.serenitybdd.screenplay.actors.OnStage.theActorCalled;

import de.gematik.demis.notificationgateway.integration.abilities.HospitalizationAbility;
import de.gematik.demis.notificationgateway.integration.tasks.SendHospitalizationNotification;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.rest.abilities.CallAnApi;
import net.thucydides.core.util.EnvironmentVariables;

public class HospitalizationStepDefinition {
  private EnvironmentVariables environmentVariables;

  @Given("^(.*) sends hospitalization notification with valid data and format for all fields")
  public void testautomatSendsHospitalizationNotificationWithValidDataAndFormatForAllFields(
      String actorName) {
    Actor automatica = theActorCalled(actorName);
    // .. Nutze deine Fähigkeit und hole Hospitalization raus, um es zu versenden
    var hospitalization = automatica.usingAbilityTo(HospitalizationAbility.class);

    automatica
        .whoCan(CallAnApi.at(environmentVariables.getProperty("api.int.url")))
        .attemptsTo(
            SendHospitalizationNotification.createHospitalizationNotification(hospitalization));
  }

  @Then("I expect positive http-response code")
  public void iExpectPositiveHttpResponseCode() {
    restAssuredThat(response -> response.statusCode(200));
  }

  @And("I expect my data in the outgoing FHIR-Request")
  public void iExpectMyDataInTheOutgoingFHIRRequest() {}
}
