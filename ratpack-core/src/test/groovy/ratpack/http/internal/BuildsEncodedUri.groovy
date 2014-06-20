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

import com.google.common.collect.Multimap
import ratpack.func.Action
import ratpack.http.HttpUriBuilder
import ratpack.test.internal.RatpackGroovyDslSpec

class BuildsEncodedUri extends RatpackGroovyDslSpec {

  def "builds and encodes uri with all parameters correctly"() {
    String value
    when:
      HttpUriBuilder builder = new DefaultHttpUriBuilder()
      value = builder.secure().host("foo.com").port(1234).path("foo/bar").pathComponent("foo/bar").params(new Action<Multimap<String, String>>() {
        @Override
        void execute(Multimap<String, String> thing) throws Exception {
            thing.put("test","test")
            thing.put("test","Test2")
        }
      }).build().toString()
    then:
      value ==  "https://foo.com:1234/foo/bar/foo%2Fbar%3Ftest%3Dtest%26test%3DTest2"
  }

  def "builds and encodes uri when missing parmeters"(){
    String value
    when:
      value = new DefaultHttpUriBuilder().secure().host("foo.com").path("foo").pathComponent("foo/bar").build().toString()

    then:
      value ==  "https://foo.com/foo/foo%2Fbar"
  }

  def "builds and encodes uri with multiple calls to path method"(){
    String value

    when:
      value = new DefaultHttpUriBuilder().host("foo.com").path("foo").path("bar").pathComponent("foo").build().toString()

    then:
      value == "http://foo.com/foo/bar/foo"

  }

  def "builds and encodes uri with multiple calls to pathComponent method"(){
    String value

    when:
      value = new DefaultHttpUriBuilder().host("foo.com").path("foo").pathComponent("bar").pathComponent("foo").build().toString()

    then:
      value == "http://foo.com/foo/bar%2Ffoo"

  }

  def "overwrites host if host method called more than once"(){
    String value

    when:
      value = new DefaultHttpUriBuilder().host("foo.com").path("foo").pathComponent("bar").pathComponent("foo").host("bar.com").build().toString()

    then:
      value == "http://bar.com/foo/bar%2Ffoo"
  }

  def "builds and encodes uri with special characters in host"(){
    String value

    when:
      value = new DefaultHttpUriBuilder().host("christlichepÄrteiösterreichs.com").path("foo").build().toString()

    then:
      value == "http://christlichep%C3%84rtei%C3%B6sterreichs.com/foo"
  }

  def "builds and ecodes uri with special characters in path"(){
    String value

    when:
      value = new DefaultHttpUriBuilder().host("foo.com").path("pÄrteiösterreich").build().toString()

    then:
      value == "http://foo.com/p%C3%84rtei%C3%B6sterreich"
  }

  def "builds and ecodes uri with special characters in pathComponent"(){
    String value

    when:
      value = new DefaultHttpUriBuilder().host("foo.com").path("bar").pathComponent("pÄrteiösterreich").build().toString()

    then:
      value == "http://foo.com/bar/p%C3%84rtei%C3%B6sterreich"
  }

  def "builds and ecodes uri with special characters in params"(){
    String value

    when:
    value = new DefaultHttpUriBuilder().host("foo.com").path("bar").pathComponent("foo").params(new Action<Multimap<String, String>>() {
      @Override
      void execute(Multimap<String, String> thing) throws Exception {
        thing.put("test","pÄrteiösterreich")
      }
    }).build().toString()

    then:
      value == "http://foo.com/bar/foo%3Ftest%3Dp%C3%84rtei%C3%B6sterreich"
  }

}
