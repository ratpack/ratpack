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

package org.ratpackframework.test.groovy

import org.ratpackframework.groovy.RatpackScript
import org.ratpackframework.groovy.launch.GroovyScriptHandlerFactory
import org.ratpackframework.launch.LaunchConfig
import org.ratpackframework.launch.LaunchConfigFactory
import org.ratpackframework.server.RatpackServer
import org.ratpackframework.server.RatpackServerBuilder
import org.ratpackframework.test.InternalRatpackSpec

abstract class RatpackGroovyScriptAppSpec extends InternalRatpackSpec {

  boolean compileStatic = false
  boolean reloadable = false

  File getRatpackFile() {
    file("ratpack.groovy")
  }

  File templateFile(String path) {
    file("templates/$path")
  }

  def setup() {
    ratpackFile << "import static ${RatpackScript.name}.ratpack\n\n"
  }

  void script(String text) {
    ratpackFile.text = "import static ${RatpackScript.name}.ratpack\n\n$text"
  }

  protected Properties getProperties() {
    Properties properties = new Properties()
    properties.setProperty(LaunchConfigFactory.Property.HANDLER_FACTORY, GroovyScriptHandlerFactory.name)
    properties.setProperty(LaunchConfigFactory.Property.RELOADABLE, reloadable.toString())
    properties.setProperty(LaunchConfigFactory.Property.PORT, "0")
    properties.setProperty("other." + GroovyScriptHandlerFactory.COMPILE_STATIC_PROPERTY_NAME, compileStatic.toString())
    properties.setProperty("other." + GroovyScriptHandlerFactory.SCRIPT_PROPERTY_NAME, ratpackFile.name)

    return properties
  }

  @Override
  protected RatpackServer createServer() {
    LaunchConfig launchConfig = LaunchConfigFactory.createWithBaseDir(getClass().classLoader, ratpackFile.parentFile, properties)
    RatpackServerBuilder.build(launchConfig)
  }

}
