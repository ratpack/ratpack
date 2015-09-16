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

package ratpack.gradle.functional

class DslSpec extends FunctionalSpec {

  def "can use extension to add dependencies"() {
    given:
    buildFile << """
      configurations.all {
        transitive = false // we don't need jackson itself for this test
      }
      dependencies {
        compile ratpack.dependency("jackson-guice")
      }

      task showDeps {
        doLast { file("deps.txt") << configurations.compile.dependencies*.toString().join('\\n') }
      }
    """

    when:
    run "showDeps"

    then:
    file("deps.txt").text.contains("ratpack-jackson-guice")
  }

}
