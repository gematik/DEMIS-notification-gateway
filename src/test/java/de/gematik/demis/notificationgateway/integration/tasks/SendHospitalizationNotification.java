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

package de.gematik.demis.notificationgateway.integration.tasks;

import static io.restassured.RestAssured.given;
import static net.serenitybdd.screenplay.Tasks.instrumented;

import de.gematik.demis.notificationgateway.integration.abilities.HospitalizationAbility;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Performable;
import net.serenitybdd.screenplay.Task;

@Slf4j
public class SendHospitalizationNotification implements Task {
  private static final String ENDPOINT = "api/ng/notification/hospitalization";
  private HospitalizationAbility hospitalization;

  public SendHospitalizationNotification(HospitalizationAbility hospitalization) {
    this.hospitalization = hospitalization;
  }

  public static Performable createHospitalizationNotification(
      HospitalizationAbility hospitalizationNotification) {
    return instrumented(SendHospitalizationNotification.class, hospitalizationNotification);
  }

  @Override
  public <T extends Actor> void performAs(T actor) {
    String lAccessToken =
        "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJsS2VZeHdrbTBFdEN5eTJLUWhPZ0lvelZRYWlHX1BRZGZobFZOeW5yTTVFIn0.eyJleHAiOjE2NTgxNDYzODQsImlhdCI6MTY1ODE0NTc4NCwianRpIjoiNWZlNzJmNWUtNTQxYi00Mzc5LTkxZjgtYTQ4MjQyMjJhMWQyIiwiaXNzIjoiaHR0cHM6Ly9kZW1pcy1pbnQucmtpLmRlL2F1dGgvcmVhbG1zL0hPU1BJVEFMIiwiYXVkIjoibm90aWZpY2F0aW9uLWVudHJ5LXNlcnZpY2UiLCJzdWIiOiI1MzgwNTYwNS1mNDk1LTQ1YzUtYTNkYS1iMTA1ZmQ2M2RhMGMiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJkZW1pcy10ZXN0Iiwic2Vzc2lvbl9zdGF0ZSI6IjFmYmZlMWI2LWQxYTYtNDFmNi1hMjU4LTE5ZmUyYTYyNzA0NSIsImFjciI6IjEiLCJyZXNvdXJjZV9hY2Nlc3MiOnsibm90aWZpY2F0aW9uLWVudHJ5LXNlcnZpY2UiOnsicm9sZXMiOlsibGFiLW5vdGlmaWNhdGlvbi1zZW5kZXIiLCJkaXNlYXNlLW5vdGlmaWNhdGlvbi1zZW5kZXIiXX19LCJzY29wZSI6InByb2ZpbGUiLCJzaWQiOiIxZmJmZTFiNi1kMWE2LTQxZjYtYTI1OC0xOWZlMmE2MjcwNDUiLCJvcmdhbml6YXRpb24iOiJnZW1hdGlrIC0gU29mdHdhcmUgRGV2ZWxvcG1lbnQiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJ0ZXN0LWludCJ9.na23rbfChjj9CJD5ivyKmsPyeerAucdUlKFxoih38ESTS2M7OFcLfZQciI-3LkIiRPzv4Urt2Pp5c3nmS8sZ57C_tEiaLaYLjXQCb9OVNsJ9keed09ibefkNkBXrV1JSct17wf7wCK-gUpj2vl79C0OKBwSTxD1vpCJemdTD9rBdQw2ZfCEIIWrvIChyhaJFyYL3MRmpBWHmkJk-yWQ52ZiMbCMBQ5q0R1WgwZ2K0HnNSIwz92LcBZE_qiRIJAwfiuSlT2sx_pNQBmtvdxodGCyQEW_lhgHCZkdBGlX2puTD0NdfswGA7Y_t1nzor6uRKKUSVpIq26u3lpWpctoXuw";

    RestAssured.baseURI = "https://146.185.106.154/";

    //        log.info(hospitalization.getHospitalization().toString());

    Response response =
        given()
            .header("Content-type", "application/json")
            .header("Authorization", lAccessToken)
            .and()
            .body(hospitalization)
            .when()
            .post(ENDPOINT)
            .then()
            .extract()
            .response();

    response.getStatusCode();

    actor.remember("Gesendeter Body", hospitalization);
  }
}
