package org.ratpackframework.guice;

import org.ratpackframework.Action;
import org.ratpackframework.routing.Handler;

public interface GuiceBackedHandlerFactory {

  Handler create(Action<? super ModuleRegistry> modulesAction, Handler handler);

}
