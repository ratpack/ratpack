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

import org.ratpackframework.groovy.templating.internal.TemplateRendererBindingHandler;
import org.ratpackframework.groovy.templating.internal.TemplateRenderingErrorHandler;
import org.ratpackframework.routing.Handler;

public abstract class TemplatingHandlers {

  public static Handler templates(String templatesDir, Handler handler) {
    return new TemplateRendererBindingHandler(templatesDir, new TemplateRenderingErrorHandler(handler));
  }
}
