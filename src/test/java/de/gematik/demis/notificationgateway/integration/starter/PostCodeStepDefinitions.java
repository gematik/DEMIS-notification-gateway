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

package de.gematik.demis.notificationgateway.integration.starter;

import static net.serenitybdd.rest.SerenityRest.restAssuredThat;
import static org.hamcrest.Matchers.equalTo;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import net.thucydides.core.annotations.Steps;

public class PostCodeStepDefinitions {

  @Steps NotificationGatewayAPI notificationGatewayAPI;

  @When("I look up a post code {word} for country code {word}")
  public void lookUpAPostCode(String postCode, String country) {
    notificationGatewayAPI.fetchLocationByPostCodeAndCountry(postCode, country);
  }

  @Then("the resulting location should be {} in {}")
  public void theResultingLocationShouldBe(String placeName, String country) {
    restAssuredThat(response -> response.statusCode(200));
    restAssuredThat(response -> response.body(LocationResponse.COUNTRY, equalTo(country)));
    restAssuredThat(
        response -> response.body(LocationResponse.FIRST_PLACE_NAME, equalTo(placeName)));
  }
}
