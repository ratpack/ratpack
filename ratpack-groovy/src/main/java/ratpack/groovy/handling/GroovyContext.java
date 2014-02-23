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
import ratpack.handling.Context;
import ratpack.handling.RequestOutcome;

/**
 * Subclass of {@link ratpack.handling.Context} that adds Groovy friendly variants of methods.
 */
public interface GroovyContext extends Context {

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyContext getContext();

  /**
   * Responds based on HTTP method.
   *
   * @param closure defines the action to take for different HTTP methods
   */
  void byMethod(@DelegatesTo(GroovyByMethodHandler.class) Closure<?> closure);

  /**
   * Responds based on the client's preferred response content type (i.e. content negotiation).
   *
   * @param closure defines the action to take for the different content types
   */
  void byContent(@DelegatesTo(GroovyByContentHandler.class) Closure<?> closure);

  /**
   * Adds a request close handler.
   *
   * @param closure A closure to call when the request is closed
   */
  void onClose(@DelegatesTo(value = RequestOutcome.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure);

}
