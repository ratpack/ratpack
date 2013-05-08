package org.ratpackframework.error.internal;

import org.ratpackframework.error.ErrorHandlingContext;
import org.ratpackframework.routing.Exchange;

public class TopLevelErrorHandlingContext implements ErrorHandlingContext {

  @Override
  public void error(Exchange exchange, Exception exception) {
      System.err.println("UNHANDLED EXCEPTION: " + exchange.getRequest().getUri());
      exception.printStackTrace(System.err);
      exchange.getResponse().status(500).send();
    }

}
