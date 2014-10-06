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

public abstract class ConfigurableModule<T> extends AbstractModule {

  private Action<? super T> configurer = Action.noop();

  public void configure(Action<? super T> configurer) {
    this.configurer = configurer;
  }

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

  protected void defaultConfig(LaunchConfig launchConfig, T config) {

  }

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
