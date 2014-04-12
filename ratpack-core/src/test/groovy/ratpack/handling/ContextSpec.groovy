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

package ratpack.handling

import spock.lang.Specification

class ContextSpec extends Specification {

  def "context interface redeclares all inherited methods"() {
    // This is such a key type, that we should make it easier for people to read the docs by having all its methods present as first class
    when:
    checkMethods(Context.interfaces)

    then:
    noExceptionThrown()
  }

  void checkMethods(Class<?>[] interfaces) {
    interfaces.each {
      it.declaredMethods.each {
        Context.getDeclaredMethod(it.name, it.parameterTypes)
      }
      checkMethods(it.interfaces)
    }
  }
}
