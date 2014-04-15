/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.groovy.test.embed;

import com.google.inject.Injector;
import com.google.inject.Module;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import ratpack.func.Action;
import ratpack.func.Factory;
import ratpack.func.Transformer;
import ratpack.groovy.Groovy;
import ratpack.groovy.guice.GroovyModuleRegistry;
import ratpack.groovy.guice.internal.DefaultGroovyModuleRegistry;
import ratpack.groovy.handling.GroovyChain;
import ratpack.groovy.internal.ClosureUtil;
import ratpack.guice.Guice;
import ratpack.guice.GuiceBackedHandlerFactory;
import ratpack.guice.ModuleRegistry;
import ratpack.guice.internal.DefaultGuiceBackedHandlerFactory;
import ratpack.handling.Handler;
import ratpack.launch.HandlerFactory;
import ratpack.launch.LaunchConfig;
import ratpack.launch.LaunchConfigBuilder;
import ratpack.test.embed.BaseDirBuilder;
import ratpack.test.embed.LaunchConfigEmbeddedApplication;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import static ratpack.groovy.internal.ClosureUtil.configureDelegateFirst;

/**
 * A highly configurable {@link ratpack.test.embed.EmbeddedApplication} implementation that allows the application to be defined in code at runtime.
 * <p>
 * This implementation is usually sufficient for testing Ratpack modules or extensions.
 * <p>
 * <pre class="tested">
 * import ratpack.test.embed.PathBaseDirBuilder
 * import ratpack.groovy.test.TestHttpClients
 * import ratpack.session.SessionModule
 *
 * import java.nio.file.Files
 *
 * import static ratpack.groovy.test.embed.EmbeddedApplications.embeddedApp
 *
 * def tmp = Files.createTempDirectory("ratpack-test")
 * def baseDir = new PathBaseDirBuilder(tmp)
 *
 * // Create a file within the application base dir
 * baseDir.file "public/foo.txt", "bar"
 *
 * def app = embeddedApp(baseDir) {
 *   // Configure the launch config
 *   launchConfig {
 *     other "some.other.property": "value"
 *     reloadable true
 *   }
 *
 *   // Configure the module registry
 *   modules {
 *     register new SessionModule()
 *   }
 *
 *   // Use the GroovyChain DSL for defining the application handlers
 *   handlers {
 *     get {
 *       render "root"
 *     }
 *     assets "public"
 *   }
 * }
 *
 * // Test the application with the test http client
 * def client = TestHttpClients.testHttpClient(app)
 *
 * assert client.getText() == "root"
 * assert client.getText("foo.txt") == "bar"
 *
 * // Cleanup
 * app.close()
 * </pre>
 *
 * @see ratpack.test.embed.PathBaseDirBuilder
 * @see ratpack.groovy.test.embed.EmbeddedApplications
 */
public class ClosureBackedEmbeddedApplication extends LaunchConfigEmbeddedApplication {

  private Closure<?> handlersClosure = ClosureUtil.noop();
  private Closure<?> modulesClosure = ClosureUtil.noop();
  private Closure<?> launchConfigClosure = ClosureUtil.noop();

  private final List<Module> modules = new LinkedList<>();

  private Factory<Path> baseDirFactory;

  /**
   * Constructor.
   */
  public ClosureBackedEmbeddedApplication() { }

  /**
   * Constructor.
   * <p>
   * Useful for deferring the base dir determination
   *
   * @param baseDirFactory The factory for the base dir
   */
  public ClosureBackedEmbeddedApplication(Factory<Path> baseDirFactory) {
    this.baseDirFactory = baseDirFactory;
  }

  /**
   * Constructor.
   *
   * @param baseDirBuilder the builder whose {@link BaseDirBuilder#build()} method will be called to provide the base dir for this app
   */
  public ClosureBackedEmbeddedApplication(final BaseDirBuilder baseDirBuilder) {
    this(new Factory<Path>() {
      @Override
      public Path create() {
        return baseDirBuilder.build();
      }
    });
  }

  /**
   * A mutable list of modules to use in this application.
   *
   * @return A mutable list of modules to use in this application
   */
  public List<Module> getModules() {
    return modules;
  }

