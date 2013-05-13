package org.ratpackframework.bootstrap.internal

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.ratpackframework.bootstrap.RatpackServerBuilder
import org.ratpackframework.routing.Handler
import spock.lang.Specification

import java.util.concurrent.ExecutionException

class NettyRatpackServerTest extends Specification {

  @Rule TemporaryFolder temporaryFolder

  def "throws exception if can't bind to port"() {
    given:
    def server1 = new RatpackServerBuilder({} as Handler, temporaryFolder.root).with {
      port = 0
      build()
    }

    server1.start().get()

    when:
    def server2 = new RatpackServerBuilder({} as Handler, temporaryFolder.root).with {
      port = server1.bindPort
      build()
    }

    server2.start().get()

    then:
    def e = thrown(ExecutionException)
    e.cause instanceof BindException

    cleanup:
    [server1, server2].each {
      if (it && it.running) {
        it.stopAndWait()
      }
    }
  }

}
