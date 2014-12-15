/*
 * Copyright 2014 the original author or authors.
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

package ratpack.guice;

import com.google.common.reflect.Invokable;
import com.google.common.reflect.TypeToken;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import ratpack.func.Action;
import ratpack.func.Factory;
import ratpack.launch.LaunchConfig;
import ratpack.util.ExceptionUtils;

import javax.inject.Singleton;
import java.lang.reflect.Constructor;

/**
 * Provides a standard approach for modules that require some parametrization / configuration.
 * <p>
 * A configurable module provides a single, mutable, “config object” (type parameter {@code C}).
 * The {@link ratpack.guice.BindingsSpec#add(Class, Action)} method can be used to add the module and configure it at the same time.
 * It is conventional, but not required, for the config type to be a nested static class named {@code Config} of the module class.
 * <pre class="java">
 * import com.google.inject.Provides;
 * import ratpack.guice.ConfigurableModule;
 * import ratpack.guice.Guice;
 * import ratpack.test.embed.EmbeddedApp;
 * 
 * public class Example {
 * 
 *   public static class StringModule extends ConfigurableModule&lt;StringModule.Config&gt; {
 *     public static class Config {
 *       private String value;
 * 
 *       public void value(String value) {
 *         this.value = value;
 *       }
 *     }
 * 
 *     protected void configure() {}
 * 
 *     {@literal @}Provides
 *     String provideString(Config config) {
 *       return config.value;
 *     }
 *   }
 * 
 *   public static void main(String... args) {
 *     EmbeddedApp.fromHandlerFactory(launchConfig -&gt;
 *         Guice.builder(launchConfig)
 *           .bindings(b -&gt;
 *               b.add(StringModule.class, c -&gt; c.value("foo"))
 *           )
 *           .build(chain -&gt; chain
 *               .get(ctx -&gt;
 *                   ctx.render(ctx.get(String.class))
 *               )
 *           )
 *     ).test(httpClient -&gt; {
 *       assert httpClient.getText().equals("foo");
 *     });
 *   }
 * }
 * </pre>
 * <p>
 * Alternatively, the config object can be provided as a separate binding.
 * <pre class="java">
 * import com.google.inject.Provides;
 * import ratpack.guice.ConfigurableModule;
 * import ratpack.guice.Guice;
 * import ratpack.test.embed.EmbeddedApp;
 * 
 * public class Example {
 *   public static class StringModule extends ConfigurableModule&lt;StringModule.Config&gt; {
 *     public static class Config {
 *       private String value;
 * 
 *       public Config value(String value) {
 *         this.value = value;
 *         return this;
 *       }
 *     }
 * 
 *     protected void configure() {
 *     }
 * 
 *     {@literal @}Provides
 *     String provideString(Config config) {
 *       return config.value;
 *     }
 *   }
 * 
 *   public static void main(String... args) {
 *     EmbeddedApp.fromHandlerFactory(launchConfig -&gt;
 *         Guice.builder(launchConfig)
 *           .bindings(b -&gt; b
 *               .add(StringModule.class)
 *               .bindInstance(new StringModule.Config().value("bar"))
 *           )
 *           .build(chain -&gt; chain
 *               .get(ctx -&gt;
 *                   ctx.render(ctx.get(String.class))
 *               )
 *           )
 *     ).test(httpClient -&gt; {
 *       assert httpClient.getText().equals("bar");
 *     });
 *   }
 * }
 * </pre>
 *
 * @param <T> the type of the config object
 */
public abstract class ConfigurableModule<T> extends AbstractModule {

  private Action<? super T> configurer = Action.noop();

  /**
   * Registers the configuration action.
   * <p>
   * This method is called by {@link ratpack.guice.BindingsSpec#add(Class, Action)}.
   *
   * @param configurer the configuration action.
   */
  public void configure(Action<? super T> configurer) {
    this.configurer = configurer;
  }

  /**
   * Creates the configuration object.
   * <p>
   * This implementation reflectively creates an instance of the type denoted by type param {@code C}.
   * In order for this to succeed, the following needs to be met:
   * <ul>
   * <li>The type must be public.</li>
   * <li>Must have a public constructor that takes only a {@link LaunchConfig}, or takes no args.</li>
   * </ul>
   * <p>
   * If the config object cannot be created this way, override this method.
   *
   * @param launchConfig the application launch config
   * @return a newly created config object
   */
  protected T createConfig(LaunchConfig launchConfig) {
    TypeToken<T> typeToken = new TypeToken<T>(getClass()) {
    };

    if (typeToken.getType() instanceof Class) {
      @SuppressWarnings("unchecked") Class<T> clazz = (Class<T>) typeToken.getRawType();
      Factory<T> factory;
      try {
        Constructor<T> constructor = clazz.getConstructor(LaunchConfig.class);
        factory = () -> Invokable.from(constructor).invoke(null, launchConfig);
      } catch (NoSuchMethodException ignore) {
        try {
          Constructor<T> constructor = clazz.getConstructor();
          factory = () -> Invokable.from(constructor).invoke(null);
        } catch (NoSuchMethodException e) {
          throw new IllegalStateException("No suitable constructor (no arg, or just LaunchConfig) for module config type " + typeToken);
        }
      }

      return ExceptionUtils.uncheck(factory);
    } else {
      throw new IllegalStateException("Can't auto instantiate configuration type " + typeToken + " as it is not a simple class");
    }
  }

  /**
   * Hook for applying any default configuration to the configuration object created by {@link #createConfig(LaunchConfig)}.
   * <p>
   * This can be used if it's not possible to apply the configuration in the constructor.
   *
   * @param launchConfig the application launch config
   * @param config the config object
   */
  protected void defaultConfig(LaunchConfig launchConfig, T config) {

  }

  /**
   * Binds the config object, after creating it via {@link #createConfig(LaunchConfig)} and after giving it to {@link #defaultConfig(LaunchConfig, Object)}.
   *
   * @param launchConfig the application launch config
   * @return the config object
   */
  @Provides
  @Singleton
  T provideConfig(LaunchConfig launchConfig) {
    T configuration = createConfig(launchConfig);
    defaultConfig(launchConfig, configuration);
    try {
      configurer.execute(configuration);
    } catch (Exception e) {
      throw ExceptionUtils.uncheck(e);
    }
    return configuration;
  }

}
