package ratpack.logging

import org.slf4j.MDC
import ratpack.exec.ExecController
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.test.embed.EmbeddedApp
import ratpack.test.http.TestHttpClient
import spock.lang.Specification

import java.util.concurrent.CountDownLatch

class MDCInterceptorSpec extends Specification {

  def "one level execution"() {
    given:
    def values = []

    when:
    EmbeddedApp app = GroovyEmbeddedApp.build {
      handlers {
        handler {
          addInterceptor(new MDCInterceptor(getRequest())) {
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
    }

    then:
    app.test { TestHttpClient httpClient ->
      assert httpClient.getText("foo1") == "foo1"
      assert httpClient.getText("foo2") == "foo2"
      assert values == ["foo1", null, "foo2"]
    }
  }

  def "blocking execution with interceptor"() {
    given:
    def values = []

    when:
    EmbeddedApp app = GroovyEmbeddedApp.build {
      handlers {
        handler {
          addInterceptor(new MDCInterceptor(getRequest())) {
            next()
          }
        }
        get("foo1") {
          MDC.put("value", "foo1")
          values << MDC.get("value")
          blocking {
            values << MDC.get("value")
            MDC.put("value", "foo2")
            values << MDC.get("value")
          }.then {
            values << MDC.get("value")
            render "foo1"
          }
        }
      }
    }

    then:
    app.test { TestHttpClient httpClient ->
      assert httpClient.getText("foo1") == "foo1"
      assert values == ["foo1", "foo1", "foo2", "foo2"]
    }
  }

  def "more blocking executions with interceptor"() {
    given:
    def values = []

    when:
    EmbeddedApp app = GroovyEmbeddedApp.build {
      handlers {
        handler {
          addInterceptor(new MDCInterceptor(getRequest())) {
            next()
          }
        }
        get("foo1") {
          MDC.put("value", "foo1")
          values << MDC.get("value")
          blocking {
            values << MDC.get("value")
          }.then {
            values << MDC.get("value")

            blocking {
              values << MDC.get("value")
            }.then {
              values << MDC.get("value")
              render "foo1"
            }
          }
        }
        get("foo2") {
          values << MDC.get("value")
          render "foo2"
        }
      }
    }

    then:
    app.test { TestHttpClient httpClient ->
      assert httpClient.getText("foo1") == "foo1"
      assert httpClient.getText("foo2") == "foo2"
      assert values == ["foo1", "foo1", "foo1", "foo1", "foo1", null]
    }
  }

  def "compute executor with interceptor"() {
    given:
    def values = []
    def values2 = []

    when:
    EmbeddedApp app = GroovyEmbeddedApp.build {
      handlers {
        handler {
          addInterceptor(new MDCInterceptor(getRequest())) {
            next()
          }
        }
        get("foo1") { ExecController execController ->
          System.out.println(">> 1")
          MDC.put("value", "foo1")
          values << MDC.get("value")
          promise { f ->
            execController.control.exec().start {
              System.out.println(">> 3")
              values << MDC.get("value")
              f.success("foo1")
            }
          }.then {
            System.out.println(">> 4")
            values << MDC.get("value")
            render it
          }
          System.out.println(">> 2")
          values << MDC.get("value")
        }
        get("foo2") { ExecController execController ->
          MDC.put("value", "foo2")
          values2 << MDC.get("value")
          promise { f ->
            execController.control.exec().start {
              values2 << MDC.get("value")
              MDC.put("value", "foo3")
              values2 << MDC.get("value")
              f.success("foo2")
            }
          }.then {
            values2 << MDC.get("value")
            render it
          }
        }
      }
    }

    then:
    app.test { TestHttpClient httpClient ->
      assert httpClient.getText("foo1") == "foo1"
      assert httpClient.getText("foo2") == "foo2"
      assert values == ["foo1", "foo1", null, "foo1"]
      assert values2 == ["foo2", null, "foo3", "foo2"]
    }
  }

  def "parallel blocking executions with interceptor"() {
    given:
    def values = []

    when:
    EmbeddedApp app = GroovyEmbeddedApp.build {
      handlers {
        handler {
          addInterceptor(new MDCInterceptor(getRequest())) {
            next()
          }
        }
        get("foo1") {
          def latch = new CountDownLatch(1)
          MDC.put("value", "foo1")
          values << MDC.get("value")

          blocking {
            values << MDC.get("value")
          }.then {
            values << MDC.get("value")
            latch.countDown()
          }

          blocking {
            values << MDC.get("value")
          }.then {
            values << MDC.get("value")
            latch.await()
            MDC.put("value", "foo2")
            values << MDC.get("value")
            render "foo1"
          }
        }
      }
    }

    then:
    app.test { TestHttpClient httpClient ->
      assert httpClient.getText("foo1")
      assert values == ["foo1", "foo1", "foo1", "foo1", "foo1", "foo2"]
    }
  }

  def "parallel compute executors with interceptor"() {
    given:
    def values = []
    def values2 = []

    when:
    EmbeddedApp app = GroovyEmbeddedApp.build {
      handlers {
        handler {
          addInterceptor(new MDCInterceptor(getRequest())) {
            next()
          }
        }
        get("foo1") { ExecController execController ->
          def latch = new CountDownLatch(1)
          MDC.put("value", "foo1")
          values << MDC.get("value")

          promise { f ->
            execController.control.exec().start {
              values2 << MDC.get("value")
              f.success{"1"}
            }
          }.then {
            values << MDC.get("value")
            latch.countDown()
          }

          promise { f ->
            execController.control.exec().start {
              values2 << MDC.get("value")
              f.success("2")
            }
          }.then {
            values << MDC.get("value")
            latch.await()
            MDC.put("value", "foo2")
            values << MDC.get("value")
            render "foo1"
          }
        }
      }
    }

    then:
    app.test { TestHttpClient httpClient ->
      assert httpClient.getText("foo1")
      assert values == ["foo1", "foo1", "foo1", "foo2"]
      assert values2 == [null, null]
    }
  }

  def "async operation in seperate thread"() {
    given:
    def values = []

    when:
    EmbeddedApp app = GroovyEmbeddedApp.build {
      handlers {
        handler {
          addInterceptor(new MDCInterceptor(getRequest())) {
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
    }

    then:
    app.test { TestHttpClient httpClient ->
      assert httpClient.getText("foo") == "foo1"
      assert values == ["foo1", "foo1", null, "foo1", null, "foo1"]
    }
  }
}
