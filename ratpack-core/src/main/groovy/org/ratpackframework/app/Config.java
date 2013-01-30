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

import org.vertx.java.core.Vertx;

import java.io.File;

/**
 * The API for the config file.
 */
public interface Config {

  /**
   * The base directory of the app.
   *
   * All other paths that are part of the config (e.g. templatesDir) are relative to this. <p> If ratpack was started via {@link RatpackMain}, this will be the JVM working directory, or the parent
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
   * The hostname to listen to requests on.
   *
   * Defaults to "localhost".
   */
  String getHost();

  /**
   * @see #getHost()
   */
  void setHost(String host);

  /**
   * @see #getHost()
   */
  void host(String host);

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

  /**
   * <<<<<<< HEAD How many compiled templates to keep cached in memory.
   *
   * Defaults to 0 (i.e. always reload templates)
   */
  int getTemplatesCacheSize();

  /**
   * @see #getTemplatesCacheSize()
   */
  void setTemplatesCacheSize(int templateCacheSize);

  /**
   * @see #getTemplatesCacheSize()
   */
  void templatesCacheSize(int templateCacheSize);

  /**
   * Should the templates be compiled statically.
   *
   * Defaults to true.
   */
  boolean isStaticallyCompileTemplates();

  /**
   * @see #isStaticallyCompileTemplates()
   */
  void setStaticallyCompileTemplates(boolean staticallyCompileTemplates);

  /**
   * @see #isStaticallyCompileTemplates()
   */
  void staticallyCompileTemplates(boolean staticallyCompileTemplates);

  /**
   * Should the routes file (i.e. ratpack.groovy) be compiled statically.
   *
   * Defaults to true.
   */
  boolean isStaticallyCompileRoutes();

  /**
   * @see #isStaticallyCompileRoutes()
   */
  void setStaticallyCompileRoutes(boolean staticallyCompileRoutes);

  /**
   * @see #isStaticallyCompileRoutes()
   */
  void staticallyCompileRoutes(boolean staticallyCompileRoutes);

  /**
   * Should the routes file be automatically reloaded when it changes.
   *
   * Defaults to true.
   */
  boolean isReloadRoutes();

  /**
   * @see #isReloadRoutes()
   */
  void setReloadRoutes(boolean reloadRoutes);

  /**
   * @see #isReloadRoutes()
   */
  void reloadRoutes(boolean reloadRoutes);

  /**
   * The maximum number of sessions to keep in memory.
   *
   * Defaults to 100.
   */
  int getMaxActiveSessions();

  /**
   * @see #getMaxActiveSessions()
   */
  void setMaxActiveSessions(int maxActiveSessions);

  /**
   * @see #getMaxActiveSessions()
   */
  void maxActiveSessions(int maxActiveSessions);

  /**
   * The number of mins to allow sessions to remain inactive for before they are eligible for eviction.
   *
   * Defaults to 60.
   */
  int getSessionTimeoutMins();

  /**
   * @see #getSessionTimeoutMins()
   */
  void setSessionTimeoutMins(int sessionTimeoutMins);

  /**
   * @see #getSessionTimeoutMins()
   */
  void sessionTimeoutMins(int sessionTimeoutMins);

  /**
   * The number of mins to allow the session id cookie to live for.
   *
   * Defaults to 1 year.
   *
   * Use 0 to specify no expiry (i.e. temporary session).
   */
  int getSessionCookieExpiresMins();

  /**
   * @see #getSessionCookieExpiresMins()
   */
  void setSessionCookieExpiresMins(int sessionCookieExpiresMins);

  /**
   * @see #getSessionCookieExpiresMins()
   */
  void sessionCookieExpiresMins(int sessionCookieExpiresMins);

  /**
   * The vertx instance to attach the app to.
   *
   * Defaults to {@code Vertx.newVertx()}.
   */
  Vertx getVertx();

  /**
   * @see #getVertx()
   */
  void setVertx(Vertx vertx);

  /**
   * @see #getVertx()
   */
  void vertx(Vertx vertx);
}