/*
 * Copyright 2018 the original author or authors.
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

package ratpack.stream.internal

import ratpack.exec.BaseExecutionSpec
import ratpack.exec.Promise
import ratpack.stream.Streams

class FlatMapPublisherSpec extends BaseExecutionSpec {

  def "does not excessively request from upstream"() {
    expect:
    def c = new MaxAwareCounter()
    execHarness.yield {
      Streams.yield { c.inc() }
        .flatMap { Promise.value(2).next { c.dec() } }
        .take(100)
        .toList()
    }.valueOrThrow.size() == 100

    c.max() == 1
  }

}

