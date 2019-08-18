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

package ratpack.groovy;

import com.google.common.collect.ImmutableMap;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.lang.GroovySystem;
import groovy.xml.MarkupBuilder;
import io.netty.util.CharsetUtil;
import ratpack.api.Nullable;
import ratpack.file.FileSystemBinding;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.groovy.handling.*;
import ratpack.groovy.handling.internal.ClosureBackedHandler;
import ratpack.groovy.handling.internal.DefaultGroovyByMethodSpec;
import ratpack.groovy.handling.internal.GroovyDslChainActionTransformer;
import ratpack.groovy.internal.ClosureInvoker;
import ratpack.groovy.internal.ClosureUtil;
import ratpack.groovy.internal.GroovyVersionCheck;
import ratpack.groovy.internal.ScriptBackedHandler;
import ratpack.groovy.internal.capture.*;
import ratpack.groovy.script.ScriptNotFoundException;
import ratpack.groovy.template.Markup;
import ratpack.groovy.template.MarkupTemplate;
import ratpack.groovy.template.TextTemplate;
import ratpack.guice.BindingsSpec;
import ratpack.guice.Guice;
import ratpack.handling.Chain;
import ratpack.handling.Handler;
import ratpack.handling.Handlers;
import ratpack.handling.internal.ChainBuilders;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.registry.Registry;
import ratpack.server.*;
import ratpack.server.internal.BaseDirFinder;
import ratpack.server.internal.FileBackedReloadInformant;
import ratpack.util.internal.Paths2;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static ratpack.util.Exceptions.uncheck;

/**
 * Static methods for specialized Groovy integration.
 */
public abstract class Groovy {

  private Groovy() {

  }

