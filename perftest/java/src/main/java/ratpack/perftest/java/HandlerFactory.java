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

package ratpack.perftest.java;

import ratpack.handling.Chain;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.launch.LaunchConfig;
import ratpack.util.Action;

import static ratpack.handling.Handlers.chain;

public class HandlerFactory implements ratpack.launch.HandlerFactory {

  public Handler create(LaunchConfig launchConfig) throws Exception {
    return chain(launchConfig, new Action<Chain>() {
      public void execute(Chain chain) {
        chain
          .assets("public")
          .prefix("path", new Handler() {
            public void handle(Context context) {
              context.getResponse().send(context.getRequest().getUri());
            }
          });
      }
    });
  }

}
