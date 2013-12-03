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

package ratpack.guice.internal;

import com.google.inject.Injector;
import ratpack.guice.Guice;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.registry.Registry;

public class InjectorBindingHandler implements Handler {

  private final Handler handler;
  private final Registry registry;

  public InjectorBindingHandler(Injector injector, Handler handler) {
    this.handler = handler;
    this.registry = Guice.registry(injector);
  }

  public void handle(Context context) {
    context.insert(registry, handler);
  }

}