  /**
   * Starts a Ratpack app, defined by the given closure.
   * <p>
   * This method is used in Ratpack scripts as the entry point.
   * <pre class="groovy-ratpack-dsl">
   * import ratpack.groovy.sql.SqlModule
   * import static ratpack.groovy.Groovy.*
   *
   * ratpack {
   *   bindings {
   *     // example of registering a module
   *     add(new SqlModule())
   *   }
   *   handlers {
   *     // define the application handlers
   *     get("foo") {
   *       render "bar"
   *     }
   *   }
   * }
   * </pre>
   * <h3>Standalone Scripts</h3>
   * <p>
   * This method can be used by standalone scripts to start a Ratpack application.
   * That is, you could save the above content in a file named “{@code ratpack.groovy}” (the name is irrelevant),
   * then start the application by running `{@code groovy ratpack.groovy}` on the command line.
   * <h3>Full Applications</h3>
   * <p>
   * It's also possible to build Groovy Ratpack applications with a traditional class based entry point.
   * The {@link ratpack.groovy.GroovyRatpackMain} class provides such an entry point.
   * In such a mode, a script like above is still used to define the application, but the script is no longer the entry point.
   * Ratpack will manage the compilation and execution of the script internally.
   *
   * @param closure The definition closure, delegating to {@link ratpack.groovy.Groovy.Ratpack}
   */
  public static void ratpack(@DelegatesTo(value = Ratpack.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
    try {
      RatpackScriptBacking.execute(closure);
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

  /**
   * The definition of a Groovy Ratpack application.
   *
   * @see ratpack.groovy.Groovy#ratpack(groovy.lang.Closure)
   */
  public interface Ratpack {

    /**
     * Registers the closure used to configure the {@link ratpack.guice.BindingsSpec} that will back the application.
     *
     * @param configurer The configuration closure, delegating to {@link ratpack.guice.BindingsSpec}
     */
    void bindings(@DelegatesTo(value = BindingsSpec.class, strategy = Closure.DELEGATE_FIRST) Closure<?> configurer);

    /**
     * Registers the closure used to build the handler chain of the application.
     *
     * @param configurer The configuration closure, delegating to {@link GroovyChain}
     */
    void handlers(@DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> configurer);

    /**
     * Registers the closure used to build the configuration of the server.
     *
     * @param configurer The configuration closure, delegating to {@link ServerConfigBuilder}
     */
    void serverConfig(@DelegatesTo(value = ServerConfigBuilder.class, strategy = Closure.DELEGATE_FIRST) Closure<?> configurer);

    /**
     * Evaluates the provided path using the Ratpack DSL and applies the configuration to this server.
     * <p>
     * The configuration supplied by the included path are applied inline with the existing parent configuration.
     * This allows the same semantics as specifying the configuration in a single file to be followed.
     * For {@link Ratpack#bindings(Closure)}, the configuration is appended.
     * For {@link Ratpack#handlers} and {@link Ratpack#serverConfig(Closure)}, the configuration is merged.
     * Settings from the parent configuration that are applied after the {@code include}, will be applied after the child configurations.
     * <p>
     * If the {@code path} is a relative path, then it will be resolved against the location of the parent script file that is including it.
     *
     * @param path The absolute path to the external Groovy DSL to included into the server.
     * @since 1.3
     */
    void include(Path path);

    /**
     * Evaluates the path provided using the Ratpack DSL and applies the configuration to this server.
     * <p>
     * The provided string is evaluated using {@link java.nio.file.Paths#get(String, String...)}
     *
     * @param path The absolute path to the external Groovy DSL to included into the server.
     * @see #include(Path)
     * @since 1.3
     */
    void include(String path);

  }

  /**
   * Methods for working with Groovy scripts as application components.
   */
  public static abstract class Script {

    /**
     * {@value}
     */
    public static final String DEFAULT_HANDLERS_PATH = "handlers.groovy";

    /**
     * {@value}
     */
    public static final String DEFAULT_BINDINGS_PATH = "bindings.groovy";

    /**
     * {@value}
     */
    public static final String DEFAULT_APP_PATH = "ratpack.groovy";

    private Script() {
    }

    /**
     * Asserts that the version of Groovy on the classpath meets the minimum requirement for Ratpack.
     */
    public static void checkGroovy() {
      GroovyVersionCheck.ensureRequiredVersionUsed(GroovySystem.getVersion());
    }

    /**
     * Creates an application defining action from a Groovy script named {@value #DEFAULT_APP_PATH}.
     * <p>
     * This method returns an action that can be used with {@link RatpackServer#of(Action)} to create an application.
     * <p>
     * The script should call the {@link Script#ratpack(Closure)} method.
     *
     * @return an application definition action
     */
    public static Action<? super RatpackServerSpec> app() {
      return app(false);
    }

    /**
     * Creates an application defining action from a Groovy script named {@value #DEFAULT_APP_PATH}.
     * <p>
     * This method returns an action that can be used with {@link RatpackServer#of(Action)} to create an application.
     * <p>
     * The script should call the {@link Script#ratpack(Closure)} method.
     *
     * @param compileStatic whether to statically compile the script
     * @return an application definition action
     */
    public static Action<? super RatpackServerSpec> app(boolean compileStatic) {
      return app(compileStatic, DEFAULT_APP_PATH, DEFAULT_APP_PATH.substring(0, 1).toUpperCase() + DEFAULT_APP_PATH.substring(1));
    }

    /**
     * Creates an application defining action from a Groovy script.
     * <p>
     * This method returns an action that can be used with {@link RatpackServer#of(Action)} to create an application.
     * <p>
     * The script should call the {@link Script#ratpack(Closure)} method.
     *
     * @param script the script
     * @return an application definition action
     */
    public static Action<? super RatpackServerSpec> app(Path script) {
      return app(false, script);
    }

    /**
     * Creates an application defining action from a Groovy script.
     * <p>
     * This method returns an action that can be used with {@link RatpackServer#of(Action)} to create an application.
     * <p>
     * The script should call the {@link Script#ratpack(Closure)} method.
     *
     * @param compileStatic whether to statically compile the script
     * @param script the script
     * @return an application definition action
     */
    public static Action<? super RatpackServerSpec> app(boolean compileStatic, Path script) {
      return b -> doApp(b, compileStatic, script.getParent(), script);
    }

    /**
     * Creates an application defining action from a Groovy script.
     * <p>
     * This method returns an action that can be used with {@link RatpackServer#of(Action)} to create an application.
     * <p>
     * The script should call the {@link Script#ratpack(Closure)} method.
     *
     * @param compileStatic whether to statically compile the script
     * @param scriptPaths the potential paths to the scripts (first existing is used)
     * @return an application definition action
     */
    public static Action<? super RatpackServerSpec> app(boolean compileStatic, String... scriptPaths) {
      return appWithArgs(compileStatic, scriptPaths);
    }

    /**
     * Creates an application defining action from a Groovy script named {@value #DEFAULT_APP_PATH}
     * <p>
     * This method returns an action that can be used with {@link RatpackServer#of(Action)} to create an application.
     * <p>
     * The script should call the {@link Script#ratpack(Closure)} method.
     *
     * @param args args to make available to the script via the {@code args} variable
     * @return an application definition action
     * @since 1.1
     */
    public static Action<? super RatpackServerSpec> appWithArgs(String... args) {
      return appWithArgs(false, new String[]{DEFAULT_APP_PATH, DEFAULT_APP_PATH.substring(0, 1).toUpperCase() + DEFAULT_APP_PATH.substring(1)}, args);
    }

    /**
     * Creates an application defining action from a Groovy script.
     * <p>
     * This method returns an action that can be used with {@link RatpackServer#of(Action)} to create an application.
     * <p>
     * The script should call the {@link Script#ratpack(Closure)} method.
     *
     * @param compileStatic whether to statically compile the script
     * @param script the script
     * @param args args to make available to the script via the {@code args} variable
     * @return an application definition action
     * @since 1.1
     */
    public static Action<? super RatpackServerSpec> appWithArgs(boolean compileStatic, Path script, String... args) {
      return b -> doApp(b, compileStatic, script.getParent(), script, args);
    }

    /**
     * Creates an application defining action from a Groovy script.
     * <p>
     * This method returns an action that can be used with {@link RatpackServer#of(Action)} to create an application.
     * <p>
     * The script should call the {@link Script#ratpack(Closure)} method.
     *
     * @param compileStatic whether to statically compile the script
     * @param scriptPaths the potential paths to the scripts (first existing is used)
     * @param args args to make available to the script via the {@code args} variable
     * @return an application definition action
     * @since 1.1
     */
    public static Action<? super RatpackServerSpec> appWithArgs(boolean compileStatic, String[] scriptPaths, String... args) {
      return b -> {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        BaseDirFinder.Result baseDirResult = Arrays.stream(scriptPaths)
          .map(scriptPath -> BaseDirFinder.find(classLoader, scriptPath))
          .filter(Optional::isPresent)
          .map(Optional::get)
          .findFirst()
          .orElseThrow(() -> new ScriptNotFoundException(scriptPaths));

        Path baseDir = baseDirResult.getBaseDir();
        Path scriptFile = baseDirResult.getResource();
        doApp(b, compileStatic, baseDir, scriptFile, args);
      };
    }

    private static void doApp(RatpackServerSpec definition, boolean compileStatic, Path baseDir, Path scriptFile, String... args) throws Exception {
      String script = Paths2.readText(scriptFile, StandardCharsets.UTF_8);

      RatpackDslClosures closures = new RatpackDslScriptCapture(compileStatic, args, RatpackDslBacking::new).apply(scriptFile, script);
      definition.serverConfig(ClosureUtil.configureDelegateFirstAndReturn(loadPropsIfPresent(ServerConfig.builder().baseDir(baseDir), baseDir), closures.getServerConfig()));

      definition.registry(r -> {
        return Guice.registry(bindingsSpec -> {
          bindingsSpec.bindInstance(new FileBackedReloadInformant(scriptFile));
          ClosureUtil.configureDelegateFirst(bindingsSpec, closures.getBindings());
        }).apply(r);
      });

      definition.handler(r -> {
        return Groovy.chain(r, closures.getHandlers());
      });
    }

    private static ServerConfigBuilder loadPropsIfPresent(ServerConfigBuilder serverConfigBuilder, Path baseDir) {
      Path propsFile = baseDir.resolve(BaseDir.DEFAULT_BASE_DIR_MARKER_FILE_PATH);
      if (Files.exists(propsFile)) {
        serverConfigBuilder.props(propsFile);
      }
      return serverConfigBuilder;
    }

    /**
     * Creates a handler defining function from a {@value #DEFAULT_HANDLERS_PATH} Groovy script.
     * <p>
     * This method returns a function that can be used with {@link RatpackServerSpec#handler(Function)} when bootstrapping.
     * <p>
     * The script should call the {@link Script#ratpack(Closure)} method, and <b>only</b> invoke the {@link Ratpack#handlers(Closure)} method.
     *
     * @return a handler definition function
     */
    public static Function<Registry, Handler> handlers() {
      return handlers(false);
    }

    /**
     * Creates a handler defining function from a {@value #DEFAULT_HANDLERS_PATH} Groovy script.
     * <p>
     * This method returns a function that can be used with {@link RatpackServerSpec#handler(Function)} when bootstrapping.
     * <p>
     * The script should call the {@link Script#ratpack(Closure)} method, and <b>only</b> invoke the {@link Ratpack#handlers(Closure)} method.
     *
     * @param compileStatic whether to statically compile the script
     * @return a handler definition function
     */
    public static Function<Registry, Handler> handlers(boolean compileStatic) {
      return handlers(compileStatic, DEFAULT_HANDLERS_PATH);
    }

    /**
     * Creates a handler defining function from a Groovy script.
     * <p>
     * This method returns a function that can be used with {@link RatpackServerSpec#handler(Function)} when bootstrapping.
     * <p>
     * The script should call the {@link Script#ratpack(Closure)} method, and <b>only</b> invoke the {@link Ratpack#handlers(Closure)} method.
     *
     * @param compileStatic whether to statically compile the script
     * @param scriptPath the path to the script
     * @return a handler definition function
     */
    public static Function<Registry, Handler> handlers(boolean compileStatic, String scriptPath) {
      return handlersWithArgs(compileStatic, scriptPath);
    }

    /**
     * Creates a handler defining function from a Groovy script.
     * <p>
     * This method returns a function that can be used with {@link RatpackServerSpec#handler(Function)} when bootstrapping.
     * <p>
     * The script should call the {@link Script#ratpack(Closure)} method, and <b>only</b> invoke the {@link Ratpack#handlers(Closure)} method.
     *
     * @param compileStatic whether to statically compile the script
     * @param scriptPath the path to the script
     * @param args args to make available to the script via the {@code args} variable
     * @return a handler definition function
     * @since 1.1
     */
    public static Function<Registry, Handler> handlersWithArgs(boolean compileStatic, String scriptPath, String... args) {
      checkGroovy();
      return r -> {
        Path scriptFile = r.get(FileSystemBinding.class).file(scriptPath);
        boolean development = r.get(ServerConfig.class).isDevelopment();
        return new ScriptBackedHandler(scriptFile, development,
          new RatpackDslScriptCapture(compileStatic, args, HandlersOnly::new)
            .andThen(RatpackDslClosures::getHandlers)
            .andThen(c -> Groovy.chain(r, c))
        );
      };
    }

    /**
     * Creates a registry building function from a Groovy script named {@value #DEFAULT_BINDINGS_PATH}.
     * <p>
     * This method returns a function that can be used with {@link RatpackServerSpec#registry(Function)} when bootstrapping.
     * <p>
     * The script should call the {@link Script#ratpack(Closure)} method, and <b>only</b> invoke the {@link Ratpack#bindings(Closure)} method.
     *
     * @return a registry definition function
     */
    public static Function<Registry, Registry> bindings() {
      return bindings(false);
    }

    /**
     * Creates a registry building function from a Groovy script named {@value #DEFAULT_BINDINGS_PATH}.
     * <p>
     * This method returns a function that can be used with {@link RatpackServerSpec#registry(Function)} when bootstrapping.
     * <p>
     * The script should call the {@link Script#ratpack(Closure)} method, and <b>only</b> invoke the {@link Ratpack#bindings(Closure)} method.
     *
     * @param compileStatic whether to statically compile the script
     * @return a registry definition function
     */
    public static Function<Registry, Registry> bindings(boolean compileStatic) {
      return bindings(compileStatic, DEFAULT_BINDINGS_PATH);
    }

    /**
     * Creates a registry building function from a Groovy script.
     * <p>
     * This method returns a function that can be used with {@link RatpackServerSpec#registry(Function)} when bootstrapping.
     * <p>
     * The script should call the {@link Script#ratpack(Closure)} method, and <b>only</b> invoke the {@link Ratpack#bindings(Closure)} method.
     *
     * @param compileStatic whether to statically compile the script
     * @param scriptPath the path to the script
     * @return a registry definition function
     */
    public static Function<Registry, Registry> bindings(boolean compileStatic, String scriptPath) {
      return bindingsWithArgs(compileStatic, scriptPath);
    }

    /**
     * Creates a registry building function from a Groovy script.
     * <p>
     * This method returns a function that can be used with {@link RatpackServerSpec#registry(Function)} when bootstrapping.
     * <p>
     * The script should call the {@link Script#ratpack(Closure)} method, and <b>only</b> invoke the {@link Ratpack#bindings(Closure)} method.
     *
     * @param compileStatic whether to statically compile the script
     * @param scriptPath the path to the script
     * @param args args to make available to the script via the {@code args} variable
     * @return a registry definition function
     * @since 1.1
     */
    public static Function<Registry, Registry> bindingsWithArgs(boolean compileStatic, String scriptPath, String... args) {
      checkGroovy();
      return r -> {
        Path scriptFile = r.get(FileSystemBinding.class).file(scriptPath);
        String script = Paths2.readText(scriptFile, StandardCharsets.UTF_8);
        Closure<?> bindingsClosure = new RatpackDslScriptCapture(compileStatic, args, BindingsOnly::new).andThen(RatpackDslClosures::getBindings).apply(scriptFile, script);
        return Guice.registry(bindingsSpec -> {
          bindingsSpec.bindInstance(new FileBackedReloadInformant(scriptFile));
          ClosureUtil.configureDelegateFirst(bindingsSpec, bindingsClosure);
        }).apply(r);
      };
    }
  }

  /**
   * Builds a handler chain, with no backing registry.
   *
   * @param serverConfig The application server config
   * @param closure The chain definition
   * @return A handler
   * @throws Exception any exception thrown by the given closure
   */
  public static Handler chain(ServerConfig serverConfig, @DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) throws Exception {
    return chain(serverConfig, null, closure);
  }

  /**
   * Builds a chain, backed by the given registry.
   *
   * @param serverConfig The application server config
   * @param registry The registry.
   * @param closure The chain building closure.
   * @return A handler
   * @throws Exception any exception thrown by the given closure
   */
  public static Handler chain(@Nullable ServerConfig serverConfig, @Nullable Registry registry, @DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) throws Exception {
    return ChainBuilders.build(
      new GroovyDslChainActionTransformer(serverConfig, registry),
      new ClosureInvoker<Object, GroovyChain>(closure).toAction(registry, Closure.DELEGATE_FIRST)
    );
  }

  /**
   * Builds a chain, backed by the given registry.
   *
   * @param registry The registry.
   * @param closure The chain building closure.
   * @return A handler
   * @throws Exception any exception thrown by the given closure
   */
  public static Handler chain(Registry registry, @DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) throws Exception {
    return chain(registry.get(ServerConfig.class), registry, closure);
  }

  /**
   * Creates a chain action based on the given closure.
   *
   * @param closure The chain building closure.
   * @return A chain action
   */
  public static Action<Chain> chainAction(@DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> closure) {
    return chain -> ClosureUtil.configureDelegateFirst(GroovyChain.from(chain), closure);
  }

  /**
   * Creates a {@link ratpack.handling.Context#render(Object) renderable} Groovy based template, using no model and the default content type.
   *
   * @param id The id/name of the template
   * @return a template
   */
  public static TextTemplate groovyTemplate(String id) {
    return groovyTemplate(id, null);
  }

  /**
   * Creates a {@link ratpack.handling.Context#render(Object) renderable} Groovy based markup template, using no model and the default content type.
   *
   * @param id The id/name of the template
   * @return a template
   */
  public static MarkupTemplate groovyMarkupTemplate(String id) {
    return groovyMarkupTemplate(id, (String) null);
  }

  /**
   * Creates a {@link ratpack.handling.Context#render(Object) renderable} Groovy based template, using no model.
   *
   * @param id The id/name of the template
   * @param type The content type of template
   * @return a template
   */
  public static TextTemplate groovyTemplate(String id, String type) {
    return groovyTemplate(ImmutableMap.<String, Object>of(), id, type);
  }

  /**
   * Creates a {@link ratpack.handling.Context#render(Object) renderable} Groovy based markup template, using no model.
   *
   * @param id The id/name of the template
   * @param type The content type of template
   * @return a template
   */
  public static MarkupTemplate groovyMarkupTemplate(String id, String type) {
    return groovyMarkupTemplate(ImmutableMap.<String, Object>of(), id, type);
  }

  /**
   * Creates a {@link ratpack.handling.Context#render(Object) renderable} Groovy based template, using the default content type.
   *
   * @param model The template model
   * @param id The id/name of the template
   * @return a template
   */
  public static TextTemplate groovyTemplate(Map<String, ?> model, String id) {
    return groovyTemplate(model, id, null);
  }

  /**
   * Creates a {@link ratpack.handling.Context#render(Object) renderable} Groovy based markup template, using the default content type.
   *
   * @param model The template model
   * @param id The id/name of the template
   * @return a template
   */
  public static MarkupTemplate groovyMarkupTemplate(Map<String, ?> model, String id) {
    return groovyMarkupTemplate(model, id, null);
  }

  /**
   * Creates a {@link ratpack.handling.Context#render(Object) renderable} Groovy based markup template, using the default content type.
   *
   * @param id the id/name of the template
   * @param modelBuilder an action the builds a model map
   * @return a template
   */
  public static MarkupTemplate groovyMarkupTemplate(String id, Action<? super ImmutableMap.Builder<String, Object>> modelBuilder) {
    return groovyMarkupTemplate(id, null, modelBuilder);
  }

  /**
   * Creates a {@link ratpack.handling.Context#render(Object) renderable} Groovy based markup template.
   *
   * @param id the id/name of the template
   * @param type The content type of template
   * @param modelBuilder an action the builds a model map
   * @return a template
   */
  public static MarkupTemplate groovyMarkupTemplate(String id, String type, Action<? super ImmutableMap.Builder<String, Object>> modelBuilder) {
    ImmutableMap<String, Object> model = uncheck(() -> Action.with(ImmutableMap.<String, Object>builder(), Action.noopIfNull(modelBuilder)).build());
    return groovyMarkupTemplate(model, id, type);
  }

  /**
   * Creates a {@link ratpack.handling.Context#render(Object) renderable} Groovy based template.
   *
   * @param model The template model
   * @param id The id/name of the template
   * @param type The content type of template
   * @return a template
   */
  public static TextTemplate groovyTemplate(Map<String, ?> model, String id, String type) {
    return new TextTemplate(model, id, type);
  }

  /**
   * Creates a {@link ratpack.handling.Context#render(Object) renderable} Groovy based template.
   *
   * @param model The template model
   * @param id The id/name of the template
   * @param type The content type of template
   * @return a template
   */
  public static MarkupTemplate groovyMarkupTemplate(Map<String, ?> model, String id, String type) {
    return new MarkupTemplate(id, type, model);
  }

  /**
   * Creates a handler instance from a closure.
   *
   * @param closure The closure to convert to a handler
   * @return The created handler
   */
  public static Handler groovyHandler(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
    return new ClosureBackedHandler(closure);
  }

  /**
   * Immediately executes the given {@code closure} against the given {@code chain}, as a {@link GroovyChain}.
   *
   * @param chain the chain to add handlers to
   * @param closure the definition of handlers to add
   * @throws Exception any exception thrown by {@code closure}
   */
  public static void chain(Chain chain, @DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) throws Exception {
    new ClosureInvoker<Object, GroovyChain>(closure).invoke(chain.getRegistry(), GroovyChain.from(chain), Closure.DELEGATE_FIRST);
  }

  /**
   * Creates a chain action implementation from the given closure.
   *
   * @param closure the definition of handlers to add
   * @throws Exception any exception thrown by {@code closure}
   * @return The created action
   */
  public static Action<Chain> chain(@DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> closure) throws Exception {
    return (c) -> Groovy.chain(c, closure);
  }

  /**
   * Shorthand for {@link #markupBuilder(CharSequence, Charset, Closure)} with a content type of {@code "text/html"} and {@code "UTF-8"} encoding.
   *
   * @param closure The html definition
   * @return A renderable object (i.e. to be used with the {@link ratpack.handling.Context#render(Object)} method
   */
  public static Markup htmlBuilder(@DelegatesTo(value = MarkupBuilder.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
    return markupBuilder(HttpHeaderConstants.HTML_UTF_8, CharsetUtil.UTF_8, closure);
  }

  /**
   * Renderable object for markup built using Groovy's {@link MarkupBuilder}.
   *
   * <pre class="groovy-chain-dsl">
   * import static ratpack.groovy.Groovy.markupBuilder
   *
   * get("some/path") {
   *   render markupBuilder("text/html", "UTF-8") {
   *     // MarkupBuilder DSL in here
   *   }
   * }
   * </pre>
   *
   * @param contentType The content type of the markup
   * @param encoding The character encoding of the markup
   * @param closure The definition of the markup
   * @return A renderable object (i.e. to be used with the {@link ratpack.handling.Context#render(Object)} method
   */
  public static Markup markupBuilder(CharSequence contentType, CharSequence encoding, @DelegatesTo(value = MarkupBuilder.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
    return new Markup(contentType, Charset.forName(encoding.toString()), closure);
  }

  public static Markup markupBuilder(CharSequence contentType, Charset encoding, @DelegatesTo(value = MarkupBuilder.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
    return new Markup(contentType, encoding, closure);
  }

  /**
   * Builds a content negotiating handler.
   *
   * @param registry the registry to obtain handlers from for by-class lookups
   * @param closure the spec action
   * @return a content negotiating handler
   * @throws Exception any thrown by {@code action}
   * @since 1.5
   */
  public static Handler byContent(Registry registry, @DelegatesTo(value = GroovyByMethodSpec.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) throws Exception {
    return Handlers.byContent(registry, s -> ClosureUtil.configureDelegateFirst(new DefaultGroovyByContentSpec(s), closure));
  }

  /**
   * Builds a multi method handler.
   *
   * @param registry the registry to obtain handlers from for by-class lookups
   * @param closure the spec action
   * @return a multi method handler
   * @throws Exception any thrown by {@code action}
   * @since 1.5
   */
  public static Handler byMethod(Registry registry, @DelegatesTo(value = GroovyByContentSpec.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) throws Exception {
    return Handlers.byMethod(registry, s -> ClosureUtil.configureDelegateFirst(new DefaultGroovyByMethodSpec(s), closure));
  }

}
