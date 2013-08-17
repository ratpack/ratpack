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

package org.ratpackframework.remote;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import org.ratpackframework.guice.HandlerDecoratingModule;
import org.ratpackframework.handling.Handler;
import org.ratpackframework.launch.LaunchConfig;
import org.ratpackframework.remote.internal.RemoteControlHandler;

import static com.google.common.collect.ImmutableList.of;
import static org.ratpackframework.handling.Handlers.chain;
import static org.ratpackframework.handling.Handlers.post;

/**
 * An extension module that adds a Groovy Remote Control endpoint.
 * <p>
 * To use it one has to register the module.
 * </p>
 * <p>
 * By default the endpoind is registered under {@code /remote-control}. This can be configured using {@link #setPath(String)} or
 * {@code other.remoteControl.path} configuration property.
 * </p>
 * <p>
 * The endpoint is not registered unless {@code other.remoteControl.enabled} configuration property is set to {@code true}. This
 * is so that you have to explicitly enable it, for example when integration testing the application, and it's harder to make a mistake
 * of keeping it on for production. Securing the endpoint when used in production is left for the users to implement if desired.
 * </p>
 *
 * Example usage: (Java DSL)
 * <pre class="tested">
 * import org.ratpackframework.handling.*;
 * import org.ratpackframework.guice.*;
 * import org.ratpackframework.util.*;
 * import org.ratpackframework.launch.*;
 * import org.ratpackframework.remote.RemoteControlModule;
 *
 * class ModuleBootstrap implements Action&lt;ModuleRegistry&gt; {
 *   public void execute(ModuleRegistry modules) {
 *     modules.register(new RemoteControlModule());
 *   }
 * }
 *
 * LaunchConfig launchConfig = LaunchConfigBuilder.baseDir(new File("appRoot"))
 *   .build(new HandlerFactory() {
 *     public Handler create(LaunchConfig launchConfig) {
 *       return Guice.handler(launchConfig, new ModuleBootstrap(), new Action&lt;Chain&gt;() {
 *         public void execute(Chain chain) {
 *           chain.add(chain.getRegistry().get(MyHandler.class));
 *         }
 *       });
 *     }
 *   });
 * </pre>
 *
 * Example usage: (Groovy DSL)
 * <pre class="groovy-ratpack-dsl">
 * import org.ratpackframework.remote.RemoteControlModule
 *
 * ratpack {
 *   modules {
 *     register new RemoteControlModule()
 *   }
 * }
 * </pre>
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
    LaunchConfig launchConfig = injector.getInstance(LaunchConfig.class);
    String endpointPath = path == null ? launchConfig.getOther("remoteControl.path", "remote-control") : path;
    boolean enabled = Boolean.valueOf(launchConfig.getOther("remoteControl.enabled", "false"));

    if (enabled) {
      return chain(of(
        post(endpointPath, new RemoteControlHandler(injector)),
        handler
      ));
    } else {
      return handler;
    }
  }
}
