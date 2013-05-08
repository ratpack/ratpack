package org.ratpackframework.error;

import org.ratpackframework.routing.Exchange;

public interface ErrorHandlingContext {

  void error(Exchange exchange, Exception exception);

}
