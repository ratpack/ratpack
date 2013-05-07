package org.ratpackframework.guice;

import com.google.inject.Injector;
import com.google.inject.Module;

public interface InjectionContext {

  Injector getInjector();

}
