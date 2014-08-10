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

import ratpack.exec.Execution
import ratpack.handling.Context
import ratpack.handling.InjectionHandler
import ratpack.test.internal.RatpackGroovyDslSpec

import javax.inject.Inject

class InjectableExecutionSpec extends RatpackGroovyDslSpec {

  static class InjectionScopeService {
    final Execution execution

    @Inject
    InjectionScopeService(Execution execution) {
      this.execution = execution
    }
  }

  def "can inject context"() {
    when:
    bindings {
      bind InjectionScopeService
    }
    handlers {
      handler new InjectionHandler() {
        void handle(Context context, InjectionScopeService service) {
          assert service.execution.is(context.launchConfig.execController.control.execution)
          context.render "ok"
        }
      }
    }

    then:
    text == "ok"
  }
}
