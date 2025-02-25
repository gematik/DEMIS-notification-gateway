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

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;

/**
 * Disables and enables certificate and host-name checking in HttpsURLConnection, the default JVM
 * implementation of the HTTPS/TLS protocol. Has no effect on implementations such as Apache Http
 * Client, Ok Http.
 */
public final class TlsVerifierConfigurator {

  public static final HostnameVerifier NOOP_HOSTNAME_VERIFIER = new NoopHostnameVerifier();
  private static final TrustManager[] TRUST_MANAGERS =
      new TrustManager[] {
        new X509TrustManager() {
          public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null; // NOSONAR
          }

          public void checkClientTrusted(X509Certificate[] certs, String authType) { // NOSONAR
          }

          public void checkServerTrusted(X509Certificate[] certs, String authType) { // NOSONAR
          }
        }
      };

  private TlsVerifierConfigurator() {}

  /**
   * Creates an unsafe {@link SSLConnectionSocketFactory} object to skip the hostname and
   * certificate validation, useful for self-signed certificates.
   *
   * @return an instance of {@link SSLConnectionSocketFactory}
   * @throws KeyManagementException in case of errors with the trust manager
   * @throws NoSuchAlgorithmException in case of unrecognized algorithm
   */
  public static SSLConnectionSocketFactory createUnsafeLayeredSecureSocketFactory()
      throws KeyManagementException, NoSuchAlgorithmException {
    SSLContext sc = SSLContext.getInstance("TLS"); // NOSONAR
    sc.init(null, TRUST_MANAGERS, null);
    return new SSLConnectionSocketFactory(sc, NOOP_HOSTNAME_VERIFIER);
  }

  public static HttpClientConnectionManager createUnsafeConnectionManager()
      throws KeyManagementException, NoSuchAlgorithmException {
    return PoolingHttpClientConnectionManagerBuilder.create()
        .setSSLSocketFactory(createUnsafeLayeredSecureSocketFactory())
        .build();
  }

  public static SSLSocketFactory createUnsafeSecureSocketFactory()
      throws NoSuchAlgorithmException, KeyManagementException {
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(new KeyManager[] {}, TRUST_MANAGERS, null);
    return sslContext.getSocketFactory();
  }
}
