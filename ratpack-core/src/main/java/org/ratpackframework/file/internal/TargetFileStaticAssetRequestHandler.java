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

import org.ratpackframework.file.FileSystemContext;
import org.ratpackframework.http.Request;
import org.ratpackframework.path.PathContext;
import org.ratpackframework.routing.Exchange;
import org.ratpackframework.routing.Handler;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

public class TargetFileStaticAssetRequestHandler implements Handler {

  private final Handler delegate;

  public TargetFileStaticAssetRequestHandler(Handler delegate) {
    this.delegate = delegate;
  }

  @Override
  public void handle(Exchange exchange) {
    FileSystemContext fileSystemContext = exchange.get(FileSystemContext.class);

    Request request = exchange.getRequest();

    String path = request.getPath();
    PathContext pathContext = exchange.maybeGet(PathContext.class);
    if (pathContext != null) {
      path = pathContext.getPastBinding();
    }

    // Decode the pathRoutes.
    try {
      path = new URI(path).getPath();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }

    // Convert file separators.
    path = path.replace('/', File.separatorChar);

    if (
        path.contains(File.separator + '.')
            || path.contains('.' + File.separator)
            || path.startsWith(".") || path.endsWith(".")
        ) {
      return;
    }

    FileSystemContext childContext = fileSystemContext.context(path);
    exchange.nextWithContext(childContext, delegate);
  }
}
