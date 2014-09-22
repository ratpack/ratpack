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

package ratpack.test.embed;

import ratpack.func.Action;
import ratpack.handling.Chain;
import ratpack.handling.Handlers;
import ratpack.launch.LaunchConfig;
import ratpack.launch.LaunchConfigBuilder;

public interface EmbeddedApplicationBuilder {

  static EmbeddedApplicationBuilder builder() {
    return new EmbeddedApplicationBuilder() {
      private Action<? super LaunchConfigBuilder> launchConfigAction = Action.noop();

      @Override
      public EmbeddedApplicationBuilder launchConfig(Action<? super LaunchConfigBuilder> action) {
        launchConfigAction = action;
        return null;
      }

      @Override
      public EmbeddedApplication build(Action<? super Chain> action) {
        return new LaunchConfigEmbeddedApplication() {
          @Override
          protected LaunchConfig createLaunchConfig() {
            LaunchConfigBuilder launchConfigBuilder = LaunchConfigBuilder.noBaseDir().port(0);
            launchConfigAction.toConsumer().accept(launchConfigBuilder);
            return launchConfigBuilder.build(launchConfig -> Handlers.chain(launchConfig, action));
          }
        };
      }
    };
  }

  EmbeddedApplicationBuilder launchConfig(Action<? super LaunchConfigBuilder> action);

  EmbeddedApplication build(Action<? super Chain> action);

}
