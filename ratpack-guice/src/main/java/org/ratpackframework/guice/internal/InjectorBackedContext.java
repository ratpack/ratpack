package org.ratpackframework.guice.internal;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import org.ratpackframework.context.Context;
import org.ratpackframework.context.ContextSupport;

public class InjectorBackedContext extends ContextSupport {

  private final Injector injector;

  public InjectorBackedContext(Context parent, Injector injector) {
    super(parent);
    this.injector = injector;
  }

  @Override
  public <T> T doGet(Class<T> type) {
    Binding<T> existingBinding = injector.getExistingBinding(Key.get(type));
    if (existingBinding == null) {
      return null;
    } else {
      return existingBinding.getProvider().get();
    }
  }

}
