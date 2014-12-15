/*
 * Copyright 2014 the original author or authors.
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
package ratpack.groovy.template;

import java.util.Map;

public class MarkupTemplate {
  private static final String DEFAULT_EXTENSION = ".gtpl";
  private static final String DEFAULT_CONTENT_TYPE = "text/html;charset=UTF-8";

  private final String name;
  private final String contentType;
  private final Map<?, ?> model;

  public MarkupTemplate(String name, String contentType, Map<?, ?> model) {
    this.name = name;
    this.contentType = contentType != null ? contentType : (name.endsWith(DEFAULT_EXTENSION) ? DEFAULT_CONTENT_TYPE : null);
    this.model = model;
  }

  public String getName() {
    return name;
  }

  public String getContentType() {
    return contentType;
  }

  public Map<?, ?> getModel() {
    return model;
  }
}
