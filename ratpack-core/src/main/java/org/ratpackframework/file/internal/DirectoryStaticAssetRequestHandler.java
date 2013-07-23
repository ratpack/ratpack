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
import org.ratpackframework.file.FileSystemBinding;
import org.ratpackframework.handling.Exchange;
import org.ratpackframework.handling.Handler;

import java.io.File;
import java.util.List;

import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;

public class DirectoryStaticAssetRequestHandler implements Handler {

  private final List<String> indexFiles;
  private final Handler delegate;
  private final List<Handler> delegateList;

  public DirectoryStaticAssetRequestHandler(List<String> indexFiles, Handler delegate) {
    this.delegate = delegate;
    this.delegateList = ImmutableList.of(delegate);
    this.indexFiles = indexFiles;
  }

  public void handle(Exchange exchange) {
    FileSystemBinding fileSystemBinding = exchange.get(FileSystemBinding.class);
    File targetFile = fileSystemBinding.getFile();
    if (targetFile.isDirectory()) {
      for (String indexFileName : indexFiles) {
        File file = new File(targetFile, indexFileName);
        if (file.isFile()) {
          exchange.insert(FileSystemBinding.class, fileSystemBinding.binding(file), delegateList);
          return;
        }
      }
      exchange.getResponse().status(FORBIDDEN.code(), FORBIDDEN.reasonPhrase()).send();
    } else {
      delegate.handle(exchange);
    }
  }
}
