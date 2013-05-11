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

import org.ratpackframework.groovy.templating.TemplateRenderer;
import org.ratpackframework.routing.Exchange;
import org.ratpackframework.routing.Handler;

public class TemplateRenderingErrorHandler implements Handler {

  private final Handler delegate;

  public TemplateRenderingErrorHandler(Handler delegate) {
    this.delegate = delegate;
  }

  @Override
  public void handle(Exchange exchange) {
    try {
      exchange.next(delegate);
    } catch (Exception exception) {
      TemplateRenderer renderer = exchange.get(TemplateRenderer.class);
      renderer.error(ExceptionToTemplateModel.transform(exchange.getRequest(), exception));
    }
  }
}
