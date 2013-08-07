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

package org.ratpackframework.groovy;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class Template {

  private final String id;
  private final ImmutableMap<String, ?> model;

  public Template(String id, Map<String, ?> model) {
    this.id = id;
    this.model = ImmutableMap.copyOf(model);
  }

  public String getId() {
    return id;
  }

  public ImmutableMap<String, ?> getModel() {
    return model;
  }

  public static Template groovyTemplate(String id) {
    return new Template(id, ImmutableMap.<String, Object>of());
  }

  public static Template groovyTemplate(Map<String, ?> model, String id) {
    return new Template(id, model);
  }

}
