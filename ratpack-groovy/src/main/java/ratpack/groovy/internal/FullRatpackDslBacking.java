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

package ratpack.groovy.internal;

import groovy.lang.Closure;
import ratpack.groovy.Groovy;

class FullRatpackDslBacking implements Groovy.Ratpack {

  Closure<?> bindingsConfigurer;
  Closure<?> handlersConfigurer;
  Closure<?> serverConfigConfigurer;

  public void bindings(Closure<?> bindingsConfigurer) {
    this.bindingsConfigurer = bindingsConfigurer;
  }

  public void handlers(Closure<?> handlersConfigurer) {
    this.handlersConfigurer = handlersConfigurer;
  }

  public void serverConfig(Closure<?> configConfigurer) {
    this.serverConfigConfigurer = configConfigurer;
  }

  RatpackDslClosures getClosures() {
    return new RatpackDslClosures(serverConfigConfigurer, handlersConfigurer, bindingsConfigurer);
  }

}
