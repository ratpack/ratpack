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

package ratpack.groovy.templating.internal;

import com.google.common.collect.ImmutableMap;
import ratpack.groovy.templating.Template;

import java.util.Map;

public class DefaultTemplate implements Template {

  private final String id;
  private final ImmutableMap<String, ?> model;
  private final String type;

  public DefaultTemplate(String id, Map<String, ?> model, String type) {
    this.id = id;
    this.type = type;
    this.model = ImmutableMap.copyOf(model);
  }

  @Override public String getId() {
    return id;
  }

  @Override public String getType() {
    return type;
  }

  @Override public ImmutableMap<String, ?> getModel() {
    return model;
  }

  public static Template groovyTemplate(String id) {
    return groovyTemplate(id, null);
  }

  public static Template groovyTemplate(String id, String type) {
    return groovyTemplate(ImmutableMap.<String, Object>of(), id, type);
  }

  public static Template groovyTemplate(Map<String, ?> model, String id) {
    return groovyTemplate(model, id, null);
  }

  public static Template groovyTemplate(Map<String, ?> model, String id, String type) {
    return new DefaultTemplate(id, model, type);
  }

}
