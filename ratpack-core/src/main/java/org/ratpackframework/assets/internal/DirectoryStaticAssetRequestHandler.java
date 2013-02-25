/*
 * Copyright 2012 the original author or authors.
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

package org.ratpackframework.assets.internal;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.ratpackframework.handler.Handler;
import org.ratpackframework.http.HttpExchange;
import org.ratpackframework.routing.Routed;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DirectoryStaticAssetRequestHandler implements Handler<Routed<HttpExchange>> {

  private final List<String> indexFiles;

  @Inject
  public DirectoryStaticAssetRequestHandler(@Named("indexFiles") List<String> indexFiles) {
    this.indexFiles = new ArrayList<>(indexFiles);
  }

  @Override
  public void handle(final Routed<HttpExchange> routed) {
    HttpExchange exchange = routed.get();
    File targetFile = exchange.getTargetFile();
    if (targetFile.isDirectory()) {
      for (String indexFileName : indexFiles) {
        File file = new File(targetFile, indexFileName);
        if (file.isFile()) {
          exchange.setTargetFile(file);
          routed.next();
          return;
        }
      }
      exchange.end(HttpResponseStatus.FORBIDDEN);
    } else {
      routed.next();
    }

  }
}
