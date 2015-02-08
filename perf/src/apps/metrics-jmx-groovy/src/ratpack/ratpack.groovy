import ratpack.codahale.metrics.CodaHaleMetricsModule
import ratpack.perf.incl.*
<% if (patch >= 14) { %>
import ratpack.handling.ResponseTimer
<% } %>

import static ratpack.groovy.Groovy.*

ratpack {
  <% if (patch < 14) { %>
  serverConfig { it.timeResponses(true) }
  <% } %>

  bindings {
    <% if (patch < 14) { %>
    add new CodaHaleMetricsModule(), { it.enable(true).jmx { it.enable(true) } }
    <% } else { %>
    add CodaHaleMetricsModule, { it.jmx() }
    bindInstance ResponseTimer.decorator()
    <% } %>
  }

  handlers {
    handler("stop", new StopHandler())
    handler("render") {
      render "ok"
    }
  }
}

