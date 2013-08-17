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
import org.ratpackframework.util.Action;

import javax.inject.Inject;
import java.io.File;
import java.util.Date;
import java.util.concurrent.Callable;

import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED;

public class FileRenderer extends ByTypeRenderer<File> {

  @Inject
  public FileRenderer() {
    super(File.class);
  }

  @Override
  public void render(final Context context, final File targetFile) {
    final Request request = context.getRequest();
    final Response response = context.getResponse();

    if (!targetFile.isFile()) {
      context.clientError(FORBIDDEN.code());
      return;
    }

    context.getBlocking().exec(new Callable<Long>() {
      @Override
      public Long call() throws Exception {
        return targetFile.lastModified();
      }
    }).then(new Action<Long>() {
      @Override
      public void execute(Long lastModifiedTime) {
        sendFile(context, targetFile, request, response, lastModifiedTime);
      }
    });
  }

  private void sendFile(final Context context, final File targetFile, final Request request, final Response response, long lastModifiedTime) {
    if (lastModifiedTime < 1) {
      context.next();
      return;
    }

    context.lastModified(new Date(lastModifiedTime), new Runnable() {
      @Override
      public void run() {
        final String ifNoneMatch = request.getHeaders().get(HttpHeaders.Names.IF_NONE_MATCH);
        if (ifNoneMatch != null && ifNoneMatch.trim().equals("*")) {
          response.status(NOT_MODIFIED.code(), NOT_MODIFIED.reasonPhrase()).send();
          return;
        }

        String contentType = context.get(MimeTypes.class).getContentType(targetFile.getName());
        response.sendFile(context.getBlocking(), contentType, targetFile);
      }
    });

  }

}