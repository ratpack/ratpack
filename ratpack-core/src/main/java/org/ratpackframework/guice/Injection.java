package org.ratpackframework.guice;

import org.ratpackframework.guice.internal.InjectingHandler;
import org.ratpackframework.http.Handler;

public abstract class Injection {

  public static Handler handler(Class<? extends Handler> handlerType) {
    return new InjectingHandler(handlerType);
  }

}
