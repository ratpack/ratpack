package org.ratpackframework.routing.internal;

import org.ratpackframework.handler.Handler;
import org.ratpackframework.handler.internal.CompositeHandler;
import org.ratpackframework.routing.RoutedRequest;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class CompositeRoutingHandler extends CompositeHandler<RoutedRequest> {

  @Inject
  public CompositeRoutingHandler(List<Handler<RoutedRequest>> handlers) {
    super(handlers, new ExhaustionHandler<RoutedRequest>() {
          @Override
          public void exhausted(RoutedRequest original, RoutedRequest last) {
            original.next(last);
          }
        }, new NextFactory<RoutedRequest>() {
          @Override
          public RoutedRequest makeNext(RoutedRequest original, Handler<? super RoutedRequest> next) {
            return new RoutedRequest(original.getExchange(), next);
          }
        }
    );
  }

}
