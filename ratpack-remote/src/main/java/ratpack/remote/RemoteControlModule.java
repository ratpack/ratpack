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

package ratpack.remote;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import ratpack.guice.Guice;
import ratpack.guice.HandlerDecoratingModule;
import ratpack.handling.Handler;
import ratpack.launch.ServerConfig;
import ratpack.remote.internal.RemoteControlHandler;

/**
 * An extension module that adds a Groovy Remote Control endpoint.
 * <p>
 * To use it one has to register the module.
 * </p>
 * <p>
 * By default the endpoint is registered under {@code /remote-control}. This can be configured using {@link #setPath(String)} or
 * {@code other.remoteControl.path} configuration property.
 * </p>
 * <p>
 * The endpoint is not registered unless {@code other.remoteControl.enabled} configuration property is set to {@code true} or reloading
 * is enabled. This is so that you have to explicitly enable it, for example when integration testing the application, and it's harder
 * to make a mistake of keeping it on for production. Securing the endpoint when used in production is left for the users to implement if desired.
 * </p>
 * <p>
 * Command context is populated with the registry of the remote application which is available as {@code registry} variable.
 * </p>
 *
 * @see <a href="http://groovy.codehaus.org/modules/remote/" target="_blank">Groovy Remote Control</a>
 */
public class RemoteControlModule extends AbstractModule implements HandlerDecoratingModule {

  public static final String DEFAULT_REMOTE_CONTROL_PATH = "remote-control";

  private String path;

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  @Override
  protected void configure() {
  }

  @Override
  public Handler decorate(Injector injector, Handler handler) {
    ServerConfig serverConfig = injector.getInstance(ServerConfig.class);
    String endpointPath = path == null ? serverConfig.getOther("remoteControl.path", "remote-control") : path;
    boolean enabled = Boolean.valueOf(serverConfig.getOther("remoteControl.enabled", "false")) || serverConfig.isDevelopment();

    if (enabled) {
      return new RemoteControlHandler(endpointPath, Guice.justInTimeRegistry(injector), handler);
    } else {
      return handler;
    }
  }


}
