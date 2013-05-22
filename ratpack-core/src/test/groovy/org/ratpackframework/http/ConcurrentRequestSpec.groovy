package org.ratpackframework.http

import org.ratpackframework.test.groovy.RatpackGroovyDslSpec

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ConcurrentRequestSpec extends RatpackGroovyDslSpec {

  def "can serve requests concurrently without mixing up params"() {
    when:
    app {
      handlers {
        get(":id") {
          response.send pathTokens.id + ":" + request.queryParams.id[0]
        }
      }
    }

    and:
    def threads = 500
    def latch = new CountDownLatch(threads)
    def results = []
    threads.times {
      results << null
    }

    startServerIfNeeded()

    threads.times { i ->
      Thread.start {
        try {
          def text = createRequest().get(toUrl("$i?id=$i")).asString()
          assert text ==~ "\\d+:\\d+"
          def (id, value) = text.split(':').collect { it.toInteger() }
          results[id] = value
        } finally {
          latch.countDown()
        }
      }
    }

    latch.await(30, TimeUnit.SECONDS)

    then:
    (0..<threads).each {
      assert results[it] == it
    }
  }


}
