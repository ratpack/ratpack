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

package ratpack.gradle

import spock.lang.Specification
import spock.lang.Unroll

class GradleVersionSpec extends Specification {

  @Unroll
  def "can handle different gradle version"() {
    when:
    def version = GradleVersion.version(gradleVersion)

    then:
    version.version == gradleVersion
    version.valid
    version.nextMajor.version == nextMajor
    version.snapshot == snapshot

    where:
    gradleVersion                                                            | nextMajor | snapshot
    '4.0'                                                                    | "5.0"     | false
    '5.0.1'                                                                  | "6.0"     | false
    '5.0.1-rc-1'                                                             | "6.0"     | false
    '5.2-20200206000044+0000'                                                | "6.0"     | true
    '6.3-branch-bamboo_master_ie_dogfood_gradleProperty-20200205222034+0000' | "7.0"     | true
  }

}
