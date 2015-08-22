<% if (patch <= 19) { %>
import ratpack.codahale.metrics.CodaHaleMetricsModule
<% } else { %>
import ratpack.dropwizard.metrics.DropwizardMetricsModule
<% } %>
import ratpack.perf.incl.*
import ratpack.handling.ResponseTimer

import static ratpack.groovy.Groovy.*

ratpack {
  bindings {
    <% if (patch <= 19) { %>
    add CodaHaleMetricsModule, { it.jmx() }
    <% } else { %>
    add DropwizardMetricsModule, { it.jmx() }
    <% } %>

    bindInstance ResponseTimer.decorator()
  }

  handlers {
    path("stop", new StopHandler())
    path("render") {
      render "ok"
    }
  }
}

