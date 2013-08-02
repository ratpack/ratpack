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

package org.ratpackframework.file.internal;

import io.netty.handler.codec.http.HttpHeaders;
import org.ratpackframework.file.FileSystemBinding;
import org.ratpackframework.file.MimeTypes;
import org.ratpackframework.handling.Context;
import org.ratpackframework.http.Request;
import org.ratpackframework.http.Response;
import org.ratpackframework.handling.Handler;

import java.io.File;
import java.util.Date;

import static io.netty.handler.codec.http.HttpHeaders.Names.IF_MODIFIED_SINCE;
import static io.netty.handler.codec.http.HttpResponseStatus.*;


public class FileStaticAssetRequestHandler implements Handler {

  public static final Handler INSTANCE = new FileStaticAssetRequestHandler();

  public void handle(Context context) {
    Request request = context.getRequest();
    Response response = context.getResponse();

    FileSystemBinding fileSystemBinding = context.get(FileSystemBinding.class);
    File targetFile = fileSystemBinding.getFile();

    if (targetFile.isHidden() || !targetFile.exists()) {
      context.next();
      return;
    }

    if (!request.getMethod().isGet()) {
      context.clientError(METHOD_NOT_ALLOWED.code());
      return;
    }

    if (!targetFile.isFile()) {
      context.clientError(FORBIDDEN.code());
      return;
    }

    long lastModifiedTime = targetFile.lastModified();
    if (lastModifiedTime < 1) {
      context.next();
      return;
    }

    Date ifModifiedSinceHeader = request.getDateHeader(IF_MODIFIED_SINCE);

    if (ifModifiedSinceHeader != null) {
      long ifModifiedSinceSecs = ifModifiedSinceHeader.getTime() / 1000;
      long lastModifiedSecs = lastModifiedTime / 1000;
      if (lastModifiedSecs == ifModifiedSinceSecs) {
        response.status(NOT_MODIFIED.code(), NOT_MODIFIED.reasonPhrase()).send();
        return;
      }
    }

    final String ifNoneMatch = request.getHeader(HttpHeaders.Names.IF_NONE_MATCH);
    if (ifNoneMatch != null && ifNoneMatch.trim().equals("*")) {
      response.status(NOT_MODIFIED.code(), NOT_MODIFIED.reasonPhrase()).send();
      return;
    }

    String contentType = context.get(MimeTypes.class).getContentType(targetFile);

    response.sendFile(contentType, targetFile);
  }
}
