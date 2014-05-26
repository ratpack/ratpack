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

package ratpack.groovy.launch.internal;

import com.google.inject.Injector;
import com.google.inject.Module;
import groovy.lang.Closure;
import ratpack.func.Function;
import ratpack.groovy.internal.RatpackDslClosureToHandlerTransformer;
import ratpack.groovy.server.internal.GroovyKitAppFactory;
import ratpack.guice.Guice;
import ratpack.guice.GuiceBackedHandlerFactory;
import ratpack.handling.Handler;
import ratpack.launch.HandlerFactory;
import ratpack.launch.LaunchConfig;

public class GroovyClosureHandlerFactory implements HandlerFactory {

  private final Closure<?> ratpackClosure;

  public GroovyClosureHandlerFactory(Closure<?> ratpackClosure) {
    this.ratpackClosure = ratpackClosure;
  }

  @Override
  public Handler create(LaunchConfig launchConfig) throws Exception {
    GuiceBackedHandlerFactory guiceHandlerFactory = new GroovyKitAppFactory(launchConfig);
    Function<Module, Injector> moduleInjectorTransformer = Guice.newInjectorFactory(launchConfig);
    Function<Closure<?>, Handler> handlerTransformer = new RatpackDslClosureToHandlerTransformer(launchConfig, guiceHandlerFactory, moduleInjectorTransformer);
    return handlerTransformer.transform(ratpackClosure);
  }

}