  /**
   * Constructs the launch config using the other {@code create*} methods.
   * <p>
   * A launch config will be created using {@link LaunchConfigBuilder#baseDir(java.io.File)}, with the path returned by the factory given at construction.
   * The launch config will also default to using a port value of {@code 0} (i.e. an ephemeral port).
   * <p>
   * Register modules and objects using the {@link #modules(groovy.lang.Closure)} method.
   * <p>
   * Define the handlers using the {@link #handlers(groovy.lang.Closure)} method.
   * <p>
   * Prefer overriding specific {@code create*} methods instead of this one.
   *
   * @return The created launch config.
   */
  @Override
  protected LaunchConfig createLaunchConfig() {
    LaunchConfigBuilder launchConfigBuilder;
    if (this.baseDirFactory != null) {
      Path baseDirPath = baseDirFactory.create();

      launchConfigBuilder = LaunchConfigBuilder
        .baseDir(baseDirPath)
        .port(0);
    } else {
      launchConfigBuilder = LaunchConfigBuilder
        .noBaseDir()
        .port(0);
    }

    configureDelegateFirst(launchConfigBuilder, launchConfigClosure);

    final Action<? super ModuleRegistry> modulesAction = createModulesAction();

    return launchConfigBuilder.build(new HandlerFactory() {
      public Handler create(LaunchConfig launchConfig) throws Exception {
        GuiceBackedHandlerFactory handlerFactory = createHandlerFactory(launchConfig);
        Transformer<? super Module, ? extends Injector> injectorFactory = createInjectorFactory(launchConfig);
        Transformer<? super Injector, ? extends Handler> handlerTransformer = createHandlerTransformer(launchConfig);

        return handlerFactory.create(modulesAction, injectorFactory, handlerTransformer);
      }
    });
  }

  /**
   * Specifies the handlers of the application.
   * <p>
   * The given closure will not be executed until this application is started.
   * <p>
   * Subsequent calls to this method will <i>replace</i> the previous definition.
   * Calling this method after the application has started has no effect.
   *
   * @param closure The definition of the application handlers
   */
  public void handlers(@DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
    this.handlersClosure = closure;
  }

  /**
   * Specifies the modules of the application.
   * <p>
   * The given closure will not be executed until this application is started.
   * <p>
   * Subsequent calls to this method will <i>replace</i> the previous definition.
   * Calling this method after the application has started has no effect.
   *
   * @param closure The definition of the application handlers
   */
  public void modules(@DelegatesTo(value = GroovyModuleRegistry.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
    this.modulesClosure = closure;
  }

  /**
   * Modifies the launch config of the application.
   * <p>
   * The given closure will not be executed until this application is started.
   * <p>
   * Subsequent calls to this method will <i>replace</i> the previous definition.
   * Calling this method after the application has started has no effect.
   *
   * @param closure The definition of the application handlers
   */
  public void launchConfig(@DelegatesTo(value = LaunchConfigBuilder.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
    this.launchConfigClosure = closure;
  }

  /**
   * Creates a module to injector transformer based on the given launch config.
   * <p>
   * Delegates to {@link Guice#newInjectorFactory(ratpack.launch.LaunchConfig)}.
   *
   * @param launchConfig The launch config
   * @return the result of {@link Guice#newInjectorFactory(ratpack.launch.LaunchConfig)} with the given launch config
   */
  protected Transformer<? super Module, ? extends Injector> createInjectorFactory(LaunchConfig launchConfig) {
    return Guice.newInjectorFactory(launchConfig);
  }

  /**
   * Returns the factory to use to create the actual handler.
   * <p>
   * This implementation does not add any implicit behaviour.
   * Subclasses may wish to provide different implementations that do perform implicit behaviour (e.g. implied modules)
   *
   * @param launchConfig The launch config
   * @return The guice handler factory
   */
  protected GuiceBackedHandlerFactory createHandlerFactory(LaunchConfig launchConfig) {
    return new DefaultGuiceBackedHandlerFactory(launchConfig);
  }

  /**
   * Provides the module registry configuration action.
   * <p>
   * This implementation creates an action that registers {@link #getModules()} in order, then executes the closure given with {@link #modules(groovy.lang.Closure)}.
   *
   * @return The module registry configuration action
   */
  protected Action<? super ModuleRegistry> createModulesAction() {
    return new Action<ModuleRegistry>() {
      @Override
      public void execute(ModuleRegistry moduleRegistry) throws Exception {
        for (Module module : modules) {
          moduleRegistry.register(module);
        }
        configureDelegateFirst(new DefaultGroovyModuleRegistry(moduleRegistry), modulesClosure);
      }
    };
  }

  /**
   * Provides the object that, given the {@link com.google.inject.Injector} created by the module definition, creates the application handler.
   * <p>
   * This implementation uses the closure given to {@link #handlers(groovy.lang.Closure)} to build a handler using the {@link Groovy#chain(ratpack.launch.LaunchConfig, ratpack.registry.Registry, groovy.lang.Closure)} method.
   * An registry based on the injector backs the chain.
   *
   * @param launchConfig the launch config
   * @return A transformer that creates the application handler, given an injector
   */
  protected Transformer<? super Injector, ? extends Handler> createHandlerTransformer(final LaunchConfig launchConfig) {
    return new Transformer<Injector, Handler>() {
      @Override
      public Handler transform(Injector injector) throws Exception {
        return Groovy.chain(launchConfig, Guice.registry(injector), handlersClosure);
      }
    };
  }
}
