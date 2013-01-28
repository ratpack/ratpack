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

package org.ratpackframework.app

interface Config {

  File getBaseDir()
  void setBaseDir(File baseDir)
  void baseDir(File baseDir)

  int getPort()
  void setPort(int port)
  void port(int port)

  String getPublicDir()
  void setPublicDir(String publicDir)
  void publicDir(String publicDir)

  String getTemplatesDir()
  void setTemplatesDir(String templatesDir)
  void templatesDir(String templatesDir)

  int getTemplatesCacheSize()
  void setTemplatesCacheSize(int templatesCacheSize)
  void templatesCacheSize(int templatesCacheSize)

  boolean isStaticallyCompileTemplates()
  void setStaticallyCompileTemplates(boolean staticallyCompileTemplates)
  void staticallyCompileTemplates(boolean staticallyCompileTemplates)

  String getRoutes()
  void setRoutes(String routes)
  void routes(String routes)

  boolean isStaticallyCompileRoutes()
  void setStaticallyCompileRoutes(boolean staticallyCompileRoutes)
  void staticallyCompileRoutes(boolean staticallyCompileRoutes)

}