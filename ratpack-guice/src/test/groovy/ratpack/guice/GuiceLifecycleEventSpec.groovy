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

package ratpack.guice

import ratpack.server.Service
import ratpack.server.StartEvent
import ratpack.test.internal.RatpackGroovyDslSpec

import javax.inject.Inject

class GuiceLifecycleEventSpec extends RatpackGroovyDslSpec {

  @com.google.inject.Singleton
  static class Thing {
    final List<String> strings = []
  }

  static class ThingInit implements Service {
    final Thing thing

    @Inject
    ThingInit(Thing thing) {
      this.thing = thing
    }

    @Override
    void onStart(StartEvent event) {
      thing.strings << 'bar'
    }
  }

  def "can register lifecycle listeners"() {
    when:
    bindings {
      bind Thing
      bindInstance Service, new Service() {
        @Override
        void onStart(StartEvent event) {
          event.registry.get(Thing).strings << 'foo'
        }
      }
      bind ThingInit
    }
    handlers {
      get { Thing thing ->
        render thing.strings.toString()
      }
    }

    then:
    text == ['bar', 'foo'].toString()
  }

}
