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
import org.thymeleaf.fragment.IFragmentSpec;
import org.thymeleaf.fragment.WholeFragmentSpec;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

public class Template {

  private final String name;
  private final Context model;
  private final String contentType;
  private final IFragmentSpec fragmentSpec;

  public String getName() {
    return name;
  }

  public Context getModel() {
    return model;
  }

  public String getContentType() {
    return contentType;
  }

  public IFragmentSpec getFragmentSpec() {
    return fragmentSpec;
  }

  private Template(String name, Context model, String contentType, IFragmentSpec fragmentSpec) {
    this.name = name;
    this.model = model;
    this.contentType = contentType;
    this.fragmentSpec = fragmentSpec;
  }

  public static Template thymeleafTemplate(String name) {
    return thymeleafTemplate(Collections.emptyMap(), name);
  }

  public static Template thymeleafTemplate(String name, IFragmentSpec fragmentSpec) {
    return thymeleafTemplate(Collections.emptyMap(), name, fragmentSpec);
  }

  public static Template thymeleafTemplate(Map<String, ?> model, String name) {
    return thymeleafTemplate(model, name, (String) null);
  }

  public static Template thymeleafTemplate(Map<String, ?> model, String name, IFragmentSpec fragmentSpec) {
    return thymeleafTemplate(model, name, null, fragmentSpec);
  }

  public static Template thymeleafTemplate(String name, Consumer<? super ImmutableMap.Builder<String, Object>> modelBuilder) {
    return thymeleafTemplate(name, modelBuilder, WholeFragmentSpec.INSTANCE);
  }

  public static Template thymeleafTemplate(String name, Consumer<? super ImmutableMap.Builder<String, Object>> modelBuilder, IFragmentSpec fragmentSpec) {
    return thymeleafTemplate(name, null, modelBuilder, fragmentSpec);
  }

  public static Template thymeleafTemplate(String name, String contentType, Consumer<? super ImmutableMap.Builder<String, Object>> modelBuilder) {
    return thymeleafTemplate(name, contentType, modelBuilder, WholeFragmentSpec.INSTANCE);
  }

  public static Template thymeleafTemplate(String name, String contentType, Consumer<? super ImmutableMap.Builder<String, Object>> modelBuilder, IFragmentSpec fragmentSpec) {
    ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
    modelBuilder.accept(builder);
    return thymeleafTemplate(builder.build(), name, contentType, fragmentSpec);
  }

  public static Template thymeleafTemplate(Map<String, ?> model, String name, String contentType) {
    return thymeleafTemplate(model, name, contentType, WholeFragmentSpec.INSTANCE);
  }


  public static Template thymeleafTemplate(Map<String, ?> model, String name, String contentType, IFragmentSpec fragmentSpec) {
    Context context = new Context();
    if (model != null) {
      context.setVariables(model);
    }

    return new Template(name, context, contentType, fragmentSpec);
  }
}
