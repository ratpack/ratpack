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

package org.ratpackframework.handlebars;

import java.util.Map;

public class HandlebarsTemplate {

  private final String name;
  private final Object model;

  public String getName() {
    return name;
  }

  public Object getModel() {
    return model;
  }

  public HandlebarsTemplate(String name, Object model) {
    this.name = name;
    this.model = model;
  }

  public static HandlebarsTemplate handlebarsTemplate(Map<String, ?> model, String name) {
    return new HandlebarsTemplate(name, model);
  }
}
