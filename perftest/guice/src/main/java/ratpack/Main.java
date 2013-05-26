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

package ratpack;

import org.ratpackframework.bootstrap.RatpackServer;
import org.ratpackframework.bootstrap.RatpackServerBuilder;
import org.ratpackframework.guice.Injection;
import org.ratpackframework.handling.Chain;
import org.ratpackframework.handling.Handler;
import org.ratpackframework.util.Action;

import java.io.File;

import static org.ratpackframework.handling.Handlers.chain;
import static org.ratpackframework.handling.Handlers.get;

public class Main {

  public static void main(String[] args) throws Exception {

    Handler handler = Injection.handler(new ModuleBootstrap(), chain(new Action<Chain>() {
      public void execute(Chain chain) {
        chain.add(get(Injection.handler(InjectedHandler.class)));
      }
    }));

    File dir = new File("src/ratpack");
    RatpackServerBuilder ratpackServerBuilder = new RatpackServerBuilder(handler, dir);
    ratpackServerBuilder.setWorkerThreads(0); // don't use a worker connection pool
    RatpackServer server = ratpackServerBuilder.build();

    server.startAndWait();
  }

}
