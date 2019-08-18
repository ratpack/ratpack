/*
 * Copyright 2017 the original author or authors.
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

package ratpack.http.client.internal;

import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponse;

class NoContentLengthOnNoBodyHttpObjectAggregator extends HttpObjectAggregator {

  NoContentLengthOnNoBodyHttpObjectAggregator(int maxContentLength) {
    super(maxContentLength);
  }

  @Override
  protected void finishAggregation(FullHttpMessage aggregated) throws Exception {
    if (aggregated.content().readableBytes() == 0 && aggregated instanceof HttpResponse) {
      HttpResponse httpResponse = (HttpResponse) aggregated;
      int status = httpResponse.status().code();
      if (status == 204 || (status >= 100 && status < 200)) {
        httpResponse.headers().remove(HttpHeaderNames.CONTENT_LENGTH);
        return;
      } else if (status == 304) {
        return;
      }
    }

    super.finishAggregation(aggregated);
  }
}
