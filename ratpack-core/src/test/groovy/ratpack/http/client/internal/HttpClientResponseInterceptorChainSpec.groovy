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
package ratpack.http.client.internal

import com.google.common.collect.Lists
import ratpack.http.client.HttpClientResponseInterceptor
import ratpack.http.client.ReceivedResponse
import spock.lang.Specification

class HttpClientResponseInterceptorChainSpec extends Specification {
  def 'Should invoke all interceptors'() {
    given:
    def interceptorA = Mock(HttpClientResponseInterceptor)
    def interceptorB = Mock(HttpClientResponseInterceptor)
    def chain = new HttpClientResponseInterceptorChain(Lists
      .newArrayList(interceptorA, interceptorB))
    when:
    chain.intercept(Stub(ReceivedResponse))
    then:
    1 * interceptorA.intercept(_ as ReceivedResponse)
    1 * interceptorB.intercept(_ as ReceivedResponse)
  }
}
