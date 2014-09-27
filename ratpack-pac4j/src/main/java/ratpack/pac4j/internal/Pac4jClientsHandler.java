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

package ratpack.pac4j.internal;

import com.google.common.collect.ImmutableList;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.server.PublicAddress;

import java.util.List;

@SuppressWarnings("rawtypes")
public class Pac4jClientsHandler implements Handler {
  private final String callbackPath;
  private final List<Client> clients;

  public Pac4jClientsHandler(String callbackPath, Client... clients) {
    this.callbackPath = callbackPath;
    this.clients = ImmutableList.copyOf(clients);
  }

  public Pac4jClientsHandler(String callbackPath, List<Client> clients) {
    this.callbackPath = callbackPath;
    this.clients = ImmutableList.copyOf(clients);
  }

  @Override
  public void handle(Context context) throws Exception {
    String callbackUrl = context.get(PublicAddress.class).getAddress(context).toString() + "/" + callbackPath;
    context.getRequest().add(new Clients(callbackUrl, clients));
    context.next();
  }
}
