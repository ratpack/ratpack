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

import com.google.inject.Injector
import ratpack.func.Action
import ratpack.test.internal.RatpackGroovyDslSpec

import javax.inject.Inject

class GuiceInitSpec extends RatpackGroovyDslSpec {

  @com.google.inject.Singleton
  static class Thing {
    final List<String> strings = []
  }

  static class ThingInit implements Runnable {
    final Thing thing

    @Inject
    ThingInit(Thing thing) {
      this.thing = thing
    }

    @Override
    void run() {
      thing.strings << "bar"
    }
  }

  def "can register init callbacks"() {
    when:
    modules {
      bind Thing
      init new Action<Injector>() {
        @Override
        void execute(Injector injector) throws Exception {
          injector.getInstance(Thing).strings << "foo"
        }
      }
      init ThingInit
      init { Thing thing -> thing.strings << "baz" }
    }
    handlers {
      get { Thing thing ->
        render thing.strings.toString()
      }
    }

    then:
    text == ["foo", "bar", "baz"].toString()
  }

}
