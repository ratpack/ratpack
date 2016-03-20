/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.handling.internal

import io.netty.handler.codec.http.DefaultHttpHeaders
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
import org.apache.logging.log4j.junit.LoggerContextRule
import org.apache.logging.log4j.test.appender.ListAppender
import org.apache.logging.slf4j.Log4jLogger
import org.junit.ClassRule
import org.slf4j.Logger
import org.slf4j.MarkerFactory
import ratpack.handling.RequestId
import ratpack.handling.RequestOutcome
import ratpack.handling.UserId
import ratpack.http.MutableHeaders
import ratpack.http.Request
import ratpack.http.SentResponse
import ratpack.http.Status
import ratpack.http.internal.DefaultRequest
import ratpack.http.internal.DefaultSentResponse
import ratpack.http.internal.DefaultStatus
import ratpack.http.internal.HttpHeaderConstants
import ratpack.http.internal.NettyHeadersBackedMutableHeaders
import ratpack.registry.MutableRegistry
import ratpack.registry.internal.SimpleMutableRegistry
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.time.Instant

@Subject(NcsaRequestLogger)
class NcsaRequestLoggerSpec extends Specification {

  private static final Long TEST_TIMESTAMP = 1458250293458L
  private static final String INFO_LOGGER_NAME = 'ratpack.request.info'
  private static final String WARN_LOGGER_NAME = 'ratpack.request.warn'
  private static final String WARN_5XX_LOGGER_NAME = 'ratpack.request.warn.5xx'
  private static final String ERROR_LOGGER_NAME = 'ratpack.request.error'

  @Shared
  private Locale originalDefaultLocale

  @Shared
  private TimeZone originalDefaultTimeZone

  @ClassRule
  @Shared
  public LoggerContextRule loggerContextRule = new LoggerContextRule('classpath:ratpack/handling/internal/logging.xml')

  @Shared
  private ListAppender requestListAppender

  @Shared
  private ListAppender serverErrorRequestListAppender

  @Shared
  private ListAppender errorRequestListAppender

  def setupSpec() {
    // save original state of locale and timezone
    originalDefaultLocale = Locale.getDefault()
    originalDefaultTimeZone = TimeZone.getDefault()

    // init appenders
    requestListAppender = loggerContextRule.getListAppender('requestList')
    serverErrorRequestListAppender = loggerContextRule.getListAppender('serverErrorRequestList')
    errorRequestListAppender = loggerContextRule.getListAppender('errorRequestList')
  }

  def setup() {
    restoreOriginalLocaleAndTimeZone()
    clearAllListAppenders()
  }

  def cleanupSpec() {
    restoreOriginalLocaleAndTimeZone()
  }

  @Unroll
  def 'timestamp is formatted correctly with locale "#locale" and timezone "#zoneId".'() {
    setup:
    def logger = retrieveLogger(INFO_LOGGER_NAME)
    Locale.setDefault(locale)
    TimeZone.setDefault(TimeZone.getTimeZone(zoneId))
    def instance = new NcsaRequestLogger(logger)

    when:
    def timestamp = instance.getTimestampString(instant)

    then:
    timestamp == expectedResult

    where:
    instant                              | locale         | zoneId             || expectedResult
    Instant.ofEpochMilli(TEST_TIMESTAMP) | Locale.US      | 'America/New_York' || '17/Mar/2016:17:31:33 -0400'
    Instant.ofEpochMilli(TEST_TIMESTAMP) | Locale.FRANCE  | 'America/New_York' || '17/Mar/2016:17:31:33 -0400'
    Instant.ofEpochMilli(TEST_TIMESTAMP) | Locale.GERMANY | 'America/New_York' || '17/Mar/2016:17:31:33 -0400'

    Instant.ofEpochMilli(TEST_TIMESTAMP) | Locale.US      | 'Europe/Berlin'    || '17/Mar/2016:22:31:33 +0100'
    Instant.ofEpochMilli(TEST_TIMESTAMP) | Locale.FRANCE  | 'Europe/Berlin'    || '17/Mar/2016:22:31:33 +0100'
    Instant.ofEpochMilli(TEST_TIMESTAMP) | Locale.GERMANY | 'Europe/Berlin'    || '17/Mar/2016:22:31:33 +0100'
  }

