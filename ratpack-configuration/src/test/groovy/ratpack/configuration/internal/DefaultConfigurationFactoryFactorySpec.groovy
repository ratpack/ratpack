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

package ratpack.configuration.internal

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ratpack.configuration.ConfigurationException
import ratpack.configuration.ConfigurationFactoryFactory
import ratpack.configuration.ConfigurationSource
import ratpack.launch.LaunchConfigs
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class DefaultConfigurationFactoryFactorySpec extends Specification {
  private static final String PROP_CONFIGURATION_FACTORY = "${LaunchConfigs.SYSPROP_PREFIX_DEFAULT}${LaunchConfigs.Property.CONFIGURATION_FACTORY}"
  def classLoader = new GroovyClassLoader()

  @Subject
  ConfigurationFactoryFactory configurationFactoryFactory = new DefaultConfigurationFactoryFactory(classLoader)

  @Rule
  TemporaryFolder temporaryFolder

  def "uses DefaultConfigurationFactory by default"() {
    def configurationSource = createConfigurationSource()

    expect:
    configurationFactoryFactory.build(configurationSource).getClass() == DefaultConfigurationFactory
  }

  def "uses specified configuration factory if specified"() {
    configureServices(TestConfigurationFactory1, TestConfigurationFactory2)
    def configurationSource = createConfigurationSource([(PROP_CONFIGURATION_FACTORY): TestConfigurationFactory1.name])

    expect:
    configurationFactoryFactory.build(configurationSource).getClass() == TestConfigurationFactory1
  }

  def "exception is thrown if an unknown class is specified"() {
    def configurationSource = createConfigurationSource([(PROP_CONFIGURATION_FACTORY): "BadClass"])

    when:
    configurationFactoryFactory.build(configurationSource)

    then:
    def ex = thrown(ConfigurationException)
    ex.message == "Could not instantiate specified configuration factory class BadClass"
  }

  def "exception is thrown if multiple implementations are found without a property specifying which to use"() {
    configureServices(TestConfigurationFactory1, TestConfigurationFactory2)
    def configurationSource = createConfigurationSource()

    when:
    configurationFactoryFactory.build(configurationSource)

    then:
    def ex = thrown(ConfigurationException)
    ex.message == "Multiple possible configuration factories were found; please specify one with the 'configurationFactory' property"
  }

  @Unroll
  def "uses the implementation found by service loader"() {
    configureServices(implClass)
    def configurationSource = createConfigurationSource()

    expect:
    configurationFactoryFactory.build(configurationSource).getClass() == implClass

    where:
    implClass << [TestConfigurationFactory1, TestConfigurationFactory2]
  }

  private static ConfigurationSource createConfigurationSource(Map<String, String> propertyMap = [:]) {
    def props = new Properties()
    props.putAll(propertyMap)
    return new DefaultConfigurationSource(props, new Properties())
  }

  private void configureServices(Class... classes) {
    def resources = temporaryFolder.newFolder("resources")
    def servicesFile = new File(resources, "META-INF/services/ratpack.configuration.ConfigurationFactory")
    servicesFile.parentFile.mkdirs()
    servicesFile.text = classes.collect { it.name }.join("\n")
    classLoader.addURL(resources.toURI().toURL())
  }
}
