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

package org.ratpackframework.file.internal;

import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.ratpackframework.routing.Exchange;
import org.ratpackframework.routing.Handler;
import org.ratpackframework.http.Request;
import org.ratpackframework.http.Response;
import org.ratpackframework.file.FileSystemContext;
import org.ratpackframework.http.internal.HttpDateParseException;
import org.ratpackframework.http.internal.HttpDateUtil;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.IF_MODIFIED_SINCE;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.*;

public class FileStaticAssetRequestHandler implements Handler {

  @Override
  public void handle(Exchange exchange) {
    Request request = exchange.getRequest();
    Response response = exchange.getResponse();

    if (request.getMethod().isGet()) {
      response.status(METHOD_NOT_ALLOWED.getCode(), METHOD_NOT_ALLOWED.getReasonPhrase()).send();
      return;
    }

    FileSystemContext fileSystemContext = exchange.getContext().require(FileSystemContext.class);
    File targetFile = fileSystemContext.getFile();

    if (targetFile.isHidden() || !targetFile.exists()) {
      exchange.next();
      return;
    }

    if (!targetFile.isFile()) {
      response.status(FORBIDDEN.getCode(), FORBIDDEN.getReasonPhrase()).send();
      return;
    }

    long lastModifiedTime = targetFile.lastModified();
    if (lastModifiedTime < 1) {
      exchange.next();
      return;
    }

    String ifModifiedSinceHeader = request.getHeader(IF_MODIFIED_SINCE);

    if (ifModifiedSinceHeader != null) {
      try {
        long ifModifiedSinceSecs = HttpDateUtil.parseDate(ifModifiedSinceHeader).getTime() / 1000;
        long lastModifiedSecs = lastModifiedTime / 1000;
        if (lastModifiedSecs == ifModifiedSinceSecs) {
          response.status(NOT_MODIFIED.getCode(), NOT_MODIFIED.getReasonPhrase());
          return;
        }
      } catch (HttpDateParseException ignore) {
        // can't parse header, just keep going
      }
    }

    final String ifNoneMatch = request.getHeader(HttpHeaders.Names.IF_NONE_MATCH);
    if (ifNoneMatch != null && ifNoneMatch.trim().equals("*")) {
      response.status(NOT_MODIFIED.getCode(), NOT_MODIFIED.getReasonPhrase()).send();
      return;
    }

    MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
    String contentType = mimeTypesMap.getContentType(targetFile.getPath());

    response.sendFile(contentType, targetFile);
  }
}
