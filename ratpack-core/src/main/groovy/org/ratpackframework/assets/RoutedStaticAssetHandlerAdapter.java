package org.ratpackframework.assets;

import org.ratpackframework.Handler;
import org.ratpackframework.routing.RoutedRequest;

import javax.inject.Inject;

public class RoutedStaticAssetHandlerAdapter implements Handler<RoutedRequest> {

  private final Handler<RoutedStaticAssetRequest> delegate;

  @Inject
  public RoutedStaticAssetHandlerAdapter(Handler<RoutedStaticAssetRequest> delegate) {
    this.delegate = delegate;
  }

  @Override
  public void handle(final RoutedRequest original) {
    RoutedStaticAssetRequest routedStaticAssetRequest = new RoutedStaticAssetRequest(null, original.getExchange(), new Handler<RoutedRequest>() {
      @Override
      public void handle(RoutedRequest event) {
        original.next();
      }
    });

    delegate.handle(routedStaticAssetRequest);
  }
}
