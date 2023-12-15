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

package de.gematik.demis.notificationgateway.integration.hooks;

import de.gematik.demis.enums.KeyStoreType;
import de.gematik.demis.notificationgateway.integration.PropertiesReader;
import de.gematik.demis.notificationgateway.integration.abilities.HospitalizationAbility;
import de.gematik.demis.notificationgateway.integration.builder.HospitalizationNotificationBuilder;
import de.gematik.demis.token.data.KeyStoreConfigParameter;
import de.gematik.demis.token.data.RequestParameters;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Objects;
import java.util.Properties;
import javax.net.ssl.*;
import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.actors.OnStage;
import net.serenitybdd.screenplay.actors.OnlineCast;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

@Slf4j
public class Hook {
  private String accessToken;
  private Properties properties;

  // TODO: funktion implementieren, welche proxy setzt
  public void getToken() {}

  public void getHopitalizationAccessTokenViaRA()
      throws IOException,
          NoSuchAlgorithmException,
          KeyStoreException,
          KeyManagementException,
          CertificateException,
          UnrecoverableKeyException {

    String clientCertificatePath = "certs/ClientCertificate.p12";
    String trustStorePath = "C:/Program Files/Java/jre1.8.0_91/lib/security/cacerts";
    String trustStorePassword = "changeit"; // default trust store password
    String clientPassword = "";

    KeyStore clientStore = KeyStore.getInstance("PKCS12");

    clientStore.load(new FileInputStream(clientCertificatePath), clientPassword.toCharArray());

    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(clientStore, clientPassword.toCharArray());
    KeyManager[] kms = kmf.getKeyManagers();

    KeyStore trustStore = KeyStore.getInstance("JKS");
    trustStore.load(new FileInputStream(trustStorePath), trustStorePassword.toCharArray());

    TrustManagerFactory tmf =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(trustStore);
    TrustManager[] tms = tmf.getTrustManagers();

    SSLContext sslContext = null;
    sslContext = SSLContext.getInstance("TLS");
    sslContext.init(kms, tms, new SecureRandom());

    SSLSocketFactory lSchemeSocketFactory = null;

    //        lSchemeSocketFactory = new SSLSocketFactory(clientStore, clientPassword, trustStore);
    //
    //        // configure Rest Assured
    //        RestAssured.config =
    // RestAssured.config().sslConfig(sslConfig().with().sslSocketFactory(lSchemeSocketFactory).and().allowAllHostnames());
  }

  public void getHopitalizationAccessToken() {
    PropertiesReader reader = new PropertiesReader();
    properties = reader.readProperties();
    CloseableHttpClient client = HttpClients.createDefault();

    String clientId = "demis-test";
    String secret = "secret_client_secret";
    String username = properties.getProperty("0.idp.hospital.username");
    String password = properties.getProperty("0.idp.hospital.authcertpassword");
    String certAlias = properties.getProperty("0.idp.hospital.authcertalias");

    InputStream keyStoreStream = null;
    InputStream trustStoreStream = null;
    InputStream authCertKeyStoreStream = null;

    keyStoreStream =
        Hook.class.getResourceAsStream("cert/lab/gematik/DEMIS-test-int_CSM020263412.p12");
    trustStoreStream = Hook.class.getResourceAsStream("cert/lab/truststore.jks");
    authCertKeyStoreStream = Hook.class.getResourceAsStream("");

    RequestParameters requestParameters = new RequestParameters(clientId, username, secret);
    var keyStoreConfigParameter =
        KeyStoreConfigParameter.builder()
            .authCertKeyStore(Objects.requireNonNull(keyStoreStream))
            .authCertKeyStoreType(KeyStoreType.PKCS12)
            .trustStorePassword(password)
            .authCertAlias(certAlias)
            .trustStore(Objects.requireNonNull(trustStoreStream))
            .authCertKeyStore(Objects.requireNonNull(authCertKeyStoreStream));

    String tokenEndpoint =
        "https://demis-int.rki.de/auth/realms/HOSPITAL/protocol/openid-connect/token";
    //        accessToken = tokenService.getToken(client, requestParameters,
    // keyStoreConfigParameter, tokenEndpoint);
  }

  @Before
  public void init() {
    OnStage.setTheStage(new OnlineCast());
    //        getHopitalizationAccessToken();
  }

  @Before(value = "@customDataDriven")
  public void prepareValidTestData() {
    OnStage.setTheStage(new OnlineCast());
    HospitalizationNotificationBuilder notificationBuilder =
        HospitalizationNotificationBuilder.build();
    var hospitalizationWrapper = notificationBuilder.createPositiveHospitalizationNotification();

    Actor automatica = OnStage.theActor("Testautomat");
    automatica.can(hospitalizationWrapper);
    automatica.abilityTo(HospitalizationAbility.class);

    // TODO: weitere Capability für 'accessToken' erstellen

  }

  @After
  public void tearDown() {
    OnStage.drawTheCurtain();
  }
}
