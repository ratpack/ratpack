/*
 * Copyright 2016 the original author or authors.
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

package ratpack.func

import spock.lang.Specification

import static ratpack.func.Action.conditional
import static ratpack.func.Action.noop

class ActionSpec extends Specification {

  def "can conditional execute actions"() {
    given:
    int count = 0
    Action<Integer> a = Action.when({ i -> i == 1 }, { i -> count += 1 }, { i -> count += 2})

    when:
    a.execute(1)

    then:
    count == 1

    when:
    count = 0
    a.execute(10)

    then:
    count == 2
  }

  def "can build conditional action specs"() {
    given:
    int count = 0
    Action<String> a = conditional(noop(), { s -> s
      .when({ String i -> i.length() < 2 }, { String i -> count += 2})
      .when({ String i -> i.length() < 1 }, { String i -> count += 1})
    })

    when:
    a.execute("a")

    then:
    count == 2

    when:
    count = 0
    a.execute("aaaa")

    then:
    count == 0

  }
}
