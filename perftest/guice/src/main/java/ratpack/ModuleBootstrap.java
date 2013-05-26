package ratpack;

import com.google.inject.AbstractModule;
import org.ratpackframework.guice.ModuleRegistry;
import org.ratpackframework.util.Action;

public class ModuleBootstrap implements Action<ModuleRegistry> {

  public void execute(ModuleRegistry modules) {
    modules.register(new AbstractModule() {
      @Override
      protected void configure() {
        bind(Service.class).to(ServiceImpl.class);
      }
    });
  }
}
