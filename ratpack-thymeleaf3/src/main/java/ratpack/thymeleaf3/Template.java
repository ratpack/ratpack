/*
 * Copyright 2018 the original author or authors.
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

package ratpack.thymeleaf3;

import org.thymeleaf.context.IContext;
import org.thymeleaf.context.WebContext;
import ratpack.thymeleaf3.internal.ThymeleafHttpServletRequestAdapter;
import ratpack.thymeleaf3.internal.ThymeleafHttpServletResponseAdapter;
import ratpack.thymeleaf3.internal.ThymeleafServletContextAdapter;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public final class Template {

  private final String name;
  private final IContext context;

  private Template(String name, IContext context) {
    this.name = name;
    this.context = context;
  }

  public String getName() {
    return name;
  }

  public IContext getContext() {
    return context;
  }

  public static Template thymeleafTemplate(String name) {
    return thymeleafTemplate(name, (Map<String, Object>) null);
  }

  public static Template thymeleafTemplate(String name, IContext context) {
    return new Template(name, context);
  }

  public static Template thymeleafTemplate(String name, Map<String, Object> model) {
    HttpServletRequest request = new ThymeleafHttpServletRequestAdapter();
    HttpServletResponse response = new ThymeleafHttpServletResponseAdapter();
    ServletContext servletContext = new ThymeleafServletContextAdapter();
    return new Template(name, new WebContext(request, response, servletContext, null, model));
  }

}
