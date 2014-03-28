import ratpack.codahale.metrics.CodaHaleMetricsModule
import ratpack.perf.incl.*

import static ratpack.groovy.Groovy.*

ratpack {
  modules {
    register new CodaHaleMetricsModule().metrics()
  }

  handlers {
    handler("stop", new StopHandler())

    handler("render") {
      render "ok"
    }
  }
}

