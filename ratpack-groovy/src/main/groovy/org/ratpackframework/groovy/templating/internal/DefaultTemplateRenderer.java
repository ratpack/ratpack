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

package org.ratpackframework.groovy.templating.internal;

import io.netty.buffer.ByteBuf;
import org.ratpackframework.Result;
import org.ratpackframework.ResultAction;
import org.ratpackframework.groovy.templating.TemplateRenderer;
import org.ratpackframework.routing.Exchange;

import java.io.File;
import java.util.Collections;
import java.util.Map;

public class DefaultTemplateRenderer implements TemplateRenderer {

  private final File templateDir;
  private final Exchange exchange;
  private final GroovyTemplateRenderingEngine engine;

  public DefaultTemplateRenderer(File templateDir, Exchange exchange, GroovyTemplateRenderingEngine engine) {
    this.templateDir = templateDir;
    this.exchange = exchange;
    this.engine = engine;
  }

  @Override
  public void render(Map<String, ?> model, String templateId) {
    engine.renderTemplate(templateDir, templateId, model, new ResultAction<ByteBuf>() {
      @Override
      public void execute(Result<ByteBuf> event) {
        if (event.isFailure()) {
          error(ExceptionToTemplateModel.transform(exchange.getRequest(), event.getFailure()));
        } else {
          exchange.getResponse().send("text/html", event.getValue());
        }
      }
    });
  }

  public void render(String templateId) {
    render(Collections.<String, Object>emptyMap(), templateId);
  }

  @Override
  public void error(Map<String, ?> model) {
    engine.renderError(templateDir, model, new ResultAction<ByteBuf>() {
      @Override
      public void execute(Result<ByteBuf> event) {
        if (event.isFailure()) {
          exchange.error(event.getFailure());
        } else {
          exchange.getResponse().status(500).send("text/html", event.getValue());
        }
      }
    });
  }

}
