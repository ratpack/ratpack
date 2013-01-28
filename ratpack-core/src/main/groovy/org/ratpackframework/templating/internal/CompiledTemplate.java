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

package org.ratpackframework.templating.internal;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CompiledTemplate {

  private final Class<TemplateScript> templateClass;

  public CompiledTemplate(Class<TemplateScript> templateClass) {
    this.templateClass = templateClass;
  }

  ExecutedTemplate execute(Map<String, ?> model, NestedRenderer nestedRenderer) {
    TemplateScript script;
    try {
      script = templateClass.newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    List<Object> parts = new LinkedList<Object>();
    script.setParts(parts);
    @SuppressWarnings("unchecked")
    Map<String, Object> modelTyped = (Map<String, Object>) model;
    script.setModel(new MapBackedTemplateModel(modelTyped));
    script.setRenderer(nestedRenderer);
    script.run();
    return new ExecutedTemplate(parts);
  }
}
