/*
 * Copyright 2013 the original author or authors.
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

package ratpack.logging

import org.slf4j.MDC
import org.slf4j.helpers.BasicMDCAdapter
import ratpack.exec.Blocking
import ratpack.exec.Execution
import ratpack.exec.Promise
import ratpack.test.internal.RatpackGroovyDslSpec

class MDCInterceptorSpec extends RatpackGroovyDslSpec {

  def origMdcAdapter

  def setup() {
    origMdcAdapter = MDC.MDCAdapter
    MDC.mdcAdapter = new BasicMDCAdapter()
    serverConfig {
      threads 16
    }
    bindings {
      bindInstance(MDCInterceptor.instance())
    }
  }

  def cleanup() {
    MDC.mdcAdapter = origMdcAdapter
  }

  def "requests do not share MDC context, run in separate executions"() {
    given:
    def values = []

    when:
    handlers {
      get("foo1") {
        MDC.put("value", "foo1")
        values << MDC.get("value")
        render "foo1"
      }
      get("foo2") {
        values << MDC.get("value")
        MDC.put("value", "foo2")
        values << MDC.get("value")
        render "foo2"
      }
    }

    then:
    getText("foo1") == "foo1"
    getText("foo2") == "foo2"
    values == ["foo1", null, "foo2"]
  }

  def "blocking operation shares MDC context, runs in the same execution"() {
    given:
    def values = []

    when:
    handlers {
      get {
        MDC.put("value", "1")
        values << MDC.get("value")
        Blocking.get {
          values << MDC.get("value")
          MDC.put("value", "2")
          values << MDC.get("value")
        }.then {
          values << MDC.get("value")
          render "foo1"
        }
        values << MDC.get("value")
      }
    }

    then:
    get()
    values == ["1", "1", "1", "2", "2"]
  }

  def "nested blocking operations share the same MDC context, run in the same execution"() {
    given:
    def values = []

    when:
    handlers {
      get {
        MDC.put("value", "1")
        values << MDC.get("value")
        Blocking.get {
          values << MDC.get("value")
          MDC.clear()
        }.then {
          values << MDC.get("value")
          MDC.put("value", "3")
          values << MDC.get("value")
          Blocking.get {
            values << MDC.get("value")
          }.then {
            values << MDC.get("value")
            render "foo1"
          }
        }
        values << MDC.get("value")
        MDC.put("value", "2")
      }
    }

    then:
    get()
    values == ["1", "1", "2", null, "3", "3", "3"]
  }

  def "forked execution does not share mdc"() {
    given:
    def values = []

    when:
    handlers {
      get {
        MDC.put("value", "1")
        values << MDC.get("value")
        Promise.async { f ->
          Execution.fork().start {
            MDC.put("value", "2")
            values << MDC.get("value")
            f.success("foo1")
          }
        }.then {
          values << MDC.get("value")
          render it
        }
      }
    }

    then:
    get()
    values == ["1", "2", "1"]
  }

}
