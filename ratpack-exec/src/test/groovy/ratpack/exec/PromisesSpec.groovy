/*
 * Copyright 2019 the original author or authors.
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

package ratpack.exec

import spock.lang.Ignore
import spock.lang.Specification

import static ratpack.test.exec.ExecHarness.yieldSingle

class PromisesSpec extends Specification {

  def "test zipper success"() {
    when:
    def result = yieldSingle {
      Promises.zip(Promise.value("a"), Promise.value(1), { String a, Integer b ->
        a + b
      })
    }

    then:
    result.value == "a1"
  }

  def "test zipper error"() {
    when:
    def result = yieldSingle {
      Promises.zip(Promise.value("a"), Promise.error(new Exception("bah")), { String a, Integer b ->
        a + b
      })
    }

    then:
    result.throwable.message == "bah"
  }

  def "test zipper multi error"() {
    when:
    def result = yieldSingle {
      Promises.zip(Promise.error(new Exception("bah1")), Promise.error(new Exception("bah2")), { String a, Integer b ->
        a + b
      })
    }

    then:
    result.throwable.message.startsWith("bah") // undefined which error can come first
  }

  def "test zipper zip function error"() {
    when:
    def result = yieldSingle {
      Promises.zip(Promise.value("a"), Promise.value(1), { String s, Integer b ->
        throw new Exception("oops")
      })
    }

    then:
    result.throwable.message == "oops"
  }

  def "test zipper3 success"() {
    when:
    def result = yieldSingle {
      Promises.zip(Promise.value("a"), Promise.value(1), Promise.value(0.0), { String a, Integer b, Double c ->
        a + b + c
      })
    }

    then:
    result.value == "a10.0"
  }

  def "test zipper4 success"() {
    when:
    def result = yieldSingle {
      Promises.zip(Promise.value("a"), Promise.value(1), Promise.value(0.0), Promise.value("b"), { String a, Integer b, Double c, String d ->
        a + b + c + d
      })
    }

    then:
    result.value == "a10.0b"
  }

  def "test zipper5 success"() {
    when:
    def result = yieldSingle {
      Promises.zip(Promise.value("a"), Promise.value(1), Promise.value(0.0), Promise.value("b"), Promise.value(1), { String a, Integer b, Double c, String d, Integer e ->
        a + b + c + d + e
      })
    }

    then:
    result.value == "a10.0b1"
  }

  def "test zipper6 success"() {
    when:
    def result = yieldSingle {
      Promises.zip(Promise.value("a"), Promise.value(1), Promise.value(0.0), Promise.value("b"), Promise.value(1), Promise.value(7), { String a, Integer b, Double c, String d, Integer e, Integer f ->
        a + b + c + d + e + f
      })
    }

    then:
    result.value == "a10.0b17"
  }

  def "test zipper7 success"() {
    when:
    def result = yieldSingle {
      Promises.zip(Promise.value("a"), Promise.value(1), Promise.value(0.0), Promise.value("b"), Promise.value(1), Promise.value(7), Promise.value("g"), { String a, Integer b, Double c, String d, Integer e, Integer f, String g ->
        a + b + c + d + e + f + g
      })
    }

    then:
    result.value == "a10.0b17g"
  }

  def "test zipper8 success"() {
    when:
    def result = yieldSingle {
      Promises.zip(Promise.value("a"), Promise.value(1), Promise.value(0.0), Promise.value("b"), Promise.value(1), Promise.value(7), Promise.value("g"), Promise.value("0"), { String a, Integer b, Double c, String d, Integer e, Integer f, String g, String h ->
        a + b + c + d + e + f + g + h
      })
    }

    then:
    result.value == "a10.0b17g0"
  }

  @Ignore("Refactor this test")
  def "test registry is populated"() {
    given:
    String test = "TEST"

    when:
    def result = yieldSingle { exec ->
      exec.add(test)
      Promises.zip(Promise.value("a"), Promise.value(1), Promise.value(0.0), Promise.value("b"), Promise.async {
        it.success(Execution.current().get(String) + 1)
      }, { String a, Integer b, Double c, String d, String e ->
        a + b + c + d + e + Execution.current().get(String)
      })
    }

    then:
    result.value == "a10.0bTEST1TEST"
  }

  def "test zipper all success"() {
    when:
    def result = yieldSingle {
      Promises.zipAll(Promise.value("a"), Promise.value(1), { ExecResult<String> a, ExecResult<Integer> b ->
        a.value + b.value
      })
    }

    then:
    result.value == "a1"
  }

  def "test zipper all error"() {
    when:
    def result = yieldSingle {
      Promises.zipAll(Promise.error(new Exception("bah")), Promise.error(new Exception("bah")), { ExecResult<String> a, ExecResult<Integer> b ->
        a.throwable.message + b.throwable.message
      })
    }

    then:
    result.value == "bahbah"
  }

}
