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

package ratpack.file.internal;

import io.netty.handler.codec.http.HttpHeaders;
import ratpack.exec.ExecContext;
import ratpack.file.FileRenderer;
import ratpack.file.MimeTypes;
import ratpack.func.Action;
import ratpack.handling.Context;
import ratpack.http.Response;
import ratpack.render.RendererSupport;
import ratpack.util.ExceptionUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.concurrent.Callable;

import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED;

public class DefaultFileRenderer extends RendererSupport<Path> implements FileRenderer {

  @Override
  public void render(final Context context, final Path targetFile) throws Exception {
    readAttributes(context, targetFile, new Action<BasicFileAttributes>() {
      @Override
      public void execute(BasicFileAttributes attributes) throws Exception {
        if (attributes == null || !attributes.isRegularFile()) {
          context.clientError(404);
        } else {
          sendFile(context, targetFile, attributes);
        }
      }
    });
  }

  public static void sendFile(final Context context, final Path file, final BasicFileAttributes attributes) {
    if (!context.getRequest().getMethod().isGet()) {
      context.clientError(405);
      return;
    }

    Date date = new Date(attributes.lastModifiedTime().toMillis());

    context.lastModified(date, new Runnable() {
      public void run() {
        final String ifNoneMatch = context.getRequest().getHeaders().get(HttpHeaders.Names.IF_NONE_MATCH);
        Response response = context.getResponse();
        if (ifNoneMatch != null && ifNoneMatch.trim().equals("*")) {
          response.status(NOT_MODIFIED.code(), NOT_MODIFIED.reasonPhrase()).send();
          return;
        }

        if (!response.getHeaders().contains(HttpHeaders.Names.CONTENT_TYPE)) {
          String contentType = context.get(MimeTypes.class).getContentType(file.getFileName().toString());
          response.contentType(contentType);
        }

        try {
          response.sendFile(context, attributes, file);
        } catch (Exception e) {
          throw ExceptionUtils.uncheck(e);
        }
      }
    });
  }

  public static void readAttributes(ExecContext execContext, final Path file, Action<? super BasicFileAttributes> then) throws Exception {
    execContext.blocking(new Callable<BasicFileAttributes>() {
      public BasicFileAttributes call() throws Exception {
        if (Files.exists(file)) {
          return Files.readAttributes(file, BasicFileAttributes.class);
        } else {
          return null;
        }
      }
    }).then(then);
  }

}