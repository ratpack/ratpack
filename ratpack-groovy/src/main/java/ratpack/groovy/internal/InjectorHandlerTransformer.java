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
import groovy.lang.Closure;
import ratpack.func.Function;
import ratpack.groovy.Groovy;
import ratpack.guice.Guice;
import ratpack.handling.Handler;
import ratpack.launch.LaunchConfig;
import ratpack.registry.Registry;

public class InjectorHandlerTransformer implements Function<Injector, Handler> {

  private final LaunchConfig launchConfig;
  private final Closure<?> closure;

  public InjectorHandlerTransformer(LaunchConfig launchConfig, Closure<?> closure) {
    this.launchConfig = launchConfig;
    this.closure = closure;
  }

  public Handler apply(Injector injector) throws Exception {
    final Registry registry = Guice.justInTimeRegistry(injector);
    return Groovy.chain(launchConfig, registry, closure);
  }

}
