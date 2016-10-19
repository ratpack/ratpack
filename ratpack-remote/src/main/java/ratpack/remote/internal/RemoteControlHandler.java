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

package ratpack.remote.internal;

import io.remotecontrol.groovy.ContentType;
import io.remotecontrol.server.Receiver;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.handling.internal.ChainHandler;
import ratpack.registry.Registry;
import ratpack.registry.RegistryBuilder;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicReference;

import static ratpack.handling.Handlers.*;

public class RemoteControlHandler implements Handler {

  private final Registry registry;
  private final Handler[] rest;

  private final AtomicReference<Registry> registryReference = new AtomicReference<>();
  private final Handler handler;

  public RemoteControlHandler(String endpointPath, Registry registry, Handler rest) {
    this.registry = registry;
    this.rest = ChainHandler.unpack(rest);
    this.handler = chain(
      path(
        endpointPath,
        chain(
          post(),
          contentTypes(ContentType.COMMAND.getValue()),
          accepts(ContentType.RESULT.getValue()),
          new CommandHandler()
        )
      ),
      new InsertHandler()
    );
  }

  private class InsertHandler implements Handler {
    @Override
    public void handle(Context context) throws Exception {
      Registry registryInjection = registryReference.get();
      if (registryInjection == null) {
        context.insert(rest);
      } else {
        context.insert(registryInjection, rest);
      }
    }
  }

  private class CommandHandler implements Handler {
    @Override
    public void handle(Context context) throws Exception {
      final Registry commandRegistry = context.join(registry);
      final RegistryBuilder registryBuilder = Registry.builder();

      Receiver receiver = new RatpackReceiver(chain -> new DelegatingCommandDelegate(registryBuilder, commandRegistry) {
        @Override
        public void clearRegistry() {
          registryReference.set(null);
        }
      });

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      context.getRequest().getBody()
        .blockingOp(body -> receiver.execute(body.getInputStream(), outputStream))
        .then(body -> {
          if (registryBuilder.size() > 0) {
            Registry newRegistry = registryBuilder.build();
            registryReference.set(newRegistry);
          }

          context.getResponse().send(ContentType.RESULT.getValue(), outputStream.toByteArray());
        });
    }
  }

  @Override
  public void handle(final Context context) throws Exception {
    handler.handle(context);
  }

}
