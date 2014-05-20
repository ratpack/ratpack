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

package ratpack.http.internal

import ratpack.http.HttpUriBuilder
import ratpack.test.internal.RatpackGroovyDslSpec

/**
 * Created by roy on 18/05/14.
 */
class BuildsEncodedUri extends RatpackGroovyDslSpec {

  def "builds and encodes uri with all parameters correctly"() {
    String value
    when:
      HttpUriBuilder builder = new DefaultHttpUriBuilder()
      value = builder.secure().host("foo.com").port(1234).path("foo/bar").pathComponent("foo/bar").build().toString()

    then:
      value ==  "https://foo.com:1234/foo/bar/foo%2Fbar"
  }

  def "builds and encodes uri when missing parmeters"(){
    String value
    when:
      value = new DefaultHttpUriBuilder().secure().host("foo.com").path("foo").pathComponent("foo/bar").build().toString()

    then:
      value ==  "https://foo.com/foo/foo%2Fbar"
  }
}
