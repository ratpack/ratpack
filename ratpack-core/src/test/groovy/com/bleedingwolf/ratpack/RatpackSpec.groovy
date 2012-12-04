/*
 * Copyright 2012 the original author or authors.
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

package com.bleedingwolf.ratpack

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class RatpackSpec extends Specification {

  @Rule TemporaryFolder temporaryFolder

  RatpackApp app

  File getConfigFile() {
    file("config.groovy")
  }

  File getRatpackFile() {
    file("ratpack.groovy")
  }

  File templateFile(String path) {
    file("templates/$path")
  }

  File publicFile(String path) {
    file("public/$path")
  }

  File file(String path) {
    def file = new File(temporaryFolder.root, path)
    assert file.parentFile.mkdirs() || file.parentFile.exists()
    file
  }

  def setup() {
    app = new RatpackAppFactory().create(configFile)
  }

  def cleanup() {
    app.stop()
  }

  URLConnection url(String path = "") {
    new URL("http://localhost:$app.port/$path").openConnection()
  }

  String urlText(String path = "") {
    new URL("http://localhost:$app.port/$path").text
  }
}
