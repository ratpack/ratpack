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

package ratpack.groovy.internal;

import com.google.inject.Injector;
import com.google.inject.Module;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.groovy.guice.internal.DefaultGroovyBindingsSpec;
import ratpack.guice.BindingsSpec;
import ratpack.guice.GuiceBackedHandlerFactory;
import ratpack.handling.Handler;
import ratpack.server.ServerConfig;

public class RatpackDslClosureToHandlerTransformer implements Function<RatpackDslClosures, Handler> {

  private final ServerConfig serverConfig;
  private final GuiceBackedHandlerFactory handlerFactory;
  private final Function<? super Module, ? extends Injector> moduleTransformer;

  public RatpackDslClosureToHandlerTransformer(ServerConfig serverConfig, GuiceBackedHandlerFactory handlerFactory, Function<? super Module, ? extends Injector> moduleTransformer) {
    this.serverConfig = serverConfig;
    this.handlerFactory = handlerFactory;
    this.moduleTransformer = moduleTransformer;
  }

  @Override
  public Handler apply(RatpackDslClosures ratpackDslClosures) throws Exception {
    Action<BindingsSpec> bindingsAction = new Action<BindingsSpec>() {
      @Override
      public void execute(BindingsSpec thing) throws Exception {
        ClosureUtil.delegatingAction(ratpackDslClosures.getBindings()).execute(new DefaultGroovyBindingsSpec(thing));
      }
    };

    return handlerFactory.create(bindingsAction, moduleTransformer, new InjectorHandlerTransformer(serverConfig, ratpackDslClosures.getHandlers()));
  }

}
