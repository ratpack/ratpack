/*
 * Copyright 2014 the original author or authors.
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

package ratpack.http.internal;

import io.netty.handler.codec.http.HttpHeaderNames;
import ratpack.handling.Context;
import ratpack.handling.Handler;

import java.util.Arrays;
import java.util.List;

public class AcceptsHandler implements Handler {

  private final List<String> contentTypes;

  public AcceptsHandler(String... contentTypes) {
    this.contentTypes = Arrays.asList(contentTypes);
  }

  @Override
  public void handle(Context context) throws Exception {
    String acceptHeader = context.getRequest().getHeaders().get(HttpHeaderNames.ACCEPT);

    if (acceptHeader == null || acceptHeader.isEmpty()) {
      context.clientError(406);
    } else {
      String winner = MimeParse.bestMatch(contentTypes, acceptHeader);
      if (winner == null || winner.isEmpty()) {
        context.clientError(406);
      } else {
        context.next();
      }
    }
  }
}
