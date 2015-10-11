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
import spock.lang.Specification

class ArgsConfigSourceSpec extends Specification {

  def "can use args"() {
    when:
    def rootNode = loadConfig("foo=bar", "a.b=c=d", "baz")

    then:
    rootNode.path("foo").asText() == "bar"
    rootNode.path("baz").asText() == ""
    rootNode.path("a").path("b").asText() == "c=d"
  }

  private static ObjectNode loadConfig(String... args) {
    def mapper = DefaultConfigDataBuilder.newDefaultObjectMapper()
    def source = new ArgsConfigSource(null, "=", args as String[])
    source.loadConfigData(mapper, FileSystemBinding.root())
  }
}
