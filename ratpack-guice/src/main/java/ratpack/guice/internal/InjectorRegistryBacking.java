package ratpack.guice.internal;

import com.google.common.base.Supplier;
import com.google.common.collect.FluentIterable;
import com.google.common.reflect.TypeToken;
import com.google.inject.Injector;
import ratpack.registry.RegistryBacking;

public class InjectorRegistryBacking implements RegistryBacking {
  private final Injector injector;

  public InjectorRegistryBacking(Injector injector) {
    this.injector = injector;
  }

  @Override
  public <T> Iterable<Supplier<? extends T>> provide(TypeToken<T> type) {
    return FluentIterable.from(GuiceUtil.allProvidersOfType(injector, type)).transform(provider -> provider::get);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    InjectorRegistryBacking that = (InjectorRegistryBacking) o;

    return injector.equals(that.injector);
  }

  @Override
  public int hashCode() {
    return injector.hashCode();
  }
}
