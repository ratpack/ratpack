package ratpack.rx2.flowable

import io.reactivex.BackpressureStrategy
import io.reactivex.functions.Action
import io.reactivex.functions.Function
import ratpack.error.ServerErrorHandler
import ratpack.exec.Blocking
import ratpack.rx2.RxRatpack
import ratpack.rx2.internal.RatpackGroovyDslSpec
import ratpack.rx2.internal.SimpleErrorHandler

import static RxRatpack.flow
import static RxRatpack.flowEach

class RxBlockingSpec extends RatpackGroovyDslSpec {

  def setup() {
    RxRatpack.initialize()
  }

  def "can observe the blocking"() {
    when:
    handlers {
      get(":value") {
        flow(Blocking.get {
          pathTokens.value
        }, BackpressureStrategy.BUFFER) map({
          it * 2
        } as Function) map({
          it.toUpperCase()
        } as Function) subscribe {
          render it
        }
      }
    }

    then:
    getText("a") == "AA"
  }

  def "blocking errors are sent to the context renderer"() {
    when:
    bindings {
      bind ServerErrorHandler, SimpleErrorHandler
    }
    handlers {
      get(":value") {
        flow(Blocking.get {
          pathTokens.value
        }, BackpressureStrategy.BUFFER) map({
          it * 2
        } as Function) map({
          throw new Exception("!!!!")
        } as Function) subscribe{
          render "shouldn't happen"
        }
      }
    }

    then:
    getText("a").startsWith new Exception("!!!!").toString()
    response.statusCode == 500
  }

  def "blocking errors can be caught by onerror"() {
    when:
    bindings {
      bind ServerErrorHandler, SimpleErrorHandler
    }
    handlers {
      get(":value") {
        flow(Blocking.get {
          pathTokens.value
        },  BackpressureStrategy.BUFFER) map({
          it * 2
        } as Function) map({
          throw new Exception("!!!!")
        } as Function) subscribe({
          render "shouldn't happen"
        }, { render "b" })
      }
    }

    then:
    getText("a") == "b"
  }

  def "can observe the blocking operation with an Iterable return type"() {
    when:
    handlers {
      get(":value") {
        def returnString = ""

        flowEach(Blocking.get {
          pathTokens.value.split(",") as List
        },  BackpressureStrategy.BUFFER)
          .take(2)
          .map({ it.toLowerCase() } as Function)
          .subscribe({
          returnString += it
        }, { Throwable error ->
          throw error
        }, {
          render returnString
        } as Action)
      }
    }

    then:
    getText("A,B,C") == "ab"
  }
}

