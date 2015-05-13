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

import com.google.common.reflect.TypeToken
import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import com.google.inject.name.Names
import ratpack.test.internal.RatpackGroovyDslSpec

class GenericTypeLookupSpec extends RatpackGroovyDslSpec {

  def "can retrieve via type tokens"() {
    given:
    modules << new AbstractModule() {
      @Override
      protected void configure() {
        bind(new TypeLiteral<List<String>>() {}).toInstance(["a"])
      }
    }

    when:
    handlers {
      get {
        def strings = get(new TypeToken<List<String>>() {})
        render strings.get(0)
      }
    }

    then:
    text == "a"
  }

  def "can retrieve all via type tokens"() {
    given:
    modules << new AbstractModule() {
      @Override
      protected void configure() {
        bind(new TypeLiteral<List<String>>() {}).annotatedWith(Names.named("a"))toInstance(["a"])
        bind(new TypeLiteral<List<String>>() {}).annotatedWith(Names.named("b"))toInstance(["b"])
      }
    }

    when:
    handlers {
      get {
        def strings = getAll(new TypeToken<List<String>>() {})
        render strings.collect { it[0] }.join(":")
      }
    }

    then:
    text == "b:a"
  }

  def "can retrieve all via supertype type tokens"() {
    given:
    modules << new AbstractModule() {
      @Override
      protected void configure() {
        bind(new TypeLiteral<List<String>>() {}).annotatedWith(Names.named("a"))toInstance(["a"])
        bind(new TypeLiteral<List<String>>() {}).annotatedWith(Names.named("b"))toInstance(["b"])
      }
    }

    when:
    handlers {
      get {
        def strings = getAll(new TypeToken<List<? extends CharSequence>>() {})
        render strings.collect { it[0] }.join(":")
      }
    }

    then:
    text == "b:a"
  }
}
