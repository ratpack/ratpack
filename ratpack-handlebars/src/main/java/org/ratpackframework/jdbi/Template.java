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

package org.ratpackframework.jdbi;

import java.util.Map;

public class Template<T> {

  private final String name;
  private final T model;

  private final String contentType;

  public String getName() {
    return name;
  }

  public T getModel() {
    return model;
  }

  public String getContentType() {
    return contentType;
  }

  private Template(String name, T model, String contentType) {
    this.name = name;
    this.model = model;
    this.contentType = contentType;
  }

  public static Template<Object> handlebarsTemplate(String name) {
    return handlebarsTemplate(name, null);
  }

  public static Template<Map<String, ?>> handlebarsTemplate(Map<String, ?> model, String name) {
    return handlebarsTemplate(model, name, null);
  }

  public static Template<Map<String, ?>> handlebarsTemplate(Map<String, ?> model, String name, String contentType) {
    return new Template<Map<String, ?>>(name, model, contentType);
  }

  public static <T> Template<T> handlebarsTemplate(String name, T model) {
    return handlebarsTemplate(name, model, null);
  }

  public static <T> Template<T> handlebarsTemplate(String name, T model, String contentType) {
    return new Template<>(name, model, contentType);
  }
}
