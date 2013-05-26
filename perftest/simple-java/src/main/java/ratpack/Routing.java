package ratpack;

import org.ratpackframework.handling.Chain;
import org.ratpackframework.handling.Exchange;
import org.ratpackframework.handling.Handler;
import org.ratpackframework.handling.Handlers;
import org.ratpackframework.util.Action;

public class Routing implements Action<Chain> {

  public void execute(Chain chain) {
    chain.add(Handlers.get("foo/bar", new Handler() {
      public void handle(Exchange exchange) {
        exchange.getResponse().send(exchange.getRequest().getUri());
      }
    }));
  }
}
