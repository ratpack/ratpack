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

import com.google.common.reflect.TypeToken;
import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import ratpack.handling.Context;
import ratpack.http.Response;
import ratpack.http.ResponseChunks;
import ratpack.render.Renderer;
import ratpack.render.RendererSupport;

public class HttpResponseChunksRenderer extends RendererSupport<ResponseChunks> {

  public static final TypeToken<Renderer<ResponseChunks>> TYPE = new TypeToken<Renderer<ResponseChunks>>() {};

  @Override
  public void render(Context context, ResponseChunks chunks) throws Exception {
    Response response = context.getResponse();
    response.getHeaders().add(HttpHeaderConstants.TRANSFER_ENCODING, HttpHeaderConstants.CHUNKED);
    response.getHeaders().set(HttpHeaderConstants.CONTENT_TYPE, chunks.getContentType());
    Publisher<? extends ByteBuf> publisher = chunks.publisher(context.getLaunchConfig().getBufferAllocator());
    response.sendStream(context, publisher);
  }

}
