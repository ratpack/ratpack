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

package org.ratpackframework.assets.internal;

import org.jboss.netty.handler.codec.http.*;
import org.ratpackframework.Handler;
import org.ratpackframework.http.HttpDateParseException;
import org.ratpackframework.http.HttpDateUtil;
import org.ratpackframework.http.HttpExchange;
import org.ratpackframework.http.internal.DefaultHttpExchange;
import org.ratpackframework.http.internal.FileHttpTransmitter;
import org.ratpackframework.routing.Routed;

import java.io.File;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.IF_MODIFIED_SINCE;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.*;

public class FileStaticAssetRequestHandler implements Handler<Routed<HttpExchange>> {

  private final FileHttpTransmitter fileHttpTransmitter = new FileHttpTransmitter();

  @Override
  public void handle(final Routed<HttpExchange> routed) {
    HttpExchange exchange = routed.get();
    HttpRequest request = exchange.getRequest();
    if (request.getMethod() != HttpMethod.GET) {
      exchange.end(METHOD_NOT_ALLOWED);
      return;
    }

    File targetFile = exchange.getTargetFile();
    if (targetFile.isHidden() || !targetFile.exists()) {
      routed.next();
      return;
    }

    if (!targetFile.isFile()) {
      exchange.end(FORBIDDEN);
      return;
    }

    long lastModifiedTime = targetFile.lastModified();
    if (lastModifiedTime < 1) {
      routed.next();
      return;
    }

    HttpResponse response = exchange.getResponse();

    String ifModifiedSinceHeader = request.getHeader(IF_MODIFIED_SINCE);

    if (ifModifiedSinceHeader != null) {
      try {
        long ifModifiedSinceSecs = HttpDateUtil.parseDate(ifModifiedSinceHeader).getTime() / 1000;
        long lastModifiedSecs = lastModifiedTime / 1000;
        if (lastModifiedSecs == ifModifiedSinceSecs) {
          exchange.end(NOT_MODIFIED);
          return;
        }
      } catch (HttpDateParseException ignore) {
        // can't parse header, just keep going
      }
    }

    final String ifNoneMatch = request.getHeader(HttpHeaders.Names.IF_NONE_MATCH);
    if (ifNoneMatch != null && ifNoneMatch.trim().equals("*")) {
      response.setStatus(HttpResponseStatus.NOT_MODIFIED);
      exchange.end();
      return;
    }

    fileHttpTransmitter.transmit(targetFile, response, ((DefaultHttpExchange) exchange).getChannel());
  }
}
