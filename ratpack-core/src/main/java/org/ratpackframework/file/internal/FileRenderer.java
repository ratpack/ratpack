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
import org.ratpackframework.file.MimeTypes;
import org.ratpackframework.handling.Context;
import org.ratpackframework.http.Request;
import org.ratpackframework.http.Response;
import org.ratpackframework.render.ByTypeRenderer;

import javax.inject.Inject;
import java.io.File;
import java.util.Date;

import static io.netty.handler.codec.http.HttpHeaders.Names.IF_MODIFIED_SINCE;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED;

public class FileRenderer extends ByTypeRenderer<File> {

  @Inject
  public FileRenderer() {
    super(File.class);
  }

  @Override
  public void render(Context context, File targetFile) {
    Request request = context.getRequest();
    Response response = context.getResponse();

    if (targetFile.isHidden() || !targetFile.exists()) {
      context.next();
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
        context.getResponse().status(NOT_MODIFIED.code(), NOT_MODIFIED.reasonPhrase()).send();
        return;
      }
    }

    final String ifNoneMatch = request.getHeader(HttpHeaders.Names.IF_NONE_MATCH);
    if (ifNoneMatch != null && ifNoneMatch.trim().equals("*")) {
      response.status(NOT_MODIFIED.code(), NOT_MODIFIED.reasonPhrase()).send();
      return;
    }

    String contentType = context.get(MimeTypes.class).getContentType(targetFile.getName());
    response.sendFile(contentType, targetFile);
  }

}
