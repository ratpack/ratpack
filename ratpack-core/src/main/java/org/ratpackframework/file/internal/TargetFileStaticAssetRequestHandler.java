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

import org.ratpackframework.file.FileSystemBinding;
import org.ratpackframework.http.Request;
import org.ratpackframework.path.PathBinding;
import org.ratpackframework.handling.Exchange;
import org.ratpackframework.handling.Handler;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

public class TargetFileStaticAssetRequestHandler implements Handler {

  private final Handler delegate;

  public TargetFileStaticAssetRequestHandler(Handler delegate) {
    this.delegate = delegate;
  }

  public void handle(Exchange exchange) {
    FileSystemBinding fileSystemBinding = exchange.get(FileSystemBinding.class);

    Request request = exchange.getRequest();

    String path = request.getPath();
    PathBinding pathBinding = exchange.maybeGet(PathBinding.class);
    if (pathBinding != null) {
      path = pathBinding.getPastBinding();
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

    FileSystemBinding childContext = fileSystemBinding.binding(path);
    exchange.nextWithContext(childContext, delegate);
  }
}
