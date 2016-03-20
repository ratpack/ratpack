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

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.time.Instant

@Subject(NcsaRequestLogger)
class NcsaRequestLoggerSpec extends Specification {

  private static final Long TEST_TIMESTAMP = 1458250293458L
  private static final String INFO_LOGGER_NAME = 'ratpack.request.info'

  @Shared
  private Locale originalDefaultLocale

  @Shared
  private TimeZone originalDefaultTimeZone

  def setupSpec() {
    // save original state of locale and timezone
    originalDefaultLocale = Locale.getDefault()
    originalDefaultTimeZone = TimeZone.getDefault()
  }

  def setup() {
    restoreOriginalLocaleAndTimeZone()
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

  // helper methods below
  private static Logger retrieveLogger(String loggerName) {
    return LoggerFactory.getLogger(loggerName)
  }

  private void restoreOriginalLocaleAndTimeZone() {
    Locale.setDefault(originalDefaultLocale)
    TimeZone.setDefault(originalDefaultTimeZone)
  }
}
