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

package ratpack.stream.internal;

import ratpack.handling.Context;
import ratpack.http.Response;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.render.RendererSupport;
import ratpack.stream.ServerSentEvents;
import ratpack.stream.ServerSentEventsRenderer;

public class DefaultServerSentEventsRenderer extends RendererSupport<ServerSentEvents> implements ServerSentEventsRenderer {

  @Override
  public void render(Context context, ServerSentEvents object) throws Exception {
    Response response = context.getResponse();
    response.getHeaders().add(HttpHeaderConstants.CONTENT_TYPE, "text/event-stream;charset=UTF-8");
    response.getHeaders().add(HttpHeaderConstants.CACHE_CONTROL,
      ratpack.http.internal.HttpHeaderConstants.NO_CACHE + ", "
        + HttpHeaderConstants.NO_STORE + ", "
        + HttpHeaderConstants.MAX_AGE + "=0, "
        + HttpHeaderConstants.MUST_REVALIDATE
    );
    response.getHeaders().add(HttpHeaderConstants.PRAGMA, HttpHeaderConstants.NO_CACHE);

    response.sendStream(context, object.getPublisher());
  }
}
