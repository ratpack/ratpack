package ratpack;

import org.ratpackframework.handling.Exchange;
import org.ratpackframework.handling.Handler;

import javax.inject.Inject;

public class InjectedHandler implements Handler {

  private final Service service;

  @Inject
  public InjectedHandler(Service service) {
    this.service = service;
  }

  public void handle(Exchange exchange) {
    exchange.getResponse().send(service.getValue());
  }

}
