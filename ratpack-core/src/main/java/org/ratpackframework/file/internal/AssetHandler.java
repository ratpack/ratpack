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

import com.google.common.collect.ImmutableList;
import org.ratpackframework.file.IndexFiles;
import org.ratpackframework.handling.Context;
import org.ratpackframework.handling.Handler;
import org.ratpackframework.http.Request;
import org.ratpackframework.path.PathBinding;
import org.ratpackframework.util.Action;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.attribute.BasicFileAttributes;

import static org.ratpackframework.file.internal.FileRenderer.readAttributes;
import static org.ratpackframework.file.internal.FileRenderer.sendFile;

public class AssetHandler implements Handler {

  private ImmutableList<String> indexFiles;

  public AssetHandler(IndexFiles indexFiles) {
    this.indexFiles = ImmutableList.<String>builder()
      .addAll(indexFiles.getFileNames())
      .build();
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
      throw new RuntimeException(e);
    }

    // Convert file separators.
    path = path.replace('/', File.separatorChar);

    if (path.contains(File.separator + '.') || path.contains('.' + File.separator) || path.startsWith(".") || path.endsWith(".")) {
      context.clientError(404);
    } else {
      servePath(context, context.file(path));
    }
  }

  private void servePath(final Context context, final File file) {
    readAttributes(context.getBlocking(), file, new Action<BasicFileAttributes>() {
      public void execute(BasicFileAttributes attributes) {
        if (attributes == null) {
          context.clientError(404);
        } else if (attributes.isRegularFile()) {
          sendFile(context, file, attributes);
        } else if (attributes.isDirectory()) {
          maybeSendFile(context, file, 0);
        } else {
          context.clientError(404);
        }
      }
    });
  }

  private void maybeSendFile(final Context context, final File file, final int i) {
    // If this Handler has been created without any index files then load any globally
    // defined ones instead.
    if (indexFiles.isEmpty()) {
      loadGlobalIndexFiles(context);
    }

    if (i == indexFiles.size()) {
      context.clientError(403);
    } else {
      String name = indexFiles.get(i);
      final File indexFile = new File(file, name);
      readAttributes(context.getBlocking(), indexFile, new Action<BasicFileAttributes>() {
        public void execute(BasicFileAttributes attributes) {
          if (attributes != null && attributes.isRegularFile()) {
            sendFile(context, indexFile, attributes);
          } else {
            maybeSendFile(context, file, i + 1);
          }
        }
      });
    }
  }

  private void loadGlobalIndexFiles(final Context context) {
    indexFiles = ImmutableList.<String>builder()
      .addAll(context.get(IndexFiles.class).getFileNames())
      .build();
  }

}
