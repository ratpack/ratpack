package perf;

import ratpack.handling.*;
import ratpack.server.*;
import ratpack.perf.incl.*;

public class Main {

  public static void main(String... args) throws Exception {
    RatpackServer.start(s -> s
      <% if (patch > 13) { %>
      .registryOf(r -> r.add(ResponseTimer.decorator()))
      <% } else { %>
      .serverConfig(ServerConfig.noBaseDir().timeResponses(true))
      <% } %>
      .handlers(chain -> {
        chain
          .handler("stop", new StopHandler())
          .handler("render", ctx -> ctx.render("ok"))
          .handler("direct", ctx -> ctx.getResponse().send("ok"));

        for (int i = 0; i < 100; ++i) {
          chain.handler("handler" + i, ctx -> {
            throw new RuntimeException("unexpected");
          });
        }

        chain.handler("manyHandlers", ctx -> ctx.getResponse().send());
      })
    );
  }

}
