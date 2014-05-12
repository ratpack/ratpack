import ratpack.codahale.metrics.CodaHaleMetricsModule
import ratpack.perf.incl.*

import static ratpack.groovy.Groovy.*

ratpack {
  <% if (patch < 5) { %>
    modules {
      register new CodaHaleMetricsModule().metrics()
    }
  <% } else { %>
    bindings {
      add new CodaHaleMetricsModule().metrics()
    }
  <% } %>

  handlers {
    handler("stop", new StopHandler())

    handler("render") {
      render "ok"
    }
  }
}

