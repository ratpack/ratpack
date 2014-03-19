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

package ratpack.groovy.handling.internal;

import groovy.lang.Closure;
import ratpack.groovy.Groovy;
import ratpack.groovy.handling.GroovyContext;
import ratpack.groovy.internal.ClosureInvoker;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.registry.Registries;

public class ClosureBackedHandler implements Handler {

  private final ClosureInvoker<?, GroovyContext> invoker;

  public ClosureBackedHandler(Closure<?> closure) {
    this.invoker = new ClosureInvoker<Object, GroovyContext>(closure);
  }

  public void handle(Context context) {
    invoker.invoke(Registries.join(context.getRequest(), context), Groovy.context(context), Closure.DELEGATE_FIRST);
  }
}
