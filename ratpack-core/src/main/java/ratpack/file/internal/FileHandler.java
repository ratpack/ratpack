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

import com.google.common.collect.ImmutableList;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.Request;
import ratpack.path.internal.PathBindingStorage;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

import static ratpack.file.internal.FileRenderer.readAttributes;
import static ratpack.file.internal.FileRenderer.sendFile;
import static ratpack.util.Exceptions.uncheck;

public class FileHandler implements Handler {

  private final ImmutableList<String> indexFiles;
  private final boolean cacheMetadata;

  public FileHandler(ImmutableList<String> indexFiles, boolean cacheMetadata) {
    this.indexFiles = indexFiles;
    this.cacheMetadata = cacheMetadata;
  }

  public void handle(Context context) throws Exception {
    String path = context.getExecution().get(PathBindingStorage.TYPE).peek().getPastBinding();

    // Decode the path.
    try {
      path = new URI(path).getPath();
    } catch (URISyntaxException e) {
      throw uncheck(e);
    }

    Path asset = context.file(path);
    if (asset != null) {
      servePath(context, asset);
    } else {
      context.clientError(404);
    }
  }

  private void servePath(final Context context, final Path file) throws Exception {
    readAttributes(file, cacheMetadata, attributes -> {
      if (attributes == null) {
        context.next();
      } else if (attributes.isRegularFile()) {
        sendFile(context, file, attributes);
      } else if (attributes.isDirectory()) {
        maybeSendFile(context, file, 0);
      } else {
        context.next();
      }
    });
  }

  private void maybeSendFile(final Context context, final Path file, final int i) throws Exception {
    if (i == indexFiles.size()) {
      context.next();
    } else {
      String name = indexFiles.get(i);
      final Path indexFile = file.resolve(name);
      readAttributes(indexFile, cacheMetadata, attributes -> {
        if (attributes != null && attributes.isRegularFile()) {
          String path = context.getRequest().getPath();
          if (path.endsWith("/") || path.isEmpty()) {
            sendFile(context, indexFile, attributes);
          } else {
            context.redirect(currentUriWithTrailingSlash(context));
          }
        } else {
          maybeSendFile(context, file, i + 1);
        }
      });
    }
  }

  private String currentUriWithTrailingSlash(Context context) {
    Request request = context.getRequest();
    String redirectUri = "/" + request.getPath() + "/";
    String query = request.getQuery();
    if (!query.isEmpty()) {
      redirectUri += "?" + query;
    }
    return redirectUri;
  }

}
