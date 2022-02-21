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

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode

class SniSslContextDeserializerSpec extends AbstractSslContextDeserializerSpec {

  SniSslContextDeserializer deserializer = new SniSslContextDeserializer()

  def "deserialize called without any expected nodes set"() {
    when:
    deserializer.deserialize(jsonParser, context)

    then:
    def e = thrown(IllegalStateException)
    e.message == 'error with default ssl context: keystoreFile must be set if any ssl properties are set'
  }

  def "deserialize called with just keystoreFile set for default context"() {
    given:
    objectNode.put('keystoreFile', 'does-not-matter')

    when:
    deserializer.deserialize(jsonParser, context)

    then:
    def e = thrown(IllegalStateException)
    e.message == 'error with default ssl context: keystorePassword must be set if any ssl properties are set'
  }

  def "deserialize called with truststore but no password for default context"() {
    given:
    objectNode.put('keystoreFile', 'does-not-matter')
    objectNode.put('keystorePassword', 'does-not-matter')
    objectNode.put('truststoreFile', 'does-not-matter')

    when:
    deserializer.deserialize(jsonParser, context)

    then:
    def e = thrown(IllegalStateException)
    e.message == 'error with default ssl context: truststorePassword must be specified when truststoreFile is specified'
  }

  def "simple keystoreFile and keystorePassword for default context"() {
    given:
    def keyStoreFile = tempFolder.newFile('keystore.jks').toPath()
    def keyStorePassword = 'totally-unexpected'
    createKeystore(keyStoreFile, keyStorePassword)
    objectNode.put('keystoreFile', keyStoreFile.toString())
    objectNode.put('keystorePassword', keyStorePassword)

    when:
    def mapping = deserializer.deserialize(jsonParser, context)

    then:
    mapping
    mapping.map("example.com")
  }

  def "both valid keystoreFile and truststoreFile for default context"() {
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
    def mapping = deserializer.deserialize(jsonParser, context)

    then:
    mapping
    mapping.map("example.com")
  }

  def "invalid keystore password for default context"() {
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
    e.message == 'error with default ssl context: Keystore was tampered with, or password was incorrect'
  }

  def "invalid truststore password for default context"() {
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
    e.message == 'error with default ssl context: Keystore was tampered with, or password was incorrect'
  }

  def "deserialize called with just keystoreFile set for subdomain context"() {
    given:
    populateValidDefaultContext()
    ObjectNode subDomainNode = JsonNodeFactory.instance.objectNode()
    subDomainNode.put('keystoreFile', 'does-not-matter')
    objectNode.set("*.ratpack.io", subDomainNode)

    when:
    deserializer.deserialize(jsonParser, context)

    then:
    def e = thrown(IllegalStateException)
    e.message == 'error with *.ratpack.io ssl context: keystorePassword must be set if any ssl properties are set'
  }

  def "deserialize called with truststore but no password for subdomain context"() {
    given:
    populateValidDefaultContext()
    ObjectNode subDomainNode = JsonNodeFactory.instance.objectNode()
    subDomainNode.put('keystoreFile', 'does-not-matter')
    subDomainNode.put('keystorePassword', 'does-not-matter')
    subDomainNode.put('truststoreFile', 'does-not-matter')
    objectNode.set("*.ratpack.io", subDomainNode)

    when:
    deserializer.deserialize(jsonParser, context)

    then:
    def e = thrown(IllegalStateException)
    e.message == 'error with *.ratpack.io ssl context: truststorePassword must be specified when truststoreFile is specified'
  }

