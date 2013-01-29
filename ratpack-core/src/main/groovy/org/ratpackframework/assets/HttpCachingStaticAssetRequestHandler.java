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

import org.ratpackframework.util.HttpDateParseException;
import org.ratpackframework.util.HttpDateUtil;
import org.vertx.java.core.Handler;
import org.vertx.java.core.file.FileProps;

import java.util.Date;

public class HttpCachingStaticAssetRequestHandler implements Handler<StaticAssetRequest> {

  private final Handler<StaticAssetRequest> next;

  public HttpCachingStaticAssetRequestHandler(Handler<StaticAssetRequest> next) {
    this.next = next;
  }

  @Override
  public void handle(final StaticAssetRequest assetRequest) {
    assetRequest.exists(new Handler<Boolean>() {
      @Override
      public void handle(Boolean exists) {
        if (!exists) {
          next.handle(assetRequest);
          return;
        }

        assetRequest.props(new Handler<FileProps>() {
          @Override
          public void handle(FileProps props) {
            String ifModifiedSince = assetRequest.getRequest().headers().get("if-modified-since");
            if (ifModifiedSince != null) {
              try {
                Date ifModifiedSinceDate = HttpDateUtil.parseDate(ifModifiedSince);
                if (props.lastModifiedTime.before(ifModifiedSinceDate) || props.lastModifiedTime.equals(ifModifiedSinceDate)) {
                  assetRequest.getRequest().response.statusCode = 304;
                  assetRequest.getRequest().response.end();
                  return;
                }
              } catch (HttpDateParseException ignore) {
                // can't parse header, just keep going
              }
            }

            final String ifNoneMatch = assetRequest.getRequest().headers().get("if-none-match");
            if (ifNoneMatch != null && ifNoneMatch.trim().equals("*")) {
              assetRequest.getRequest().response.statusCode = 304;
              assetRequest.getRequest().response.end();
              return;
            }

            next.handle(assetRequest);
          }
        });
      }
    });
  }

}
