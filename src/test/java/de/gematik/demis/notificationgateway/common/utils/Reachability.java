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
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;

/** Reachability check of given URLs */
@Slf4j
public final class Reachability implements Predicate<String> {

  private static final int CONNECT_TIMEOUT_MILLIS = 10 * 1000;

  private static URL parseUrl(String url) {
    try {
      return new URI(url).toURL();
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to parse URL: " + url, e);
    }
  }

  /**
   * Checks reachability of given URL.
   *
   * @param url URL as text
   * @return <code>true</code> if URL is reachable, <code>false</code> if URL is not reachable
   */
  @Override
  public boolean test(String url) {
    URL parsedUrl = parseUrl(url);
    int port = parsedUrl.getPort();
    if (port == -1) {
      log.info("Using default port 443");
      port = 443;
    }
    try (Socket tcp = new Socket()) {
      tcp.connect(new InetSocketAddress(parsedUrl.getHost(), port), CONNECT_TIMEOUT_MILLIS);
      return true;
    } catch (Exception e) {
      log.error(
          "URL to check was not reachable! Url: {} Timeout: {}s",
          url,
          CONNECT_TIMEOUT_MILLIS / 1000,
          e);
      return false;
    }
  }
}
