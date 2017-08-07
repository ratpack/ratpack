package ratpack.rx2.observable

import io.reactivex.functions.Action
import ratpack.error.ServerErrorHandler
import ratpack.exec.Blocking
import ratpack.rx2.RxRatpack
import ratpack.test.internal.RatpackGroovyDslSpec
import ratpack.test.internal.SimpleErrorHandler

import static ratpack.rx2.RxRatpack.observe
import static ratpack.rx2.RxRatpack.observeEach

class RxBlockingSpec extends RatpackGroovyDslSpec {

  def setup() {
    RxRatpack.initialize()
  }

  def "can observe the blocking"() {
    when:
    handlers {
      get(":value") {
        observe(Blocking.get {
          pathTokens.value
        }) map {
          it * 2
        } map {
          it.toUpperCase()
        } subscribe {
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
        observe(Blocking.get {
          pathTokens.value
        }) map {
          it * 2
        } map {
          throw new Exception("!!!!")
        } subscribe{
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
        observe(Blocking.get {
          pathTokens.value
        }) map {
          it * 2
        } map {
          throw new Exception("!!!!")
        } subscribe({
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

        observeEach(Blocking.get {
          pathTokens.value.split(",") as List
        })
          .take(2)
          .map { it.toLowerCase() }
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

