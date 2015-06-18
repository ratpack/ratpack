/*
 * Copyright 2015 the original author or authors.
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

package ratpack.remote;

import com.google.inject.multibindings.Multibinder;
import ratpack.guice.ConfigurableModule;
import ratpack.handling.HandlerDecorator;

import javax.inject.Inject;
import javax.inject.Provider;


/**
 * Provides support for RemoteControl
 * <p>
 *
 * This module provides the {@link HandlerDecorator} that takes care of executing arbitrary code from within a Ratpack application during testing.
 * <p>
 *
 * You may configure the path that the remote control responds to.
 *
 */
public class RemoteModule extends ConfigurableModule<RemoteModule.Config> {

  public static class Config {
    private String remotePath;

    public Config remotePath(String remotePath) {
      this.remotePath = remotePath;
      return this;
    }

    public String getRemotePath() {
      if (remotePath == null) {
        return RemoteControl.DEFAULT_REMOTE_CONTROL_PATH;
      }
      return this.remotePath;
    }
  }

  @Override
  protected void configure() {
    Multibinder.newSetBinder(binder(), HandlerDecorator.class)
      .addBinding().toProvider(HandlerDecoratorProvider.class);
  }

  private static class HandlerDecoratorProvider implements Provider<HandlerDecorator> {
    private Config config;

    @Inject
    public HandlerDecoratorProvider(Config config) {
      this.config = config;
    }

    @Override
    public HandlerDecorator get() {
      return RemoteControl.handlerDecorator(config.getRemotePath());
    }
  }
}
