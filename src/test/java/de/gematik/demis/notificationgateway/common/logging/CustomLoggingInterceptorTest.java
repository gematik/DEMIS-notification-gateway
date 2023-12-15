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

package de.gematik.demis.notificationgateway.common.logging;

import static ch.qos.logback.classic.Level.INFO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

import ca.uhn.fhir.rest.client.apache.ApacheHttpRequest;
import ca.uhn.fhir.rest.client.apache.ApacheHttpResponse;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import ca.uhn.fhir.util.StopWatch;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHeader;
import org.assertj.core.groups.Tuple;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class CustomLoggingInterceptorTest {

  private final HttpClient mockHttpClient = mock(HttpClient.class);
  private final HttpResponse mockHttpResponse = mock(HttpResponse.class);
  private final StopWatch stopWatch = mock(StopWatch.class);
  private CustomLoggingInterceptor loggingInterceptor;

  private ListAppender<ILoggingEvent> listAppender;
  private IHttpResponse httpResponse;
  private IHttpRequest httpRequest;

  @BeforeEach
  public void init() {
    final BasicHeader[] headers =
        Arrays.array(
            new BasicHeader(AUTHORIZATION, "some token"),
            new BasicHeader(CONTENT_TYPE, "some content-type"));
    BasicHttpEntity httpEntity = mock(BasicHttpEntity.class);
    when(httpEntity.getContent())
        .thenReturn(IOUtils.toInputStream("some content", StandardCharsets.UTF_8));
    when(mockHttpResponse.getAllHeaders()).thenReturn(headers);
    when(mockHttpResponse.getEntity()).thenReturn(httpEntity);
    httpResponse = new ApacheHttpResponse(mockHttpResponse, stopWatch);

    HttpGet requestGet = new HttpGet();
    requestGet.setHeaders(headers);
    httpRequest = new ApacheHttpRequest(mockHttpClient, requestGet);

    loggingInterceptor = new CustomLoggingInterceptor();
    listAppender = new ListAppender<>();
    listAppender.start();
    Logger taskLogger = (Logger) LoggerFactory.getLogger(CustomLoggingInterceptor.class);
    taskLogger.addAppender(listAppender);
  }

  @Test
  void givenDisabledRequestDetailsWhenInterceptRequestThenDetailsNotLogged() {
    loggingInterceptor.setLogRequestSummary(false);
    loggingInterceptor.setLogRequestHeaders(false);

    loggingInterceptor.interceptRequest(httpRequest);

    assertThat(listAppender.list).isEmpty();
  }

  @Test
  void givenEnableRequestDetailsWhenInterceptRequestThenDetailsLogged() {
    loggingInterceptor.setLogRequestSummary(true);
    loggingInterceptor.setLogRequestHeaders(true);

    loggingInterceptor.interceptRequest(httpRequest);

    assertThat(listAppender.list)
        .hasSize(2)
        .extracting(ILoggingEvent::getFormattedMessage, ILoggingEvent::getLevel)
        .containsExactlyInAnyOrder(
            Tuple.tuple("Client request: GET null HTTP/1.1", INFO),
            Tuple.tuple(
                "Client request headers: 2\nAuthorization:[some token]\nContent-Type:[some content-type]",
                INFO));
  }

  @Test
  void
      givenEnableRequestDetailsAndExcludeAuthorizationWhenInterceptRequestThenDetailsLoggedAndAuthorizationMasked() {
    loggingInterceptor.setLogRequestSummary(true);
    loggingInterceptor.setLogRequestHeaders(true);
    loggingInterceptor.setExcludedRequestHeaders(Set.of("authorization"));

    loggingInterceptor.interceptRequest(httpRequest);

    assertThat(listAppender.list)
        .hasSize(2)
        .extracting(ILoggingEvent::getFormattedMessage, ILoggingEvent::getLevel)
        .containsExactlyInAnyOrder(
            Tuple.tuple("Client request: GET null HTTP/1.1", INFO),
            Tuple.tuple(
                "Client request headers: 2\nAuthorization:[masked]\nContent-Type:[some content-type]",
                INFO));
  }

  @Test
  void givenDisabledResponseDetailsWhenInterceptResponseThenDetailsNotLogged() throws IOException {
    loggingInterceptor.setLogResponseBody(false);
    loggingInterceptor.setLogResponseHeaders(false);

    loggingInterceptor.interceptResponse(httpResponse);

    assertThat(listAppender.list).isEmpty();
  }

  @Test
  void givenEnableResponseDetailsWhenInterceptResponseThenDetailsLogged() throws IOException {
    loggingInterceptor.setLogResponseBody(true);
    loggingInterceptor.setLogResponseHeaders(true);

    loggingInterceptor.interceptResponse(httpResponse);

    assertThat(listAppender.list)
        .hasSize(2)
        .extracting(ILoggingEvent::getFormattedMessage, ILoggingEvent::getLevel)
        .containsExactlyInAnyOrder(
            Tuple.tuple("Client response body:\nsome content", INFO),
            Tuple.tuple(
                "Client response headers: 2\nauthorization:[some token]\ncontent-type:[some content-type]",
                INFO));
  }

  @Test
  void
      givenEnableResponseDetailsAndExcludeAuthorizationWhenInterceptResponseThenDetailsLoggedAndAuthorizationMasked()
          throws IOException {
    loggingInterceptor.setLogResponseBody(true);
    loggingInterceptor.setLogResponseHeaders(true);
    loggingInterceptor.setExcludedRequestHeaders(Set.of("authorization"));

    loggingInterceptor.interceptResponse(httpResponse);

    assertThat(listAppender.list)
        .hasSize(2)
        .extracting(ILoggingEvent::getFormattedMessage, ILoggingEvent::getLevel)
        .containsExactlyInAnyOrder(
            Tuple.tuple("Client response body:\nsome content", INFO),
            Tuple.tuple(
                "Client response headers: 2\nauthorization:[masked]\ncontent-type:[some content-type]",
                INFO));
  }
}
