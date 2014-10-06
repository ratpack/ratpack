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
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import ratpack.func.Action;
import ratpack.groovy.Groovy;
import ratpack.groovy.guice.GroovyBindingsSpec;
import ratpack.groovy.guice.internal.DefaultGroovyBindingsSpec;
import ratpack.groovy.handling.GroovyChain;
import ratpack.groovy.internal.ClosureUtil;
import ratpack.guice.BindingsSpec;
import ratpack.guice.Guice;
import ratpack.launch.LaunchConfig;
import ratpack.launch.LaunchConfigBuilder;
import ratpack.test.embed.BaseDirBuilder;
import ratpack.test.embed.EmbeddedApp;
import ratpack.test.embed.internal.LaunchConfigEmbeddedApp;

import java.nio.file.Path;
import java.util.function.Supplier;

import static ratpack.groovy.internal.ClosureUtil.configureDelegateFirst;

/**
 * A highly configurable {@link ratpack.test.embed.EmbeddedApp} implementation that allows the application to be defined in code at runtime.
 * <p>
 * This implementation is usually sufficient for testing Ratpack modules or extensions.
 *
 * <pre class="tested">
 * import ratpack.test.embed.BaseDirBuilder
 * import ratpack.session.SessionModule
 * import ratpack.groovy.test.embed.GroovyEmbeddedApp
 *
 * GroovyEmbeddedApp.build {
 *   baseDir {
 *     BaseDirBuilder.tmpDir().build {
 *       it.file "public/foo.txt", "bar"
 *     }
 *   }
 *
 *   launchConfig {
 *     development true
 *     other "some.other.property": "value"
 *   }
 *
 *   // Configure the module registry
 *   bindings {
 *     add new SessionModule()
 *   }
 *
 *   // Use the GroovyChain DSL for defining the application handlers
 *   handlers {
 *     get {
 *       render "root"
 *     }
 *     assets "public"
 *   }
 * }.test {
 *   assert it.getText() == "root"
 *   assert it.getText("foo.txt") == "bar"
 * }
 * </pre>
 *
 * @see ratpack.test.embed.BaseDirBuilder
 * @see ratpack.test.embed.EmbeddedApp
 */
public interface GroovyEmbeddedApp extends EmbeddedApp {

  public interface Spec {

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
    Spec handlers(@DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure);

    /**
     * Specifies the bindings of the application.
     * <p>
     * The given closure will not be executed until this application is started.
     * <p>
     * Subsequent calls to this method will <i>replace</i> the previous definition.
     * Calling this method after the application has started has no effect.
     *
     * @param closure The definition of the application handlers
     */
    Spec bindings(@DelegatesTo(value = GroovyBindingsSpec.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure);

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
    Spec launchConfig(@DelegatesTo(value = LaunchConfigBuilder.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure);

    Spec parentInjector(Injector parentInjector);

    Spec baseDir(Supplier<? extends Path> baseDirSupplier);

    default Spec baseDir(BaseDirBuilder baseDirBuilder) {
      return baseDir(baseDirBuilder::build);
    }

    default Spec baseDir(Path baseDir) {
      return baseDir(() -> baseDir);
    }
  }

  public static EmbeddedApp build(@DelegatesTo(value = Spec.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {


    return new LaunchConfigEmbeddedApp() {
      @Override
      protected LaunchConfig createLaunchConfig() {
        final SpecWrapper spec = new SpecWrapper();
        configureDelegateFirst(spec.getSpec(), closure);
        LaunchConfigBuilder launchConfigBuilder;

        if (spec.baseDirSupplier != null) {
          Path baseDirPath = spec.baseDirSupplier.get();
          launchConfigBuilder = LaunchConfigBuilder.baseDir(baseDirPath);
        } else {
          launchConfigBuilder = LaunchConfigBuilder.noBaseDir();
        }

        configureDelegateFirst(launchConfigBuilder.port(0), spec.launchConfig);

        final Action<? super BindingsSpec> bindingsAction = bindingsSpec -> configureDelegateFirst(new DefaultGroovyBindingsSpec(bindingsSpec), spec.bindings);

        return launchConfigBuilder.build(launchConfig -> {

          Guice.Builder builder = Guice.builder(launchConfig);
          if (spec.parentInjector != null) {
            builder.parent(spec.parentInjector);
          }

          return builder.bindings(bindingsAction).build(chain -> Groovy.chain(chain, spec.handlers));
        });
      }
    };
  }

  static class SpecWrapper {
    private Closure<?> handlers = ClosureUtil.noop();
    private Closure<?> bindings = ClosureUtil.noop();
    private Closure<?> launchConfig = ClosureUtil.noop();
    private Injector parentInjector = null;
    private Supplier<? extends Path> baseDirSupplier;

    Spec getSpec() {
      return new Spec() {
        @Override
        public Spec handlers(Closure<?> closure) {
          handlers = closure;
          return this;
        }

        @Override
        public Spec bindings(Closure<?> closure) {
          bindings = closure;
          return this;
        }

        @Override
        public Spec launchConfig(Closure<?> closure) {
          launchConfig = closure;
          return this;
        }

        @Override
        public Spec parentInjector(Injector injector) {
          parentInjector = injector;
          return this;
        }

        @Override
        public Spec baseDir(Supplier<? extends Path> supplier) {
          baseDirSupplier = supplier;
          return this;
        }
      };
    }

  }
}
