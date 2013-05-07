package org.ratpackframework.http

import org.ratpackframework.groovy.RatpackGroovyDslSpec

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ConcurrentRequestSpec extends RatpackGroovyDslSpec {

  def "can serve requests concurrently without mixing up params"() {
    when:
    app {
      routing {
        get(":id") {
          response.send pathTokens.id + ":" + request.queryParams.id[0]
        }
      }
    }

    and:
    def threads = 100
    def latch = new CountDownLatch(threads)
    def results = []
    threads.times {
      results << null
    }

    threads.times { i ->
      Thread.start {
        try {
          def text = urlGetText("$i?id=$i")
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
