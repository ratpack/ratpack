package ratpack.rx2.internal

import ratpack.error.ServerErrorHandler
import ratpack.error.internal.DefaultDevelopmentErrorHandler
import ratpack.groovy.handling.GroovyChain
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.test.embed.EmbeddedApp
import spock.lang.AutoCleanup

abstract class BaseHttpClientSpec extends RatpackGroovyDslSpec {

  @AutoCleanup
  EmbeddedApp otherApp

  EmbeddedApp otherApp(@DelegatesTo(value = GroovyChain, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
    otherApp = GroovyEmbeddedApp.of {
      registryOf { add ServerErrorHandler, new DefaultDevelopmentErrorHandler() }
      handlers(closure)
    }
  }

  URI otherAppUrl(String path = "") {
    new URI("$otherApp.address$path")
  }

}