  def "simple keystoreFile and keystorePassword for subdomain context"() {
    given:
    populateValidDefaultContext()

    def keyStoreFile = tempFolder.newFile("keystore2.jsk").toPath()
    def keyStorePassword = 'totally-unexpected-again'
    createKeystore(keyStoreFile, keyStorePassword)
    ObjectNode subDomainNode = JsonNodeFactory.instance.objectNode()
    subDomainNode.put('keystoreFile', keyStoreFile.toString())
    subDomainNode.put('keystorePassword', keyStorePassword)
    objectNode.set("*.ratpack.io", subDomainNode)

    when:
    def mapping = deserializer.deserialize(jsonParser, context)

    then:
    mapping

    when:
    def defaultContext = mapping.map("example.com")
    def ratpackContext = mapping.map("api.ratpack.io")

    then:
    defaultContext
    ratpackContext
    defaultContext != ratpackContext
  }

  def "both valid keystoreFile and truststoreFile for subdomain context"() {
    given:
    populateValidDefaultContext()
    ObjectNode subDomainNode = JsonNodeFactory.instance.objectNode()

    def keyStoreFile = tempFolder.newFile('keystore2.jks').toPath()
    def keyStorePassword = 'totally-unexpected'
    def trustStoreFile = tempFolder.newFile('truststore2.jks').toPath()
    def trustStorePassword = 'different'
    createKeystore(keyStoreFile, keyStorePassword)
    createKeystore(trustStoreFile, trustStorePassword)
    subDomainNode.put('keystoreFile', keyStoreFile.toString())
    subDomainNode.put('keystorePassword', keyStorePassword)
    subDomainNode.put('truststoreFile', trustStoreFile.toString())
    subDomainNode.put('truststorePassword', trustStorePassword)
    objectNode.set("*.ratpack.io", subDomainNode)

    when:
    def mapping = deserializer.deserialize(jsonParser, context)

    then:
    mapping

    when:
    def defaultContext = mapping.map("example.com")
    def ratpackContext = mapping.map("api.ratpack.io")

    then:
    defaultContext
    ratpackContext
    defaultContext != ratpackContext
  }

  def "invalid keystore password for subdomain context"() {
    given:
    populateValidDefaultContext()
    ObjectNode subDomainNode = JsonNodeFactory.instance.objectNode()

    def keyStoreFile = tempFolder.newFile('keystore2.jks').toPath()
    def keyStorePassword = 'totally-unexpected'
    createKeystore(keyStoreFile, keyStorePassword)
    subDomainNode.put('keystoreFile', keyStoreFile.toString())
    subDomainNode.put('keystorePassword', 'definitely-not-correct')
    objectNode.set("*.ratpack.io", subDomainNode)

    when:
    deserializer.deserialize(jsonParser, context)

    then:
    def e = thrown(IOException)
    e.message == 'error with *.ratpack.io ssl context: Keystore was tampered with, or password was incorrect'
  }

  def "invalid truststore password for subdomain context"() {
    given:
    populateValidDefaultContext()
    ObjectNode subDomainNode = JsonNodeFactory.instance.objectNode()

    def keyStoreFile = tempFolder.newFile('keystore2.jks').toPath()
    def keyStorePassword = 'totally-unexpected'
    def trustStoreFile = tempFolder.newFile('truststore2.jks').toPath()
    def trustStorePassword = 'different'
    createKeystore(keyStoreFile, keyStorePassword)
    createKeystore(trustStoreFile, trustStorePassword)
    subDomainNode.put('keystoreFile', keyStoreFile.toString())
    subDomainNode.put('keystorePassword', keyStorePassword)
    subDomainNode.put('truststoreFile', trustStoreFile.toString())
    subDomainNode.put('truststorePassword', 'not-the-correct-one')
    objectNode.set("*.ratpack.io", subDomainNode)

    when:
    deserializer.deserialize(jsonParser, context)

    then:
    def e = thrown(IOException)
    e.message == 'error with *.ratpack.io ssl context: Keystore was tampered with, or password was incorrect'
  }

  private void populateValidDefaultContext() {
    def keyStoreFile = tempFolder.newFile('keystore.jks').toPath()
    def keyStorePassword = 'totally-unexpected'
    createKeystore(keyStoreFile, keyStorePassword)
    objectNode.put('keystoreFile', keyStoreFile.toString())
    objectNode.put('keystorePassword', keyStorePassword)
  }
}
