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

package ratpack.groovy.handling;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import ratpack.handling.ByContentSpec;

/**
 * Closure overloads of methods from {@link ratpack.handling.ByContentSpec}.
 *
 * @see ratpack.groovy.handling.GroovyContext#byContent(groovy.lang.Closure)
 */
public interface GroovyByContentSpec extends ByContentSpec {

  /**
   * Register how to respond with the given mime type.
   *
   * @param mimeType The mime type to register for
   * @param closure The action to take if the client wants to given type
   * @return this
   */
  GroovyByContentSpec type(String mimeType, @DelegatesTo(GroovyContext.class) Closure<?> closure);

  /**
   * Convenience method to respond with "text/plain" mime type.
   *
   * @param closure the action to take if the client wants plain text
   * @return this
   */
  GroovyByContentSpec plainText(@DelegatesTo(GroovyContext.class) Closure<?> closure);

  /**
   * Convenience method to respond with "text/html" mime type.
   *
   * @param closure the action to take if the client wants html
   * @return this
   */
  GroovyByContentSpec html(@DelegatesTo(GroovyContext.class) Closure<?> closure);

  /**
   * Convenience method to respond with "application/json" mime type.
   *
   * @param closure the action to take if the client wants json
   * @return this
   */
  GroovyByContentSpec json(@DelegatesTo(GroovyContext.class) Closure<?> closure);

  /**
   * Convenience method to respond with "application/xml" mime type.
   *
   * @param closure the action to take if the client wants xml
   * @return this
   */
  GroovyByContentSpec xml(@DelegatesTo(GroovyContext.class) Closure<?> closure);

}
