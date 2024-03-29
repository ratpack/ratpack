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

package ratpack.core.config.internal.module


class NettySslContextDeserializerSpec extends AbstractSslContextDeserializerSpec {

  NettySslContextDeserializer deserializer = new NettySslContextDeserializer()

  def setup() {
    jsonParser.readValueAsTree() >> objectNode
  }

  def "deserialize called without any expected nodes set"() {
    when:
    deserializer.deserialize(jsonParser, context)
    then:
    def e = thrown(IllegalStateException)
    e.message == 'keystoreFile must be set if any ssl properties are set'
  }

  def "deserialize called with just keystoreFile set"() {
    given:
    objectNode.put('keystoreFile', 'does-not-matter')
    when:
    deserializer.deserialize(jsonParser, context)
    then:
    def e = thrown(IllegalStateException)
    e.message == 'keystorePassword must be set if any ssl properties are set'
  }

  def "deserialize called with truststore but no password"() {
    given:
    objectNode.put('keystoreFile', 'does-not-matter')
    objectNode.put('keystorePassword', 'does-not-matter')
    objectNode.put('truststoreFile', 'does-not-matter')
    when:
    deserializer.deserialize(jsonParser, context)
    then:
    def e = thrown(IllegalStateException)
    e.message == 'truststorePassword must be specified when truststoreFile is specified'
  }

  def "simple keystoreFile and keystorePassword"() {
    given:
    def keyStoreFile = tempFolder.newFile('keystore.jks').toPath()
    def keyStorePassword = 'totally-unexpected'
    createKeystore(keyStoreFile, keyStorePassword)
    objectNode.put('keystoreFile', keyStoreFile.toString())
    objectNode.put('keystorePassword', keyStorePassword)
    when:
    def sslContext = deserializer.deserialize(jsonParser, context)
    then:
    sslContext
  }

  def "both valid keystoreFile and truststoreFile"() {
    given:
    def keyStoreFile = tempFolder.newFile('keystore.jks').toPath()
    def keyStorePassword = 'totally-unexpected'
    def trustStoreFile = tempFolder.newFile('truststore.jks').toPath()
    def trustStorePassword = 'different'
    createKeystore(keyStoreFile, keyStorePassword)
    createKeystore(trustStoreFile, trustStorePassword)
    objectNode.put('keystoreFile', keyStoreFile.toString())
    objectNode.put('keystorePassword', keyStorePassword)
    objectNode.put('truststoreFile', trustStoreFile.toString())
    objectNode.put('truststorePassword', trustStorePassword)
    when:
    def sslContext = deserializer.deserialize(jsonParser, context)
    then:
    sslContext
  }

  def "invalid keystore password"() {
    given:
    def keyStoreFile = tempFolder.newFile('keystore.jks').toPath()
    def keyStorePassword = 'totally-unexpected'
    createKeystore(keyStoreFile, keyStorePassword)
    objectNode.put('keystoreFile', keyStoreFile.toString())
    objectNode.put('keystorePassword', 'definitely-not-correct')
    when:
    deserializer.deserialize(jsonParser, context)
    then:
    def e = thrown(IOException)
    e.message == 'Keystore was tampered with, or password was incorrect'
  }

  def "invalid truststore password"() {
    given:
    def keyStoreFile = tempFolder.newFile('keystore.jks').toPath()
    def keyStorePassword = 'totally-unexpected'
    def trustStoreFile = tempFolder.newFile('truststore.jks').toPath()
    def trustStorePassword = 'different'
    createKeystore(keyStoreFile, keyStorePassword)
    createKeystore(trustStoreFile, trustStorePassword)
    objectNode.put('keystoreFile', keyStoreFile.toString())
    objectNode.put('keystorePassword', keyStorePassword)
    objectNode.put('truststoreFile', trustStoreFile.toString())
    objectNode.put('truststorePassword', 'not-the-correct-one')
    when:
    deserializer.deserialize(jsonParser, context)
    then:
    def e = thrown(IOException)
    e.message == 'Keystore was tampered with, or password was incorrect'
  }
}
