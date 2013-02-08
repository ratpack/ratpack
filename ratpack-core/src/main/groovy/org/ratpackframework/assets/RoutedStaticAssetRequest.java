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

import org.ratpackframework.Handler;
import org.ratpackframework.handler.HttpExchange;
import org.ratpackframework.routing.RoutedRequest;

import java.io.File;

public class RoutedStaticAssetRequest extends RoutedRequest {

  private File targetFile;

  public RoutedStaticAssetRequest(File targetFile, HttpExchange exchange, final Handler<? super RoutedStaticAssetRequest> next) {
    super(exchange, new Handler<RoutedRequest>() {
      @Override
      public void handle(RoutedRequest event) {
        next.handle((RoutedStaticAssetRequest) event);
      }
    });
    this.targetFile = targetFile;
  }

  public File getTargetFile() {
    return targetFile;
  }

  public void setTargetFile(File targetFile) {
    this.targetFile = targetFile;
  }

}
