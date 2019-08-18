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

package ratpack.groovy.internal

import spock.lang.Specification

class GroovyVersionCheckSpec extends Specification {

  void '#version is a supported groovy runtime version'() {
    when:
    GroovyVersionCheck.ensureRequiredVersionUsed(version, "2.2.1")

    then:
    notThrown(RuntimeException)

    where:
    version << ['2.2.1', '2.2.2', '2.3.0', '2.10.0', '3.0.0']
  }

  void '#version is not a supported groovy runtime version'() {
    when:
    GroovyVersionCheck.ensureRequiredVersionUsed(version, "2.2.1")

    then:
    def e = thrown(RuntimeException)
    e.message == "Ratpack requires Groovy 2.2.1+ to run but the version used is $version"

    where:
    version << ['1.8.9', '2.1.9', '2.2.0']
  }
}
