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

package org.ratpackframework.groovy.internal;

import com.google.inject.Injector;
import groovy.lang.Closure;
import org.ratpackframework.groovy.handling.Chain;
import org.ratpackframework.groovy.handling.internal.GroovyDslChainActionTransformer;
import org.ratpackframework.guice.internal.JustInTimeInjectorRegistry;
import org.ratpackframework.handling.Handler;
import org.ratpackframework.handling.internal.ChainBuilder;
import org.ratpackframework.registry.Registry;
import org.ratpackframework.util.Action;
import org.ratpackframework.util.Transformer;

public class InjectorHandlerTransformer implements Transformer<Injector, Handler> {

  private final Closure<?> closure;

  public InjectorHandlerTransformer(Closure<?> closure) {
    this.closure = closure;
  }

  public Handler transform(Injector injector) {
    final Registry registry = new JustInTimeInjectorRegistry(injector);

    Action<Chain> chainAction = new Action<Chain>() {
      public void execute(Chain chain) {
        ClosureInvoker<Object, Chain> closureInvoker = new ClosureInvoker<Object, Chain>(closure);
        closureInvoker.invoke(registry, chain, Closure.DELEGATE_FIRST);
      }
    };

    return ChainBuilder.INSTANCE.buildHandler(new GroovyDslChainActionTransformer(registry), chainAction);
  }

}
