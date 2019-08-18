/*
 * Copyright 2015 the original author or authors.
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

package ratpack.config.internal.source

import com.fasterxml.jackson.databind.node.ObjectNode
import ratpack.config.internal.DefaultConfigDataBuilder
import ratpack.file.FileSystemBinding
import ratpack.server.internal.ServerEnvironment
import spock.lang.Specification

import static ratpack.config.ConfigDataBuilder.DEFAULT_ENV_PREFIX

class EnvironmentConfigSourceSpec extends Specification {
  private static final SAMPLE_ENV_VARS = [USER: "jdoe", SHELL: "/bin/bash", LANG: "en_US.UTF-8"]

  def "supports empty prefix"() {
    def input = SAMPLE_ENV_VARS + [THREADS: "10", PUBLIC_ADDRESS: "http://localhost:8080"]

    when:
    def rootNode = loadConfig(input, "")

    then:
    rootNode.path("threads").asText() == "10"
    rootNode.path("publicAddress").asText() == "http://localhost:8080"
    rootNode.size() > 2
  }

  def "when prefix provided, only matched elements are included, minus prefix: #prefix"() {
    when:
    def rootNode = loadConfig(input, prefix)

    then:
    rootNode.path("threads").asText() == "10"
    rootNode.path("publicAddress").asText() == "http://localhost:8080"
    rootNode.size() == 2

    where:
    prefix             | input
    DEFAULT_ENV_PREFIX | SAMPLE_ENV_VARS + [(DEFAULT_ENV_PREFIX + "THREADS"): "10", (DEFAULT_ENV_PREFIX + "PUBLIC_ADDRESS"): "http://localhost:8080"]
    "APP_"             | SAMPLE_ENV_VARS + ["APP_THREADS": "10", "APP_PUBLIC_ADDRESS": "http://localhost:8080"]
  }

  def "entries are broken into sub-objects based on double underscore delimiter"() {
    def input = [RATPACK_SERVER__PORT: "8080", RATPACK_SERVER__THREADS: "10", RATPACK_DB__JDBC_URL: "jdbc:h2:mem:"]

    when:
    def rootNode = loadConfig(input)

    then:
    rootNode.path("server").path("port").asText() == "8080"
    rootNode.path("server").path("threads").asText() == "10"
    rootNode.path("db").path("jdbcUrl").asText() == "jdbc:h2:mem:"
    rootNode.size() == 2
  }

  private static ObjectNode loadConfig(Map<String, String> input, String prefix = DEFAULT_ENV_PREFIX) {
    def environment = new ServerEnvironment(input, new Properties())
    def mapper = DefaultConfigDataBuilder.newDefaultObjectMapper()
    def source = new EnvironmentConfigSource(environment, prefix)
    source.loadConfigData(mapper, FileSystemBinding.root())
  }
}
