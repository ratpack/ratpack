package org.ratpackframework.assets;

import org.ratpackframework.handler.Handler;
import org.ratpackframework.handler.internal.CompositeHandler;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class CompositeStaticAssetHandler extends CompositeHandler<RoutedStaticAssetRequest> {

  @Inject
  public CompositeStaticAssetHandler(List<Handler<RoutedStaticAssetRequest>> handlers) {
    super(
        handlers, new ExhaustionHandler<RoutedStaticAssetRequest>() {
          @Override
          public void exhausted(RoutedStaticAssetRequest original, RoutedStaticAssetRequest last) {
            original.next(last);
          }
        }, new NextFactory<RoutedStaticAssetRequest>() {
          @Override
          public RoutedStaticAssetRequest makeNext(RoutedStaticAssetRequest original, Handler<? super RoutedStaticAssetRequest> next) {
            return new RoutedStaticAssetRequest(original.getTargetFile(), original.getExchange(), next);
          }
        }
    );
  }
}
