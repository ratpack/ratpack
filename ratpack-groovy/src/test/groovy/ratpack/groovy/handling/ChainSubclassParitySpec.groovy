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

package ratpack.groovy.handling

import ratpack.handling.Chain
import ratpack.handling.Handler
import spock.lang.Specification

import java.lang.reflect.Modifier

class ChainSubclassParitySpec extends Specification {

  def javaType = Chain
  def groovyType = GroovyChain

  def "groovy chain subclass overrides universally overrides return type"() {
    /*
      We are testing here that the Groovy subclass overrides all of the Java methods
      in order to specify that it returns the Groovy type
     */

    given:
    def javaChainReturningMethods = javaType.getDeclaredMethods().findAll {
      it.returnType.equals(javaType) && Modifier.isPublic(it.modifiers)
    }

    expect:
    javaChainReturningMethods.each {
      try {
        def override = groovyType.getDeclaredMethod(it.name, it.parameterTypes)
        assert override.returnType == groovyType
      } catch (NoSuchMethodException ignore) {
        throw new AssertionError("Chain method $it is not overridden in groovy subclass")
      }
    }
  }

  def "groovy chain subclass has closure overloads for all handler methods"() {
    given:
    def javaChainHandlerAsLastArgMethods = javaType.declaredMethods.findAll {
      it.parameterTypes.size() > 0 && it.parameterTypes.last() == Handler
    }

    expect:
    javaChainHandlerAsLastArgMethods.each {
      try {
        def paramList = it.parameterTypes.toList()
        paramList[paramList.size() - 1] = Closure
        def override = groovyType.getDeclaredMethod(it.name, (Class[]) paramList.toArray())
        assert override.returnType == GroovyChain
        assert override.returnType == groovyType
      } catch (NoSuchMethodException ignore) {
        throw new AssertionError("Chain method $it is not overridden in groovy subclass")
      }
    }
  }
}
