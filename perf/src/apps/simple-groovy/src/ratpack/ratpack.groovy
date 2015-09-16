import ratpack.perf.incl.*
import static ratpack.groovy.Groovy.*

<% if (patch >= 14) { %>
import ratpack.handling.ResponseTimer
<% } %>

ratpack {
  <% if (patch < 14) { %>
    serverConfig { it.timeResponses(true) }
  <% } %>
  bindings {
    <% if (patch >= 14) { %>
      bindInstance ResponseTimer.decorator()
    <% } %>
  }
  handlers {
    path("stop", new StopHandler())

    path("render") {
      render "ok"
    }

    path("direct")  {
      response.send("ok")
    }

    for (int i = 0; i < 100; ++ i) {
      path("all\$i") { throw new RuntimeException("unexpected") }
    }

    path("manyHandlers") { response.send() }
  }
}
