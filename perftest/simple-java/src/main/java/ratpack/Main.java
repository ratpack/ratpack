package ratpack;

import org.ratpackframework.bootstrap.RatpackServer;
import org.ratpackframework.bootstrap.RatpackServerBuilder;
import org.ratpackframework.handling.Exchange;
import org.ratpackframework.handling.Handler;
import org.ratpackframework.handling.Handlers;

import java.io.File;

public class Main {

  public static void main(String[] args) throws Exception {
    Handler handler = Handlers.chain(new Routing());

    File dir = new File("src/ratpack");
    RatpackServerBuilder ratpackServerBuilder = new RatpackServerBuilder(handler, dir);
    ratpackServerBuilder.setWorkerThreads(0); // don't use a worker connection pool
    RatpackServer server = ratpackServerBuilder.build();

    server.startAndWait();
  }

}
