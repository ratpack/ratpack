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

package ratpack.groovy.markup;

import groovy.lang.Closure;

import java.nio.charset.Charset;

/**
 * Render type for markup to be built using Groovy's {@link groovy.xml.MarkupBuilder}.
 *
 * @see ratpack.groovy.Groovy#markupBuilder(String, String, groovy.lang.Closure)
 */
public interface Markup {

  /**
   * The content type of the markup.
   *
   * @return The content type of the markup.
   */
  CharSequence getContentType();

  /**
   * The character encoding of the markup.
   *
   * @return The character encoding of the markup.
   */
  Charset getEncoding();

  /**
   * The closure that defines the markup.
   *
   * @return The closure that defines the markup.
   */
  Closure<?> getDefinition();

}
