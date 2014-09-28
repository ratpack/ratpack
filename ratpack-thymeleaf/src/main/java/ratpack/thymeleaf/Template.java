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

package ratpack.thymeleaf;

import com.google.common.collect.ImmutableMap;
import org.thymeleaf.context.Context;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

public class Template {

  private final String name;
  private final Context model;
  private final String contentType;

  public String getName() {
    return name;
  }

  public Context getModel() {
    return model;
  }

  public String getContentType() {
    return contentType;
  }

  private Template(String name, Context model, String contentType) {
    this.name = name;
    this.model = model;
    this.contentType = contentType;
  }

  public static Template thymeleafTemplate(String name) {
    return thymeleafTemplate(Collections.emptyMap(), name);
  }

  public static Template thymeleafTemplate(String name, Consumer<? super ImmutableMap.Builder<String, Object>> modelBuilder) {
    return thymeleafTemplate(name, null, modelBuilder);
  }

  public static Template thymeleafTemplate(Map<String, ?> model, String name) {
    return thymeleafTemplate(model, name, null);
  }

  public static Template thymeleafTemplate(String name, String contentType, Consumer<? super ImmutableMap.Builder<String, Object>> modelBuilder) {
    ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
    modelBuilder.accept(builder);
    return thymeleafTemplate(builder.build(), name, contentType);
  }

  public static Template thymeleafTemplate(Map<String, ?> model, String name, String contentType) {
    Context context = new Context();
    if (model != null) {
      context.setVariables(model);
    }

    return new Template(name, context, contentType);
  }
}
