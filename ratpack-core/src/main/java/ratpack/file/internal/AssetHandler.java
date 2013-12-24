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
import ratpack.file.FileSystemBinding;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.Request;
import ratpack.path.PathBinding;
import ratpack.util.Action;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.attribute.BasicFileAttributes;

import static ratpack.file.internal.DefaultFileRenderer.readAttributes;
import static ratpack.file.internal.DefaultFileRenderer.sendFile;
import static ratpack.util.ExceptionUtils.uncheck;

public class AssetHandler implements Handler {

  private final ImmutableList<String> indexFiles;

  public AssetHandler(ImmutableList<String> indexFiles) {
    this.indexFiles = indexFiles;
  }

  public void handle(Context context) {
    Request request = context.getRequest();

    String path = request.getPath();
    PathBinding pathBinding = context.maybeGet(PathBinding.class);
    if (pathBinding != null) {
      path = pathBinding.getPastBinding();
    }

    // Decode the path.
    try {
      path = new URI(path).getPath();
    } catch (URISyntaxException e) {
      throw uncheck(e);
    }

    File asset = context.file(path);
    if (asset != null) {
      servePath(context, asset);
    } else {
      context.clientError(404);
    }
  }

  private void servePath(final Context context, final File file) {
    readAttributes(context.getBackground(), file, new Action<BasicFileAttributes>() {
      public void execute(BasicFileAttributes attributes) {
        if (attributes == null) {
          context.next();
        } else if (attributes.isRegularFile()) {
          sendFile(context, file, attributes);
        } else if (attributes.isDirectory()) {
          maybeSendFile(context, file, 0);
        } else {
          context.next();
        }
      }
    });
  }

  private void maybeSendFile(final Context context, final File file, final int i) {
    if (i == indexFiles.size()) {
      context.next();
    } else {
      String name = indexFiles.get(i);
      final File indexFile = new File(file, name);
      readAttributes(context.getBackground(), indexFile, new Action<BasicFileAttributes>() {
        public void execute(BasicFileAttributes attributes) {
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

  private FileSystemBinding getFileSystemBinding(Context context) {
    return context.get(FileSystemBinding.class);
  }

}
