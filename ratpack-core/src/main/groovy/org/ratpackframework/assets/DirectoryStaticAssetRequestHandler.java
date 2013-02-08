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

package org.ratpackframework.assets;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.ratpackframework.Handler;

import java.io.File;

public class DirectoryStaticAssetRequestHandler implements Handler<RoutedStaticAssetRequest> {

  @Override
  public void handle(final RoutedStaticAssetRequest assetRequest) {
    File targetFile = assetRequest.getTargetFile();
    if (!targetFile.exists() || !targetFile.isDirectory()) {
      assetRequest.next();
      return;
    }

    assetRequest.getExchange().end(HttpResponseStatus.FORBIDDEN);
  }
}