  /*
   * This test is not related to NcsaRequestLogger at all.
   * It just validates that the testing logger configuration behaves as expected.
   * Which it doesn't. But anyway...
   */
  def 'logger configuration sanity checks'() {
    setup:
    def infoLogger = retrieveLogger(INFO_LOGGER_NAME)
    def warnLogger = retrieveLogger(WARN_LOGGER_NAME)
    def warn5xxLogger = retrieveLogger(WARN_5XX_LOGGER_NAME)
    def errorLogger = retrieveLogger(ERROR_LOGGER_NAME)
    def m4 = MarkerFactory.getMarker(NcsaRequestLogger.STATUS_4XX_MARKER_NAME)
    def m5 = MarkerFactory.getMarker(NcsaRequestLogger.STATUS_5XX_MARKER_NAME)

    expect:
    infoLogger.isWarnEnabled()
    infoLogger.isInfoEnabled()
    !infoLogger.isDebugEnabled()
    infoLogger.isWarnEnabled(m5)
    infoLogger.isWarnEnabled(m4)

    warnLogger.isWarnEnabled()
    !warnLogger.isInfoEnabled()
    !warnLogger.isDebugEnabled()
    warnLogger.isWarnEnabled(m5)
    warnLogger.isWarnEnabled(m4)

    warn5xxLogger.isWarnEnabled()
    !warn5xxLogger.isInfoEnabled()
    !warn5xxLogger.isDebugEnabled()
    warn5xxLogger.isWarnEnabled(m5)

    // http://stackoverflow.com/questions/22966475/log4j2-marker-and-isenabled
    // https://issues.apache.org/jira/browse/LOG4J2-601
    //
    // log4j2 isXxxEnabled(Marker) methods are essentially broken since they
    // only work if the MarkerFilters are defined at the Configuration level
    //
    // The actual logging calls will work as expected, though.

    // !warn5xxLogger.isWarnEnabled(m4)
    // ^^^ this should work but doesn't ^^^

    !errorLogger.isWarnEnabled()
    !errorLogger.isInfoEnabled()
    !errorLogger.isDebugEnabled()
    !errorLogger.isWarnEnabled(m5)
    !errorLogger.isWarnEnabled(m4)
  }

  def 'missing logger causes correct exception in constructor'() {
    when:
    new NcsaRequestLogger(null)

    then:
    NullPointerException ex = thrown()
    ex.message == 'logger must not be null!'
  }

  // helper methods below
  private Logger retrieveLogger(String loggerName) {
    final org.apache.logging.log4j.core.Logger log4jLogger = loggerContextRule.getLogger(loggerName)
    return new Log4jLogger(log4jLogger, loggerName)
  }

  private void restoreOriginalLocaleAndTimeZone() {
    Locale.setDefault(originalDefaultLocale)
    TimeZone.setDefault(originalDefaultTimeZone)
  }

  private void clearAllListAppenders() {
    requestListAppender.clear()
    serverErrorRequestListAppender.clear()
    errorRequestListAppender.clear()
  }

  private static RequestOutcome createRequestOutcome(String host, String user, long timestamp, String httpMethod, String path, String httpVersion, int statusCode, long responseSize, String requestId) {
    Instant instant = Instant.ofEpochMilli(timestamp)
    InetSocketAddress remoteSocket = InetSocketAddress.createUnresolved(host, 0)

    // prepare request
    Request request = new DefaultRequest(
      instant,                          // timestamp
      null,                             // headers
      HttpMethod.valueOf(httpMethod),   // method
      HttpVersion.valueOf(httpVersion), // protocol
      '/' + path,                       // rawUri
      remoteSocket,                     // remoteSocket
      null,                             // localSocket
      null,                             // serverConfig
      null                              // bodyReader
    )

    MutableRegistry registry = new SimpleMutableRegistry()
    if (user) {
      registry.add(UserId, new DefaultUserId(user))
    }
    if (requestId) {
      registry.add(RequestId, new DefaultRequestId(requestId))
    }

    DefaultRequest.setDelegateRegistry(request, registry)

    // prepare response
    Status status = new DefaultStatus(HttpResponseStatus.valueOf(statusCode))
    MutableHeaders responseHeaders = new NettyHeadersBackedMutableHeaders(new DefaultHttpHeaders())
    if (responseSize >= 0) {
      responseHeaders.add(HttpHeaderConstants.CONTENT_LENGTH, String.valueOf(responseSize))
    }
    SentResponse sentResponse = new DefaultSentResponse(responseHeaders, status)

    return new DefaultRequestOutcome(request, sentResponse, instant)
  }
}
