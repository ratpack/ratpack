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

package org.ratpackframework.test.groovy

import org.ratpackframework.groovy.handling.Chain
import org.ratpackframework.groovy.handling.internal.GroovyDslChainActionTransformer
import org.ratpackframework.handling.Handler
import org.ratpackframework.handling.internal.ChainBuilder
import org.ratpackframework.test.DefaultRatpackSpec

import static org.ratpackframework.groovy.util.Closures.action

abstract class RatpackGroovyDslSpec extends DefaultRatpackSpec {

  void handlers(@DelegatesTo(Chain) Closure<?> configurer) {
    this.handlersClosure = configurer
  }

  @Override
  protected Handler createHandler() {
    ChainBuilder.INSTANCE.build(GroovyDslChainActionTransformer.INSTANCE, action(handlersClosure))
  }
}
