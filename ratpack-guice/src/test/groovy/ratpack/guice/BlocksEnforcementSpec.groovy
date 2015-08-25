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

package ratpack.guice

import com.google.inject.AbstractModule
import ratpack.api.Blocks
import ratpack.exec.Blocking
import ratpack.test.internal.RatpackGroovyDslSpec
import ratpack.test.internal.SimpleErrorHandler

class BlocksEnforcementSpec extends RatpackGroovyDslSpec {

  @Blocks
  static class ClassLevelBlocking {
    int m1() { 1 }

    int m2() { 2 }
  }

  static class MethodLevelBlocking {
    int nonblocking() { 1 }

    @Blocks
    int blocking() { 2 }
  }

  def setup() {
    modules << new AbstractModule() {
      @Override
      protected void configure() {
        bind(SimpleErrorHandler)
      }
    }
  }

  def "can enforce blocking at the class level"() {
    when:
    bindings {
      bind ClassLevelBlocking
    }

    handlers {
      get("fail") { ClassLevelBlocking s ->
        s.m1()
        render "ok"
      }
      get("works") { ClassLevelBlocking s ->
        Blocking.get { s.m1() } then { render "ok" }
      }
    }

    then:
    get("works").status.code == 200
    getText("fail").startsWith("ratpack.exec.ExecutionException: Method")
  }


  def "can enforce blocking at the method level"() {
    when:
    bindings {
      bind MethodLevelBlocking
    }

    handlers {
      get("fail") { MethodLevelBlocking s ->
        s.blocking()
        render "ok"
      }
      get("works") { MethodLevelBlocking s ->
        Blocking.get { s.blocking() } then { render "ok" }
      }
    }

    then:
    get("works").status.code == 200
    getText("fail").startsWith("ratpack.exec.ExecutionException: Method")
  }

  def "non annotated methods do not require blocking"() {
    when:
    bindings {
      bind MethodLevelBlocking
    }

    handlers {
      get("works") { MethodLevelBlocking s ->
        s.nonblocking()
        render "ok"
      }
    }

    then:
    get("works").status.code == 200
  }

}
