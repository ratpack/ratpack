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

import org.vertx.java.core.Handler;
import org.vertx.java.core.file.FileProps;

public class DirectoryStaticAssetRequestHandler implements Handler<StaticAssetRequest> {

  private final Handler<StaticAssetRequest> next;

  public DirectoryStaticAssetRequestHandler(Handler<StaticAssetRequest> next) {
    this.next = next;
  }

  @Override
  public void handle(final StaticAssetRequest assetRequest) {
    assetRequest.exists(new Handler<Boolean>() {
      @Override
      public void handle(Boolean exists) {
        if (exists) {
          assetRequest.props(new Handler<FileProps>() {
            @Override
            public void handle(FileProps props) {
              if (props.isDirectory) {
                assetRequest.getRequest().response.statusCode = 403;
                assetRequest.getRequest().response.end();
              } else {
                next.handle(assetRequest);
              }
            }
          });
        } else {
          next.handle(assetRequest);
        }
      }
    });
  }
}
