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

package org.ratpackframework.groovy.templating;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import org.ratpackframework.error.ClientErrorHandler;
import org.ratpackframework.error.ServerErrorHandler;
import org.ratpackframework.groovy.templating.internal.DefaultTemplatingConfig;
import org.ratpackframework.groovy.templating.internal.GroovyTemplateRenderingEngine;
import org.ratpackframework.groovy.templating.internal.TemplateRenderingClientErrorHandler;
import org.ratpackframework.groovy.templating.internal.TemplateRenderingServerErrorHandler;
import org.ratpackframework.guice.HandlerDecoratingModule;
import org.ratpackframework.handling.Handler;

import javax.inject.Singleton;

@SuppressWarnings("UnusedDeclaration")
public class TemplatingModule extends AbstractModule implements HandlerDecoratingModule {

  private String templatesPath = "templates";
  private int cacheSize = 100;
  private boolean staticallyCompile;

  public String getTemplatesPath() {
    return templatesPath;
  }

  public void setTemplatesPath(String templatesPath) {
    this.templatesPath = templatesPath;
  }

  public int getCacheSize() {
    return cacheSize;
  }

  public void setCacheSize(int cacheSize) {
    this.cacheSize = cacheSize;
  }

  public boolean isStaticallyCompile() {
    return staticallyCompile;
  }

  public void setStaticallyCompile(boolean staticallyCompile) {
    this.staticallyCompile = staticallyCompile;
  }

  @Override
  protected void configure() {
    bind(GroovyTemplateRenderingEngine.class).in(Singleton.class);
    bind(TemplatingConfig.class).toInstance(new DefaultTemplatingConfig(templatesPath, cacheSize, staticallyCompile));

    bind(ClientErrorHandler.class).to(TemplateRenderingClientErrorHandler.class).in(Singleton.class);
    bind(ServerErrorHandler.class).to(TemplateRenderingServerErrorHandler.class).in(Singleton.class);
  }

  public Handler decorate(Injector injector, Handler handler) {
    TemplatingConfig config = injector.getInstance(TemplatingConfig.class);
    return TemplatingHandlers.templates(config.getTemplatesPath(), handler);
  }
}
