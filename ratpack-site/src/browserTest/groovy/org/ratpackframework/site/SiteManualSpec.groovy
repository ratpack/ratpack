/*
 * Copyright 2013 the original author or authors.
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

package org.ratpackframework.site

import geb.spock.GebReportingSpec
import org.ratpackframework.groovy.test.LocalScriptApplicationUnderTest
import org.ratpackframework.site.pages.APIIndexPage
import org.ratpackframework.site.pages.HomePage
import org.ratpackframework.site.pages.ManualToCPage
import org.ratpackframework.test.ServerBackedApplicationUnderTest

class SiteManualSpec extends GebReportingSpec {

  private final static ServerBackedApplicationUnderTest applicationUnderTest = new LocalScriptApplicationUnderTest()

  def setup() {
    URI base = applicationUnderTest.address
    browser.baseUrl = base.toString()
  }

  def cleanup() {
    applicationUnderTest.stop()
  }

  def "go to current manual"() {
    given:
    to HomePage

    when:
    manualLink.click()

    then:
    at ManualToCPage
  }

  def "got to current api" () {
    given:
    to HomePage

    when:
    apiLink.click()

    then:
    at APIIndexPage
  }


}
