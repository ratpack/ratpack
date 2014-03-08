/*
 * Copyright 2014 the original author or authors.
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

import geb.buildadapter.BuildAdapterFactory
import geb.driver.SauceLabsDriverFactory
import org.openqa.selenium.firefox.FirefoxDriver

if (!BuildAdapterFactory.getBuildAdapter(this.class.classLoader).reportsDir) {
  reportsDir = "build/geb"
}

driver = {
  def sauceBrowser = System.getProperty("geb.sauce.browser")
  if (sauceBrowser) {
    def username = System.getenv("GEB_SAUCE_LABS_USER")
    assert username
    def accessKey = System.getenv("GEB_SAUCE_LABS_ACCESS_PASSWORD")
    assert accessKey
    new SauceLabsDriverFactory().create(sauceBrowser, username, accessKey)
  } else {
    new FirefoxDriver()
  }
}
