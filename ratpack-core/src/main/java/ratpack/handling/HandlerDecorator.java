/*
 * Copyright 2015 the original author or authors.
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

package ratpack.handling;

import ratpack.registry.Registry;

public interface HandlerDecorator {

  Handler decorate(Registry serverRegistry, Handler rest) throws Exception;

  static HandlerDecorator prepend(Handler handler) {
    return (registry, rest) -> Handlers.chain(registry, c -> c.handler(handler).handler(rest));
  }

  static HandlerDecorator passThru() {
    return (registry, rest) -> rest;
  }

}
