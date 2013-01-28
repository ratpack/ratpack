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

package org.ratpackframework.app.internal

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.ratpackframework.app.Config

@ToString
@CompileStatic
class ConfigScript extends Script implements Config {

  File baseDir
  int port = 5050
  String publicDir = "public"
  String templatesDir = "templates"
  int templatesCacheSize = 0
  boolean staticallyCompileTemplates = true
  String routes = "ratpack.groovy"
  boolean staticallyCompileRoutes = false

  ConfigScript(File baseDir) {
    this.baseDir = baseDir
  }

  @Override
  void baseDir(File baseDir) {
    setBaseDir(baseDir)
  }

  void port(int port) {
    setPort(port)
  }

  void publicDir(String publicDir) {
    setPublicDir(publicDir)
  }

  void templatesDir(String templatesDir) {
    setTemplatesDir(templatesDir)
  }

  @Override
  void templatesCacheSize(int templatesCacheSize) {
    setTemplatesCacheSize(templatesCacheSize)
  }

  @Override
  void staticallyCompileTemplates(boolean staticallyCompileTemplates) {
    setStaticallyCompileTemplates(staticallyCompileTemplates)
  }

  void routes(String routes) {
    setRoutes(routes)
  }

  @Override
  void staticallyCompileRoutes(boolean staticallyCompileRoutes) {
    setStaticallyCompileRoutes(staticallyCompileRoutes)
  }

  @Override
  Object run() {
    this
  }
}
