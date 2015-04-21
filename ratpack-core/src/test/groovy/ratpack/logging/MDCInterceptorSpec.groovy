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
import ratpack.exec.ExecController
import ratpack.test.internal.RatpackGroovyDslSpec

import java.util.concurrent.CountDownLatch

class MDCInterceptorSpec extends RatpackGroovyDslSpec {

  def origMdcAdapter

  def setup() {
    origMdcAdapter = MDC.MDCAdapter
    MDC.mdcAdapter = new BasicMDCAdapter()
    serverConfig {
      threads 16
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
      handler {
        addInterceptor(new MDCInterceptor()) {
          next()
        }
      }
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
      handler {
        addInterceptor(new MDCInterceptor()) {
          next()
        }
      }
      get("foo1") {
        // 1
        println "1: ${Thread.currentThread().name}: ${ExecController.current().map{it.control.toString()}}"
        MDC.put("value", "foo1")
        values << MDC.get("value")
        blocking {
          // 3
          println "3: ${Thread.currentThread().name}: ${ExecController.current().map{it.control.toString()}}"
          values << MDC.get("value")
          MDC.put("value", "foo2")
          values << MDC.get("value")
        }.then {
          // 4
          println "4: ${Thread.currentThread().name}: ${ExecController.current().map{it.control.toString()}}"
          values << MDC.get("value")
          render "foo1"
        }
        // 2
        println "2: ${Thread.currentThread().name}: ${ExecController.current().map{it.control.toString()}}"
        values << MDC.get("value")
      }
    }

    then:
    getText("foo1") == "foo1"
    values == ["foo1", "foo1", "foo1", "foo2", "foo2"]
  }

  def "nested blocking operations share the same MDC context, run in the same execution"() {
    given:
    def values = []

    when:
    handlers {
      handler {
        addInterceptor(new MDCInterceptor()) {
          next()
        }
      }
      get("foo1") {
        // 1
        MDC.put("value", "foo1")
        values << MDC.get("value")
        blocking {
          // 3
          values << MDC.get("value")
          MDC.clear()
        }.then {
          //4
          values << MDC.get("value")
          MDC.put("value", "foo3")
          values << MDC.get("value")
          blocking {
            // 5
            values << MDC.get("value")
          }.then {
            // 6
            values << MDC.get("value")
            render "foo1"
          }
        }
        // 2
        values << MDC.get("value")
        MDC.put("value", "foo2")
      }
    }

    then:
    getText("foo1") == "foo1"
    values == ["foo1", "foo1", "foo2", null, "foo3", "foo3", "foo3"]
  }

  def "subsequent blocking operations share MDC context, run in the same execution"() {
    given:
    def values = []

    when:
    handlers {
      handler {
        addInterceptor(new MDCInterceptor()) {
          next()
        }
      }
      get("foo1") {
        // 1
        MDC.put("value", "foo1")
        values << MDC.get("value")

        blocking {
          // 3
          values << MDC.get("value")
          MDC.clear()
        }.then {
          // 4
          values << MDC.get("value")
          MDC.put("value", "foo3")
        }

        blocking {
          // 5
          values << MDC.get("value")
        }.then {
          // 6
          values << MDC.get("value")
          render "foo1"
        }

        // 2
        values << MDC.get("value")
        MDC.put("value", "foo2")
      }
    }

    then:
    getText("foo1")
    values == ["foo1", "foo1", "foo2", null, "foo3", "foo3"]
  }

  def "async operation controlled by promise does not share MDC context, runs in seperate execution"() {
    given:
    def values = []

    when:
    handlers {
      handler {
        addInterceptor(new MDCInterceptor()) {
          next()
        }
      }
      get("foo1") { ExecController execController ->
        // 1
        MDC.put("value", "foo1")
        values << MDC.get("value")
        promise { f ->
          // 3
          execController.control.exec().start {
            // 4
            values << MDC.get("value")
            MDC.put("value", "foo3")
            f.success("foo1")
          }
        }.then {
          // 5
          values << MDC.get("value")
          render it
        }
        // 2
        values << MDC.get("value")
        MDC.put("value", "foo2")
      }
    }

    then:
    getText("foo1") == "foo1"
    values == ["foo1", "foo1", null, "foo2"]
  }

