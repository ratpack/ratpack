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

package ratpack.messaging.internal;

import com.google.common.collect.Maps;
import org.apache.camel.Exchange;
import ratpack.messaging.MessageResponse;

import java.util.Map;

public class DefaultMessageResponse implements MessageResponse {
  private final Exchange exchange;

  DefaultMessageResponse(Exchange exchange) {
    this.exchange = exchange;
  }

  @Override
  public void send(String message) {
    exchange.getOut().setBody(message);
  }

  @Override
  public void send(Map<String, String> headers, String message) {
    exchange.getOut().setBody(message);

    // Ridiculous, Java.
    exchange.getOut().setHeaders(Maps.transformEntries(headers, new Maps.EntryTransformer<String, String, Object>() {
      public Object transformEntry(String key, String value) {
        return value;
      }
    }));
  }
}
