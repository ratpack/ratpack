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

package org.ratpackframework.app.internal;

import groovy.lang.Script;
import org.ratpackframework.app.Config;
import org.ratpackframework.app.RatpackApp;
import org.ratpackframework.internal.NoOpHandler;
import org.ratpackframework.service.ServiceRegistry;
import org.ratpackframework.session.SessionListener;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.sockjs.SockJSServer;

import java.io.File;

public class ConfigScript extends Script implements Config {

  private File baseDir;
  private int port = 5050;
  private String host = "localhost";
  private String staticAssetsDir = "public";
  private String templatesDir = "templates";
  private String routes = "ratpack.groovy";
  private int templatesCacheSize = 0;
  private boolean staticallyCompileTemplates = true;
  boolean staticallyCompileRoutes = false;
  private boolean reloadRoutes = true;

  private int sessionCookieExpiresMins = 60 * 60 * 24 * 365; // 1 year
  private SessionListener sessionListener;

  private ServiceRegistry serviceRegistry;

  private Vertx vertx;

  private Handler<RatpackApp> onStart = null;

  public ConfigScript(File baseDir) {
    this.baseDir = baseDir;
  }

  @Override
  public File getBaseDir() {
    return baseDir;
  }

  @Override
  public void setBaseDir(File baseDir) {
    this.baseDir = baseDir;
  }

  @Override
  public int getPort() {
    return port;
  }

  @Override
  public void setPort(int port) {
    this.port = port;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  @Override
  public String getStaticAssetsDir() {
    return staticAssetsDir;
  }

  @Override
  public void setStaticAssetsDir(String staticAssetsDir) {
    this.staticAssetsDir = staticAssetsDir;
  }

  @Override
  public String getTemplatesDir() {
    return templatesDir;
  }

  @Override
  public void setTemplatesDir(String templatesDir) {
    this.templatesDir = templatesDir;
  }

  @Override
  public String getRoutes() {
    return routes;
  }

  @Override
  public void setRoutes(String routes) {
    this.routes = routes;
  }

  public boolean isReloadRoutes() {
    return reloadRoutes;
  }

  public void setReloadRoutes(boolean reloadRoutes) {
    this.reloadRoutes = reloadRoutes;
  }

  @Override
  public int getTemplatesCacheSize() {
    return templatesCacheSize;
  }

  @Override
  public void setTemplatesCacheSize(int templatesCacheSize) {
    this.templatesCacheSize = templatesCacheSize;
  }

  @Override
  public boolean isStaticallyCompileTemplates() {
    return staticallyCompileTemplates;
  }

  @Override
  public void setStaticallyCompileTemplates(boolean staticallyCompileTemplates) {
    this.staticallyCompileTemplates = staticallyCompileTemplates;
  }

  @Override
  public boolean isStaticallyCompileRoutes() {
    return staticallyCompileRoutes;
  }

  @Override
  public void setStaticallyCompileRoutes(boolean staticallyCompileRoutes) {
    this.staticallyCompileRoutes = staticallyCompileRoutes;
  }

  @Override
  public void baseDir(File baseDir) {
    setBaseDir(baseDir);
  }

  @Override
  public void port(int port) {
    setPort(port);
  }

  @Override
  public void host(String host) {
    setHost(host);
  }

  @Override
  public void staticAssetsDir(String publicDir) {
    setStaticAssetsDir(publicDir);
  }

  @Override
  public void templatesDir(String templatesDir) {
    setTemplatesDir(templatesDir);
  }

  @Override
  public void routes(String routes) {
    setRoutes(routes);
  }

  @Override
  public void templatesCacheSize(int templateCacheSize) {
    setTemplatesCacheSize(templateCacheSize);
  }

  @Override
  public void staticallyCompileTemplates(boolean staticallyCompileTemplates) {
    setStaticallyCompileTemplates(staticallyCompileTemplates);
  }

  @Override
  public void staticallyCompileRoutes(boolean staticallyCompileRoutes) {
    setStaticallyCompileRoutes(staticallyCompileRoutes);
  }

  @Override
  public void reloadRoutes(boolean reloadRoutes) {
    setReloadRoutes(reloadRoutes);
  }

  @Override
  public int getSessionCookieExpiresMins() {
    return sessionCookieExpiresMins;
  }

  @Override
  public void setSessionCookieExpiresMins(int sessionCookieExpiresMins) {
    this.sessionCookieExpiresMins = sessionCookieExpiresMins;
  }

  @Override
  public void sessionCookieExpiresMins(int sessionCookieExpiresMins) {
    this.sessionCookieExpiresMins = sessionCookieExpiresMins;
  }

  @Override
  public SessionListener getSessionListener() {
    return sessionListener;
  }

  @Override
  public void setSessionListener(SessionListener sessionListener) {
    this.sessionListener = sessionListener;
  }

  @Override
  public void sessionListener(SessionListener sessionListener) {
    this.sessionListener = sessionListener;
  }

  @Override
  public ServiceRegistry getServices() {
    return serviceRegistry;
  }

  @Override
  public void setServices(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  @Override
  public void services(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  @Override
  public Vertx getVertx() {
    if (vertx == null) {
      vertx = Vertx.newVertx();
    }
    return vertx;
  }

  @Override
  public void setVertx(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public void vertx(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public Handler<RatpackApp> getOnStart() {
    return onStart;
  }

  @Override
  public void setOnStart(Handler<RatpackApp> onStart) {
    this.onStart = onStart;
  }

  @Override
  public void onStart(Handler<RatpackApp> onStart) {
    this.onStart = onStart;
  }

  @Override
  public Object run() {
    return this;
  }

}
