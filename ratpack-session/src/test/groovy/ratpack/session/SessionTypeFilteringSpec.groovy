/*
 * Copyright 2021 the original author or authors.
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

package ratpack.session

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import ratpack.api.Nullable
import ratpack.error.ServerErrorHandler
import ratpack.handling.Context
import ratpack.test.internal.RatpackGroovyDslSpec

class SessionTypeFilteringSpec extends RatpackGroovyDslSpec {

  boolean allowAll

  def setup() {
    modules << new SessionModule()
    modules << new AbstractModule() {
      @Override
      protected void configure() {
        bind(ServerErrorHandler).toInstance(new ServerErrorHandler() {
          @Override
          void error(Context context, Throwable throwable) throws Exception {
            context.response.status(500)
            context.render(throwable.toString())
          }
        })
        Multibinder.newSetBinder(binder(), SessionTypeFilterPlugin).addBinding().toInstance(new SessionTypeFilterPlugin() {
          @Override
          boolean allow(String type) {
            return allowAll
          }
        })
      }
    }
  }

  @SuppressWarnings('GrUnnecessaryPublicModifier')
  public <T> void addHandlersFor(Class<T> type, T obj) {
    handlers {
      get("read") {
        get(Session).get(type).then { render it.map { "true" }.orElse("false") }
      }
      get("write") {
        get(Session).set(type, obj).then { render "ok" }
      }
      get("forceWrite") {
        allowAll = true
        get(Session).set(type, obj).then {
          allowAll = false
          render "ok"
        }
      }
    }
    server.stop()
  }

  @SuppressWarnings('GrUnnecessaryPublicModifier')
  public <T> void testFor(Class<?> failingType = null) {
    if (failingType == null) {
      assert getText("write") == "ok"
    } else {
      assert getText("write") == new NonAllowedSessionTypeException(failingType.name).toString()
    }

    assert getText("forceWrite") == "ok"

    if (failingType == null) {
      assert getText("read") == "true"
    } else {
      assert getText("read") == "false"
    }
  }

  @SuppressWarnings('GrUnnecessaryPublicModifier')
  public <T> void test(Class<T> type, T obj, @Nullable Class<?> failingType = null) {
    addHandlersFor(type, obj)
    testFor(failingType)
  }

  def "allows JDK types"() {
    expect:
    test(String, "foo")
    test(Integer, 1)
    test(List, [[1, 2] as int[]])
    test(List, ["foo"])
  }

  static class Outer implements Serializable {
    String str = "str"
    static class Child implements Serializable {

    }
    Child child = new Child()
  }

  def "validates transient types"() {
    given:
    bindings {
      binder { SessionModule.allowTypes(it, Outer) }
    }

    expect:
    test(Outer, new Outer(), Outer.Child)
  }

  def "can allow transient types"() {
    given:
    bindings {
      binder { SessionModule.allowTypes(it, Outer, Outer.Child) }
    }

    expect:
    test(Outer, new Outer())
  }

}
