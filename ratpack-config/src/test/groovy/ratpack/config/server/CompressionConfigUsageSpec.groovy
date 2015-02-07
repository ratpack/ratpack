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

package ratpack.config.server

import com.google.common.io.ByteSource
import ratpack.config.ConfigData
import ratpack.server.CompressionConfig
import spock.lang.Specification

import static com.google.common.base.Charsets.UTF_8

@SuppressWarnings("GrMethodMayBeStatic")
class CompressionConfigUsageSpec extends Specification {
  def "can get CompressionConfig from no data"() {
    given:
    def configData = ConfigData.of().build()

    when:
    def config = configData.get(CompressionConfig)

    then:
    !config.compressResponses
    config.minSize == CompressionConfig.DEFAULT_COMPRESSION_MIN_SIZE
    config.mimeTypeWhiteList.empty
    config.mimeTypeBlackList.empty
  }

  def "can override all CompressionConfig fields"() {
    given:
    def yamlData = """
    |---
    |compressResponses: true
    |mimeTypeBlackList:
    |  - image/png
    |  - image/gif
    |mimeTypeWhiteList:
    |  - application/json
    |  - text/plain
    |minSize: 2048
    """.stripMargin()
    def configData = ConfigData.of().yaml(ByteSource.wrap(yamlData.getBytes(UTF_8))).build()

    when:
    def config = configData.get(CompressionConfig)

    then:
    config.compressResponses
    config.minSize == 2048L
    config.mimeTypeWhiteList == ["application/json", "text/plain"] as Set
    config.mimeTypeBlackList == ["image/png", "image/gif"] as Set
  }
}
