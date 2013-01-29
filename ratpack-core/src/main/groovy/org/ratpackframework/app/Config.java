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

package org.ratpackframework.app;

import java.io.File;

/**
 * The API for the config file.
 */
public interface Config {

  /**
   * The base directory of the app.
   *
   * All other paths that are part of the config (e.g. templatesDir) are relative to this.
   * <p>
   * If ratpack was started via {@link RatpackMain}, this will be the JVM working directory, or the parent
   * directory of the config file that was passed as the argument.
   */
  File getBaseDir();

  /**
   * @see #getBaseDir()
   */
  void setBaseDir(File baseDir);

  /**
   * @see #getBaseDir()
   */
  void baseDir(File baseDir);

  /**
   * The port to listen on.
   *
   * Defaults to 5050.
   */
  int getPort();

  /**
   * @see #getPort()
   */
  void setPort(int port);

  /**
   * @see #getPort()
   */
  void port(int port);

  /**
   * The relative path (to the baseDir) to the directory containing static assets to serve.
   *
   * Defaults to "public".
   */
  String getStaticAssetsDir();

  /**
   * @see #getStaticAssetsDir()
   */
  void setStaticAssetsDir(String publicDir);

  /**
   * @see #getStaticAssetsDir()
   */
  void staticAssetsDir(String publicDir);

  /**
   * The relative path (to the baseDir) to the directory containing renderable templates.
   *
   * Defaults to "templates".
   */
  String getTemplatesDir();

  /**
   * @see #getTemplatesDir()
   */
  void setTemplatesDir(String templatesDir);

  /**
   * @see #getTemplatesDir()
   */
  void templatesDir(String templatesDir);

  /**
   * The relative path (to the baseDir) to the main routes file.
   *
   * Defaults to "ratpack.groovy".
   */
  String getRoutes();

  /**
   * @see #getRoutes()
   */
  void setRoutes(String routes);

  /**
   * @see #getRoutes()
   */
  void routes(String routes);

}