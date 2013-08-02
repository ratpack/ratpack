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
import org.ratpackframework.groovy.templating.TemplateRenderer;
import org.ratpackframework.handling.Context;
import org.ratpackframework.http.Response;
import org.ratpackframework.util.internal.Result;
import org.ratpackframework.util.internal.ResultAction;

import java.io.File;
import java.util.Collections;
import java.util.Map;

public class DefaultTemplateRenderer implements TemplateRenderer {

  private final File templateDir;
  private final Context context;
  private final GroovyTemplateRenderingEngine engine;

  public DefaultTemplateRenderer(File templateDir, Context context, GroovyTemplateRenderingEngine engine) {
    this.templateDir = templateDir;
    this.context = context;
    this.engine = engine;
  }

  public void render(Map<String, ?> model, String templateId) {
    engine.renderTemplate(templateDir, templateId, model, new ResultAction<ByteBuf>() {
      public void execute(Result<ByteBuf> thing) {
        if (thing.isFailure()) {
          error(ExceptionToTemplateModel.transform(context.getRequest(), thing.getFailure()));
        } else {
          context.getResponse().send("text/html", thing.getValue());
        }
      }
    });
  }

  public void render(String templateId) {
    render(Collections.<String, Object>emptyMap(), templateId);
  }

  public void error(Map<String, ?> model) {
    engine.renderError(templateDir, model, new ResultAction<ByteBuf>() {
      public void execute(Result<ByteBuf> thing) {
        if (thing.isFailure()) {
          context.error(thing.getFailure());
        } else {
          Response response = context.getResponse();
          if (response.getStatus().getCode() < 400) {
            response.status(500);
          }
          response.send("text/html", thing.getValue());
        }
      }
    });
  }

}
