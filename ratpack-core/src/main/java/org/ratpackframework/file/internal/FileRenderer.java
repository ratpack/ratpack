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
import org.ratpackframework.block.Blocking;
import org.ratpackframework.file.MimeTypes;
import org.ratpackframework.handling.Context;
import org.ratpackframework.render.ByTypeRenderer;
import org.ratpackframework.util.Action;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.concurrent.Callable;

import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED;

public class FileRenderer extends ByTypeRenderer<File> {

  @Override
  public void render(final Context context, final File targetFile) {
    readAttributes(context.getBlocking(), targetFile, new Action<BasicFileAttributes>() {
      @Override
      public void execute(BasicFileAttributes attributes) {
        if (attributes == null || !attributes.isRegularFile()) {
          context.clientError(404);
        } else {
          sendFile(context, targetFile, attributes);
        }
      }
    });
  }

  public static void sendFile(final Context context, final File file, final BasicFileAttributes attributes) {
    if (!context.getRequest().getMethod().isGet()) {
      context.clientError(405);
      return;
    }

    Date date = new Date(attributes.lastModifiedTime().toMillis());

    context.lastModified(date, new Runnable() {
      public void run() {
        final String ifNoneMatch = context.getRequest().getHeaders().get(HttpHeaders.Names.IF_NONE_MATCH);
        if (ifNoneMatch != null && ifNoneMatch.trim().equals("*")) {
          context.getResponse().status(NOT_MODIFIED.code(), NOT_MODIFIED.reasonPhrase()).send();
          return;
        }

        String contentType = context.get(MimeTypes.class).getContentType(file.getName());
        context.getResponse().sendFile(context.getBlocking(), contentType, attributes, file);
      }
    });
  }

  public static void readAttributes(Blocking blocking, final File file, Action<? super BasicFileAttributes> then) {
    blocking.exec(new Callable<BasicFileAttributes>() {
      public BasicFileAttributes call() throws Exception {
        Path path = Paths.get(file.toURI());
        if (Files.exists(path)) {
          return Files.readAttributes(path, BasicFileAttributes.class);
        } else {
          return null;
        }
      }
    }).then(then);
  }

}