package perf;

import ratpack.launch.*;
import ratpack.handling.*;
import ratpack.func.*;
import ratpack.server.*;
import ratpack.perf.incl.*;

public class HandlerFactory implements ratpack.launch.HandlerFactory {

  public Handler create(LaunchConfig launchConfig) throws Exception {
    return Handlers.chain(launchConfig, chain -> {
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
      }
    );
  }

}
