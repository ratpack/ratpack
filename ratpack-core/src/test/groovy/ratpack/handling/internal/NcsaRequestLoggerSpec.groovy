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
import org.apache.logging.log4j.Level
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
import ratpack.registry.internal.DefaultMutableRegistry
import ratpack.server.internal.DefaultServerConfigBuilder
import ratpack.server.internal.ServerEnvironment
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


  @Unroll
  def '"#expectedMessage" is generated correctly using message pattern "#expectedPattern".'() {
    setup:
    def logger = retrieveLogger(INFO_LOGGER_NAME)
    TimeZone.setDefault(TimeZone.getTimeZone('America/New_York'))

    def requestOutcome = createRequestOutcome(host, user, timestamp, httpMethod, path, httpVersion, statusCode, responseSize, requestId)
    def instance = new NcsaRequestLogger(logger)
    def events = requestListAppender.getEvents()


    when:
    instance.log(requestOutcome)


    then:
    events.size() == 1

    def event = events[0]
    event.message.formattedMessage == expectedMessage
    event.message.format == expectedPattern


    where:
    host        | user        | timestamp      | httpMethod | path         | httpVersion | statusCode | responseSize | requestId    | expectedMessage
    '127.0.0.1' | null        | TEST_TIMESTAMP | 'GET'      | 'huhu'       | 'HTTP/1.1'  | 100        | -1           | null         | '127.0.0.1 - - [17/Mar/2016:17:31:33 -0400] "GET /huhu HTTP/1.1" 100 -'
    '127.0.0.1' | null        | TEST_TIMESTAMP | 'PUT'      | 'foo'        | 'HTTP/1.1'  | 200        | -1           | null         | '127.0.0.1 - - [17/Mar/2016:17:31:33 -0400] "PUT /foo HTTP/1.1" 200 -'
    '127.0.0.1' | null        | TEST_TIMESTAMP | 'POST'     | 'bar'        | 'HTTP/1.1'  | 300        | -1           | 'request-id' | '127.0.0.1 - - [17/Mar/2016:17:31:33 -0400] "POST /bar HTTP/1.1" 300 - id=request-id'
    '127.0.0.1' | 'dnadams'   | TEST_TIMESTAMP | 'GET'      | 'coffee'     | 'HTTP/1.1'  | 418        | 42           | null         | '127.0.0.1 - dnadams [17/Mar/2016:17:31:33 -0400] "GET /coffee HTTP/1.1" 418 42'
    '127.0.0.1' | 'rbradbury' | TEST_TIMESTAMP | 'GET'      | 'fahrenheit' | 'HTTP/1.1'  | 451        | -1           | null         | '127.0.0.1 - rbradbury [17/Mar/2016:17:31:33 -0400] "GET /fahrenheit HTTP/1.1" 451 -'
    '127.0.0.1' | 'sfalken'   | TEST_TIMESTAMP | 'GET'      | 'wopr'       | 'HTTP/1.1'  | 500        | 0            | 'joshua'     | '127.0.0.1 - sfalken [17/Mar/2016:17:31:33 -0400] "GET /wopr HTTP/1.1" 500 0 id=joshua'

    expectedPattern = requestId ? NcsaRequestLogger.MESSAGE_PATTERN_WITH_ID : NcsaRequestLogger.MESSAGE_PATTERN_WITHOUT_ID
  }

  @Unroll
  def '#statusCode is logged at level "#expectedLevel" with error status codes emitted as "#logLevel"'() {
    setup:
    def logger = retrieveLogger(INFO_LOGGER_NAME)
    TimeZone.setDefault(TimeZone.getTimeZone('America/New_York'))

    def requestOutcome = createRequestOutcome(statusCode)
    def instance = new NcsaRequestLogger(logger, warnLogging)
    def events = requestListAppender.getEvents()


    when:
    instance.log(requestOutcome)


    then:
    events.size() == 1

    def event = events[0]
    event.level == expectedLevel


    where:
    statusCode | warnLogging
    99         | true
    99         | false
    100        | true
    100        | false
    199        | true
    199        | false
    200        | true
    200        | false
    299        | true
    299        | false
    300        | true
    300        | false
    399        | true
    399        | false
    400        | true
    400        | false
    499        | true
    499        | false
    500        | true
    500        | false
    599        | true
    599        | false
    600        | true
    600        | false

    expectedLevel = resolveExpectedLevel(statusCode, warnLogging)
    logLevel = warnLogging ? 'WARN' : 'INFO'
  }

  @Unroll
  def 'event with status #statusCode is routed correctly using logger "#loggerName" with error status codes emitted as "#logLevel"'() {
    setup:
    TimeZone.setDefault(TimeZone.getTimeZone('America/New_York'))

    def logger = retrieveLogger(loggerName)
    def instance = new NcsaRequestLogger(logger, warnLogging)

    def requestOutcome = createRequestOutcome(statusCode)


    when:
    instance.log(requestOutcome)


    then:
    if (inInfo) {
      assert requestListAppender.getEvents().size() == 1
    } else {
      assert requestListAppender.getEvents().size() == 0
    }

    if (inServerError) {
      assert serverErrorRequestListAppender.getEvents().size() == 1
    } else {
      assert serverErrorRequestListAppender.getEvents().size() == 0
    }

    if (inError) {
      assert errorRequestListAppender.getEvents().size() == 1
    } else {
      assert errorRequestListAppender.getEvents().size() == 0
    }


    where:
    statusCode | loggerName           | warnLogging | inInfo | inServerError | inError
    99         | INFO_LOGGER_NAME     | true        | true   | false         | false
    100        | INFO_LOGGER_NAME     | true        | true   | false         | false
    199        | INFO_LOGGER_NAME     | true        | true   | false         | false
    200        | INFO_LOGGER_NAME     | true        | true   | false         | false
    299        | INFO_LOGGER_NAME     | true        | true   | false         | false
    300        | INFO_LOGGER_NAME     | true        | true   | false         | false
    399        | INFO_LOGGER_NAME     | true        | true   | false         | false
    400        | INFO_LOGGER_NAME     | true        | true   | false         | true
    499        | INFO_LOGGER_NAME     | true        | true   | false         | true
    500        | INFO_LOGGER_NAME     | true        | true   | true          | true
    599        | INFO_LOGGER_NAME     | true        | true   | true          | true
    600        | INFO_LOGGER_NAME     | true        | true   | false         | false

    99         | WARN_LOGGER_NAME     | true        | true   | false         | false
    100        | WARN_LOGGER_NAME     | true        | false  | false         | false
    199        | WARN_LOGGER_NAME     | true        | false  | false         | false
    200        | WARN_LOGGER_NAME     | true        | false  | false         | false
    299        | WARN_LOGGER_NAME     | true        | false  | false         | false
    300        | WARN_LOGGER_NAME     | true        | false  | false         | false
    399        | WARN_LOGGER_NAME     | true        | false  | false         | false
    400        | WARN_LOGGER_NAME     | true        | true   | false         | true
    499        | WARN_LOGGER_NAME     | true        | true   | false         | true
    500        | WARN_LOGGER_NAME     | true        | true   | true          | true
    599        | WARN_LOGGER_NAME     | true        | true   | true          | true
    600        | WARN_LOGGER_NAME     | true        | true   | false         | false

    99         | WARN_5XX_LOGGER_NAME | true        | false  | false         | false
    100        | WARN_5XX_LOGGER_NAME | true        | false  | false         | false
    199        | WARN_5XX_LOGGER_NAME | true        | false  | false         | false
    200        | WARN_5XX_LOGGER_NAME | true        | false  | false         | false
    299        | WARN_5XX_LOGGER_NAME | true        | false  | false         | false
    300        | WARN_5XX_LOGGER_NAME | true        | false  | false         | false
    399        | WARN_5XX_LOGGER_NAME | true        | false  | false         | false
    400        | WARN_5XX_LOGGER_NAME | true        | false  | false         | false
    499        | WARN_5XX_LOGGER_NAME | true        | false  | false         | false
    500        | WARN_5XX_LOGGER_NAME | true        | true   | true          | true
    599        | WARN_5XX_LOGGER_NAME | true        | true   | true          | true
    600        | WARN_5XX_LOGGER_NAME | true        | false  | false         | false

    99         | ERROR_LOGGER_NAME    | true        | false  | false         | false
    100        | ERROR_LOGGER_NAME    | true        | false  | false         | false
    199        | ERROR_LOGGER_NAME    | true        | false  | false         | false
    200        | ERROR_LOGGER_NAME    | true        | false  | false         | false
    299        | ERROR_LOGGER_NAME    | true        | false  | false         | false
    300        | ERROR_LOGGER_NAME    | true        | false  | false         | false
    399        | ERROR_LOGGER_NAME    | true        | false  | false         | false
    400        | ERROR_LOGGER_NAME    | true        | false  | false         | false
    499        | ERROR_LOGGER_NAME    | true        | false  | false         | false
    500        | ERROR_LOGGER_NAME    | true        | false  | false         | false
    599        | ERROR_LOGGER_NAME    | true        | false  | false         | false
    600        | ERROR_LOGGER_NAME    | true        | false  | false         | false

    99         | INFO_LOGGER_NAME     | false       | true   | false         | false
    100        | INFO_LOGGER_NAME     | false       | true   | false         | false
    199        | INFO_LOGGER_NAME     | false       | true   | false         | false
    200        | INFO_LOGGER_NAME     | false       | true   | false         | false
    299        | INFO_LOGGER_NAME     | false       | true   | false         | false
    300        | INFO_LOGGER_NAME     | false       | true   | false         | false
    399        | INFO_LOGGER_NAME     | false       | true   | false         | false
    400        | INFO_LOGGER_NAME     | false       | true   | false         | true
    499        | INFO_LOGGER_NAME     | false       | true   | false         | true
    500        | INFO_LOGGER_NAME     | false       | true   | true          | true
    599        | INFO_LOGGER_NAME     | false       | true   | true          | true
    600        | INFO_LOGGER_NAME     | false       | true   | false         | false

    99         | WARN_LOGGER_NAME     | false       | false  | false         | false
    100        | WARN_LOGGER_NAME     | false       | false  | false         | false
    199        | WARN_LOGGER_NAME     | false       | false  | false         | false
    200        | WARN_LOGGER_NAME     | false       | false  | false         | false
    299        | WARN_LOGGER_NAME     | false       | false  | false         | false
    300        | WARN_LOGGER_NAME     | false       | false  | false         | false
    399        | WARN_LOGGER_NAME     | false       | false  | false         | false
    400        | WARN_LOGGER_NAME     | false       | false  | false         | false
    499        | WARN_LOGGER_NAME     | false       | false  | false         | false
    500        | WARN_LOGGER_NAME     | false       | false  | false         | false
    599        | WARN_LOGGER_NAME     | false       | false  | false         | false
    600        | WARN_LOGGER_NAME     | false       | false  | false         | false

    99         | WARN_5XX_LOGGER_NAME | false       | false  | false         | false
    100        | WARN_5XX_LOGGER_NAME | false       | false  | false         | false
    199        | WARN_5XX_LOGGER_NAME | false       | false  | false         | false
    200        | WARN_5XX_LOGGER_NAME | false       | false  | false         | false
    299        | WARN_5XX_LOGGER_NAME | false       | false  | false         | false
    300        | WARN_5XX_LOGGER_NAME | false       | false  | false         | false
    399        | WARN_5XX_LOGGER_NAME | false       | false  | false         | false
    400        | WARN_5XX_LOGGER_NAME | false       | false  | false         | false
    499        | WARN_5XX_LOGGER_NAME | false       | false  | false         | false
    500        | WARN_5XX_LOGGER_NAME | false       | false  | false         | false
    599        | WARN_5XX_LOGGER_NAME | false       | false  | false         | false
    600        | WARN_5XX_LOGGER_NAME | false       | false  | false         | false

    99         | ERROR_LOGGER_NAME    | false       | false  | false         | false
    100        | ERROR_LOGGER_NAME    | false       | false  | false         | false
    199        | ERROR_LOGGER_NAME    | false       | false  | false         | false
    200        | ERROR_LOGGER_NAME    | false       | false  | false         | false
    299        | ERROR_LOGGER_NAME    | false       | false  | false         | false
    300        | ERROR_LOGGER_NAME    | false       | false  | false         | false
    399        | ERROR_LOGGER_NAME    | false       | false  | false         | false
    400        | ERROR_LOGGER_NAME    | false       | false  | false         | false
    499        | ERROR_LOGGER_NAME    | false       | false  | false         | false
    500        | ERROR_LOGGER_NAME    | false       | false  | false         | false
    599        | ERROR_LOGGER_NAME    | false       | false  | false         | false
    600        | ERROR_LOGGER_NAME    | false       | false  | false         | false

    logLevel = warnLogging ? 'WARN' : 'INFO'
  }

  @Unroll
  def '#statusCode has the correct marker "#markerName" with error status codes emitted as "#logLevel"'() {
    setup:
    def logger = retrieveLogger(INFO_LOGGER_NAME)
    TimeZone.setDefault(TimeZone.getTimeZone('America/New_York'))

    def requestOutcome = createRequestOutcome(statusCode)
    def instance = new NcsaRequestLogger(logger, warnLogging)
    def events = requestListAppender.getEvents()


    when:
    instance.log(requestOutcome)


    then:
    events.size() == 1

    def event = events[0]
    def marker = event.marker
    marker != null
    marker.isInstanceOf(markerName)
    // all status markers have ACCESS_MARKER_NAME as parent
    marker.isInstanceOf(NcsaRequestLogger.ACCESS_MARKER_NAME)


    where:
    statusCode | markerName                                   | warnLogging
    99         | NcsaRequestLogger.STATUS_UNKNOWN_MARKER_NAME | false
    100        | NcsaRequestLogger.STATUS_1XX_MARKER_NAME     | false
    199        | NcsaRequestLogger.STATUS_1XX_MARKER_NAME     | false
    200        | NcsaRequestLogger.STATUS_2XX_MARKER_NAME     | false
    299        | NcsaRequestLogger.STATUS_2XX_MARKER_NAME     | false
    300        | NcsaRequestLogger.STATUS_3XX_MARKER_NAME     | false
    399        | NcsaRequestLogger.STATUS_3XX_MARKER_NAME     | false
    400        | NcsaRequestLogger.STATUS_4XX_MARKER_NAME     | false
    499        | NcsaRequestLogger.STATUS_4XX_MARKER_NAME     | false
    500        | NcsaRequestLogger.STATUS_5XX_MARKER_NAME     | false
    599        | NcsaRequestLogger.STATUS_5XX_MARKER_NAME     | false
    600        | NcsaRequestLogger.STATUS_UNKNOWN_MARKER_NAME | false

    99         | NcsaRequestLogger.STATUS_UNKNOWN_MARKER_NAME | true
    100        | NcsaRequestLogger.STATUS_1XX_MARKER_NAME     | true
    199        | NcsaRequestLogger.STATUS_1XX_MARKER_NAME     | true
    200        | NcsaRequestLogger.STATUS_2XX_MARKER_NAME     | true
    299        | NcsaRequestLogger.STATUS_2XX_MARKER_NAME     | true
    300        | NcsaRequestLogger.STATUS_3XX_MARKER_NAME     | true
    399        | NcsaRequestLogger.STATUS_3XX_MARKER_NAME     | true
    400        | NcsaRequestLogger.STATUS_4XX_MARKER_NAME     | true
    499        | NcsaRequestLogger.STATUS_4XX_MARKER_NAME     | true
    500        | NcsaRequestLogger.STATUS_5XX_MARKER_NAME     | true
    599        | NcsaRequestLogger.STATUS_5XX_MARKER_NAME     | true
    600        | NcsaRequestLogger.STATUS_UNKNOWN_MARKER_NAME | true

    logLevel = warnLogging ? 'WARN' : 'INFO'

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

  private static Level resolveExpectedLevel(int statusCode, boolean warnLogging) {
    if (!warnLogging) {
      return Level.INFO
    }
    if (statusCode >= 100 && statusCode < 400) {
      return Level.INFO
    }
    return Level.WARN
  }

  private static createRequestOutcome(int statusCode) {
    return createRequestOutcome('127.0.0.1', 'sfalken', TEST_TIMESTAMP, 'GET', 'wopr', 'HTTP/1.1', statusCode, 1337, 'request-id')
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
      new DefaultServerConfigBuilder(new ServerEnvironment([:], new Properties()), Impositions.none()).env().build(),  // serverConfig
      null,                              // bodyReader
      null,                   // idleTimeout
      null,                // clientCertificate
    )

    MutableRegistry registry = new DefaultMutableRegistry()
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