  def "subsequent async operations controlled by promised do not share MDC context, run in different executions"() {
    given:
    def values = []

    when:
    handlers {
      handler {
        addInterceptor(new MDCInterceptor()) {
          next()
        }
      }
      get("foo1") { ExecController execController ->
        // 1
        MDC.put("value", "foo1")
        values << MDC.get("value")

        promise { f ->
          execController.control.exec().onComplete {
            // 4
            values << MDC.get("value")
            f.success{"1"}
          }.start {
            // 3
            values << MDC.get("value")
            MDC.contextMap = ["value": "foo3"]
            values << MDC.get("value")
          }
        }.then {
          // 5
          values << MDC.get("value")
        }

        promise { f ->
          execController.control.exec().start {
            //6
            values << MDC.get("value")
            f.success("2")
          }
        }.then {
          // 7
          values << MDC.get("value")
          render "foo1"
        }
        // 2
        values << MDC.get("value")
        MDC.put("value", "foo2")
      }
    }

    then:
    getText("foo1")
    values == ["foo1", "foo1", null, "foo3", "foo3",  "foo2", null, "foo2"]
  }

  def "async operations in seperate threads controlled by promises do not share MDC context"() {
    given:
    def values = []

    when:
    handlers {
      handler {
        addInterceptor(new MDCInterceptor()) {
          next()
        }
      }
      get("foo") {
        // 1
        MDC.put("value", "foo1")
        values << MDC.get("value")
        promise { f ->
          Thread.start {
            // 3
            values << MDC.get("value")
            MDC.put("value", "foo2")
            f.success("1")
          }
        }.then {
          // 4
          values << MDC.get("value")
        }

        promise { f->
          Thread.start {
            // 5
            values << MDC.get("value")

            MDC.remove("value")
            f.success("2")
          }
        }.then {
          // 6
          values << MDC.get("value")
          render "foo1"
        }
        // 2
        values << MDC.get("value")
      }
    }

    then:
    getText("foo") == "foo1"
    values == ["foo1", "foo1", null, "foo1", null, "foo1"]
  }

  def "fork without promise is not intercepted and has the same execution and the same MDC context"() {
    given:
    def values = []
    def values2 = []

    when:
    handlers {
      handler {
        addInterceptor(new MDCInterceptor()) {
          next()
        }
      }
      get("foo1") { ExecController execController ->
        def latch = new CountDownLatch(1)
        // 1
        //println "1: ${Thread.currentThread().name}: ${ExecController.current().map{it.control.toString()}}"

        MDC.put("value", "foo1")
        values << MDC.get("value")
        execController.control.exec().onComplete {
          // 3
          //println "3: ${Thread.currentThread().name}: ${ExecController.current().map{it.control.toString()}}"
          values2 << MDC.get("value")
          latch.countDown()
        }.start {
          // 2
          //println "2: ${Thread.currentThread().name}: ${ExecController.current().map{it.control.toString()}}"
          values2 << MDC.get("value")
          MDC.put("value", "foo2")
        }

        latch.await()
        // 4
        //println "4: ${Thread.currentThread().name}: ${ExecController.current().map{it.control.toString()}}"
        values << MDC.get("value")
        render "foo1"
      }

      get("foo2") { ExecController execController ->
        def latch = new CountDownLatch(1)
        def latch2 = new CountDownLatch(1)
        // 1
        //println "1: ${Thread.currentThread().name}: ${ExecController.current().map{it.control.toString()}}"

        MDC.put("value", "foo1")
        values << MDC.get("value")
        execController.control.exec().onComplete {
          // 4
          //println "4: ${Thread.currentThread().name}: ${ExecController.current().map{it.control.toString()}}"
          values2 << MDC.get("value")

          latch2.countDown()

        }.start {

          latch.await()

          // 3
          //println "3: ${Thread.currentThread().name}: ${ExecController.current().map{it.control.toString()}}"
          values2 << MDC.get("value")
          MDC.put("value", "foo2")
        }

        // 2
        //println "2: ${Thread.currentThread().name}: ${ExecController.current().map{it.control.toString()}}"
        values << MDC.get("value")

        latch.countDown()

        latch2.await()

        render "foo2"
      }
    }

    then:
    getText("foo1") == "foo1"
    values == ["foo1", "foo2"]
    values2 == ["foo1", "foo2"]

    values.clear()
    values2.clear()

    getText("foo2") == "foo2"
    values == ["foo1", "foo1"]
    values2 == ["foo1", "foo2"]
  }
}
