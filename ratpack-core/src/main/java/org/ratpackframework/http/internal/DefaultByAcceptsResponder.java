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

package org.ratpackframework.http.internal;

import io.netty.handler.codec.http.HttpHeaders;
import org.ratpackframework.handling.Exchange;
import org.ratpackframework.http.ByAcceptsResponder;

import java.util.*;

public class DefaultByAcceptsResponder implements ByAcceptsResponder {

  private final Map<String, Runnable> map = new LinkedHashMap<String, Runnable>(3);
  private String first;
  private final Exchange exchange;

  public DefaultByAcceptsResponder(Exchange exchange) {
    this.exchange = exchange;
  }

  public ByAcceptsResponder type(String mimeType, Runnable runnable) {
    if (mimeType == null) {
      throw new IllegalArgumentException("mimeType cannot be null");
    }

    String trimmed = mimeType.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("mimeType cannot be a blank string");
    }

    if (first == null) {
      first = trimmed;
    }
    map.put(trimmed, runnable);
    return this;
  }

  public void send() {
    if (first == null) {
      exchange.clientError(406);
      return;
    }

    List<String> types = new ArrayList<String>(map.keySet());
    Collections.reverse(types);
    String winner = first;

    String acceptHeader = exchange.getRequest().getHeader(HttpHeaders.Names.ACCEPT);
    if (acceptHeader != null && !acceptHeader.isEmpty()) {
      winner = MimeParse.bestMatch(types, acceptHeader);
    }

    if (winner == null || winner.isEmpty()) {
      exchange.clientError(406);
    } else {
      exchange.getResponse().setHeader(HttpHeaders.Names.CONTENT_TYPE, winner);
      Runnable runnable = map.get(winner);
      runnable.run();
    }
  }

}
