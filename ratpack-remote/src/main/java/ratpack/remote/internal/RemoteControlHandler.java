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

import com.google.common.collect.ImmutableMap;
import groovyx.remote.server.Receiver;
import io.netty.handler.codec.http.HttpHeaders;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.Request;
import ratpack.registry.Registry;
import ratpack.registry.RegistryBuilder;

import java.io.ByteArrayOutputStream;

import static io.netty.handler.codec.http.HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE;

public class RemoteControlHandler implements Handler {

  public static final String RESPONSE_CONTENT_TYPE = "application/groovy-remote-control-result";
  public static final String REQUEST_CONTENT_TYPE = "application/groovy-remote-control-command";

  private final Registry registry;

  public RemoteControlHandler(Registry registry) {
    this.registry = registry;
  }

  private boolean validContentType(Request request) {
    String value = request.getHeaders().get(HttpHeaders.Names.CONTENT_TYPE);
    return REQUEST_CONTENT_TYPE.equals(value);
  }

  @Override
  public void handle(final Context context) {
    Request request = context.getRequest();

    if (validContentType(request)) {
      context.respond(context.getByContent().type(RESPONSE_CONTENT_TYPE, new Runnable() {
        @Override
        public void run() {
          Registry commandRegistry = RegistryBuilder.join(context, registry);
          Receiver receiver = new Receiver(ImmutableMap.of("registry", commandRegistry));

          ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
          receiver.execute(context.getRequest().getInputStream(), outputStream);

          context.getResponse().send(RESPONSE_CONTENT_TYPE, outputStream.toByteArray());
        }
      }));
    } else {
      context.clientError(UNSUPPORTED_MEDIA_TYPE.code());
    }
  }
}
