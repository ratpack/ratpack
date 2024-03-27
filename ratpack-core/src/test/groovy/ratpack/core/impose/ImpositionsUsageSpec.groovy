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

package ratpack.core.impose

import spock.lang.Specification

class ImpositionsUsageSpec extends Specification {

  def "can impose multiple times"() {
    when:
    def ports = Impositions.of { it.add(ForceServerListenPortImposition.of(1)) }.impose {
      Impositions.of { it.add(ForceServerListenPortImposition.of(2)) }.impose {
        Impositions.of { it.add(ForceServerListenPortImposition.of(3)) }.impose {
          Impositions.current().getAll(ForceServerListenPortImposition).collect { it.port }
        }
      }
    }

    then:
    ports == [1, 2, 3]
  }

  def "can impose over"() {
    when:
    def ports = Impositions.of { it.add(ForceServerListenPortImposition.of(1)) }.impose {
      Impositions.of { it.add(ForceServerListenPortImposition.of(2)) }.impose {
        Impositions.of { it.add(ForceServerListenPortImposition.of(3)) }.impose {
          Impositions.current().impose { Impositions.current().getAll(ForceServerListenPortImposition).collect { it.port } }
        }
      }
    }

    then:
    ports == [1, 2, 3, 1, 2, 3]

    when:
    ports = Impositions.of { it.add(ForceServerListenPortImposition.of(1)) }.impose {
      Impositions.of { it.add(ForceServerListenPortImposition.of(2)) }.impose {
        Impositions.of { it.add(ForceServerListenPortImposition.of(3)) }.impose {
          Impositions.current().imposeOver { Impositions.current().getAll(ForceServerListenPortImposition).collect { it.port } }
        }
      }
    }

    then:
    ports == [1, 2, 3]
  }
}
