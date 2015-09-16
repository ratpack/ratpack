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
import org.thymeleaf.context.WebContext;
import org.thymeleaf.fragment.IFragmentSpec;
import org.thymeleaf.fragment.WholeFragmentSpec;
import ratpack.thymeleaf.internal.ThymeleafHttpServletRequestAdapter;
import ratpack.thymeleaf.internal.ThymeleafHttpServletResponseAdapter;
import ratpack.thymeleaf.internal.ThymeleafServletContextAdapter;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

public class Template {

  private final String name;
  private final WebContext model;
  private final String contentType;
  private final IFragmentSpec fragmentSpec;

  public String getName() {
    return name;
  }

  public WebContext getModel() {
    return model;
  }

  public String getContentType() {
    return contentType;
  }

  public IFragmentSpec getFragmentSpec() {
    return fragmentSpec;
  }

  private Template(String name, WebContext model, String contentType, IFragmentSpec fragmentSpec) {
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
    HttpServletRequest request = new ThymeleafHttpServletRequestAdapter();
    HttpServletResponse response = new ThymeleafHttpServletResponseAdapter();
    ServletContext servletContext = new ThymeleafServletContextAdapter();
    WebContext context = new WebContext(request, response, servletContext);
    if (model != null) {
      context.setVariables(model);
    }

    return new Template(name, context, contentType, fragmentSpec);
  }
}
