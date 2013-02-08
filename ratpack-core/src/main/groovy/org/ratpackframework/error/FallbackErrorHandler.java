/*
 * Copyright 2012 the original author or authors.
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

package org.ratpackframework.error;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.ratpackframework.handler.HttpExchange;
import org.ratpackframework.handler.Result;
import org.ratpackframework.handler.ResultHandler;

/**
 * Used as a last attempt to handle the error. Useful for wrapping more sophisticated error handlers.
 */
@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
public class FallbackErrorHandler implements ResultHandler<ChannelBuffer> {

  private final HttpExchange exchange;
  private final String operationDescription;

  public FallbackErrorHandler(HttpExchange exchange, String operationDescription) {
    this.exchange = exchange;
    this.operationDescription = operationDescription;
  }

  @Override
  public void handle(Result<ChannelBuffer> event) {
    HttpResponse response = exchange.getResponse();
    if (event.isFailure()) {
      event.getFailure().printStackTrace(System.err);
      response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
      exchange.end("plain/text", "Unhandled exception occurred during " + operationDescription);
    } else {
      exchange.end(event.getValue());
    }
  }
}
