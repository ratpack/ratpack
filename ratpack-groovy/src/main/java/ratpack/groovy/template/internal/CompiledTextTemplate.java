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

package ratpack.groovy.template.internal;

import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import io.netty.buffer.ByteBuf;
import ratpack.groovy.template.TextTemplateModel;

import java.util.Map;

public class CompiledTextTemplate {

  private final Class<DefaultTextTemplateScript> templateClass;
  private final String templateName;

  public CompiledTextTemplate(String templateName, Class<DefaultTextTemplateScript> templateClass) {
    this.templateName = templateName;
    this.templateClass = templateClass;
  }

  void execute(Map<String, ?> model, ByteBuf buffer, NestedRenderer nestedRenderer) {
    @SuppressWarnings("unchecked")
    Map<String, Object> modelTyped = (Map<String, Object>) model;
    TextTemplateModel templateModel = new MapBackedTextTemplateModel(modelTyped);
    DefaultTextTemplateScript script = DefaultGroovyMethods.newInstance(templateClass, new Object[]{templateModel, buffer, nestedRenderer});

    try {
      script.run();
    } catch (Exception e) {
      if (e instanceof InvalidTemplateException) {
        throw (InvalidTemplateException) e;
      } else {
        throw new InvalidTemplateException(templateName, "template execution failed", e);
      }
    }
  }
}
