package ratpack;

import org.ratpackframework.bootstrap.RatpackServer;
import org.ratpackframework.bootstrap.RatpackServerBuilder;
import org.ratpackframework.handling.Exchange;
import org.ratpackframework.handling.Handler;

import java.io.File;

public class Main {

  public static void main(String[] args) throws Exception {
    Handler handler = new Handler() {
      public void handle(Exchange exchange) {
        // Just return 200;
        exchange.getResponse().send();
      }
    };

    File dir = new File("src/ratpack");
    RatpackServerBuilder ratpackServerBuilder = new RatpackServerBuilder(handler, dir);
    ratpackServerBuilder.setWorkerThreads(0); // don't use a worker connection pool
    RatpackServer server = ratpackServerBuilder.build();

    server.startAndWait();
  }

}
