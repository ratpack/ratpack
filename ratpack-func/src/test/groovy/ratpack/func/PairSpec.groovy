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

package ratpack.func

import spock.lang.Specification

import static ratpack.func.Pair.*

class PairSpec extends Specification {

  def "can create and transform pair"() {
    given:
    def p = pair(1, "a")

    expect:
    p.nestLeft(2).left == pair(2, 1)
    p.nestLeft(2).right == "a"
    p.nestRight(2).left == 1
    p.nestRight(2).right == pair(2, "a")

    p.pushLeft(2).left == 2
    p.pushLeft(2).right == p
    p.pushRight(2).left == p
    p.pushRight(2).right == 2

    p.mapLeft { it + 1 }.left == 2
    p.mapLeft { it + 1 }.right == "a"
    p.mapRight { it * 2 }.left == 1
    p.mapRight { it * 2 }.right == "aa"

    p.nestLeft(2).mapLeft(unpackLeft()).left == 2
    p.nestLeft(2).mapLeft(unpackRight()).left == 1
    p.nestLeft(2).mapLeft(unpackLeft()).right == "a"
    p.nestLeft(2).mapLeft(unpackRight()).right == "a"
    p.nestRight(2).mapRight(unpackLeft()).right == 2
    p.nestRight(2).mapRight(unpackRight()).right == "a"
    p.nestRight(2).mapRight(unpackLeft()).left == 1
    p.nestRight(2).mapRight(unpackRight()).left == 1
  }

}
