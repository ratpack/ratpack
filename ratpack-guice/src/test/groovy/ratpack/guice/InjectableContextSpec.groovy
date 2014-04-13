/*
 * Copyright 2014 the original author or authors.
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

package ratpack.guice

import ratpack.exec.internal.Background
import ratpack.handling.Context
import ratpack.exec.ExecContext
import ratpack.handling.InjectionHandler
import ratpack.test.internal.RatpackGroovyDslSpec

import javax.inject.Inject

class InjectableContextSpec extends RatpackGroovyDslSpec {

  static class ContextScopeService {
    final ExecContext context
    final Background background

    @Inject
    ContextScopeService(ExecContext context, Background background) {
      this.context = context
      this.background = background
    }
  }

  def "can inject context and background"() {
    when:
    modules {
      bind ContextScopeService
    }
    handlers {
      handler new InjectionHandler() {
        void handle(Context context, ContextScopeService service) {
          assert service.context.is(context)
          context.render "ok"
        }
      }
    }

    then:
    text == "ok"
  }
}
