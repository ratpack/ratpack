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
import org.ratpackframework.groovy.templating.internal.GroovyTemplateRenderingEngine;
import org.ratpackframework.guice.HandlerDecoratingModule;
import org.ratpackframework.routing.Handler;

public class TemplatingModule extends AbstractModule implements HandlerDecoratingModule {

  private final TemplatingConfig templatingConfig = new TemplatingConfig();

  public TemplatingConfig getConfig() {
    return templatingConfig;
  }

  @Override
  protected void configure() {
    bind(GroovyTemplateRenderingEngine.class);
    bind(TemplatingConfig.class).toInstance(templatingConfig);
  }

  public Handler decorate(Injector injector, Handler handler) {
    TemplatingConfig config = injector.getInstance(TemplatingConfig.class);
    return TemplatingHandlers.templates(config.getTemplatesPath(), handler);
  }
}
