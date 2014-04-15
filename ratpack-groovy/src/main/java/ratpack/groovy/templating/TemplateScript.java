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

package ratpack.groovy.templating;

import java.util.Map;

/**
 * The API available in template files.
 */
@SuppressWarnings("UnusedDeclaration")
public interface TemplateScript {

  /**
   * The backing model for this template.
   *
   * @return The backing model for this template
   */
  TemplateModel getModel();

  /**
   * Renders a nested template inline, using the same model as this template.
   *
   * @param templateName The name of the template to render
   * @return An empty string
   * @throws Exception if an error occurs compiling/executing the template
   * @see #render(java.util.Map, String)
   */
  String render(String templateName) throws Exception;

  /**
   * Renders a nested template inline, with the given model merged with the current template model.
   * <p>
   * The nested template will be rendered directly to the underlying buffer; it is not returned from this method.
   * This method returns an empty string so that it can be used in situations where the return value would have been
   * included in the output (e.g. a {@code <?= ?>} block).
   * <p>
   * The template name is resolved into a template using the same renderer that initiated rendering of this template.
   *
   * @param templateName The name of the template to render
   * @return The rendered template content
   * @throws Exception if an error occurs compiling/executing the template
   */
  String render(Map<String, ?> model, String templateName) throws Exception;

}
