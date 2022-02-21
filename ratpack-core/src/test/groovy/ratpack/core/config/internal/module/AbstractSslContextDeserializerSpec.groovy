/*
 * Copyright 2022 the original author or authors.
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

package ratpack.core.config.internal.module

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import ratpack.test.internal.BaseRatpackSpec
import ratpack.test.internal.spock.TempDir
import ratpack.test.internal.spock.TemporaryFolder

import java.nio.file.Path
import java.security.KeyStore

class AbstractSslContextDeserializerSpec extends BaseRatpackSpec {

  @TempDir
  TemporaryFolder tempFolder

  DeserializationContext context = null
  ObjectNode objectNode = JsonNodeFactory.instance.objectNode()
  JsonParser jsonParser = Stub(JsonParser)

  def setup() {
    jsonParser.readValueAsTree() >> objectNode
    jsonParser.getCodec() >> new ObjectMapper()
  }

  /**
   * Creates an empty keystore file with the given password at the path provided.
   *
   * @param path the name of the file to create.
   * @param password the password to assign to the file.  Not enforced, but when used for real,
   * it should have 6 characters or more.
   */
  static void createKeystore(Path path, String password) {
    def ks = KeyStore.getInstance("JKS")
    ks.load(null, password.toCharArray())
    path.withOutputStream { ks.store(it, password.toCharArray()) }
  }
}
