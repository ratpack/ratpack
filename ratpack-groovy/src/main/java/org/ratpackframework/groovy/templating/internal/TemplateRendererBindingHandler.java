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

import org.ratpackframework.file.FileSystemBinding;
import org.ratpackframework.groovy.templating.TemplateRenderer;
import org.ratpackframework.handling.Exchange;
import org.ratpackframework.handling.Handler;

import java.io.File;

public class TemplateRendererBindingHandler implements Handler {

  private final String templateDir;
  private final Handler delegate;

  public TemplateRendererBindingHandler(String templateDir, Handler delegate) {
    this.templateDir = templateDir;
    this.delegate = delegate;
  }

  public void handle(Exchange exchange) {
    GroovyTemplateRenderingEngine engine = exchange.get(GroovyTemplateRenderingEngine.class);
    File templateDirFile = exchange.get(FileSystemBinding.class).file(templateDir);
    TemplateRenderer renderer = new DefaultTemplateRenderer(templateDirFile, exchange, engine);
    exchange.insert(exchange.getContext().plus(TemplateRenderer.class, renderer), delegate);
  }
}
