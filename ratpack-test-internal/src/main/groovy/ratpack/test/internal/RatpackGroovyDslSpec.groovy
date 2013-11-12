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

package ratpack.test.internal

import com.google.inject.Injector
import ratpack.groovy.handling.GroovyChain
import ratpack.groovy.internal.InjectorHandlerTransformer
import ratpack.handling.Handler
import ratpack.launch.LaunchConfig
import ratpack.util.Transformer

abstract class RatpackGroovyDslSpec extends DefaultRatpackSpec {

  void handlers(@DelegatesTo(value = GroovyChain, strategy = Closure.DELEGATE_FIRST) Closure<?> configurer) {
    this.handlersClosure = configurer
  }

  @Override
  protected Transformer<Injector, Handler> createHandlerTransformer(LaunchConfig launchConfig) {
    new InjectorHandlerTransformer(launchConfig, handlersClosure);
  }
}
