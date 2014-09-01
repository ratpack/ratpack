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

package ratpack.sse.internal;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http.HttpHeaders;
import ratpack.func.Function;
import ratpack.handling.Context;
import ratpack.http.Response;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.render.Renderer;
import ratpack.render.RendererSupport;
import ratpack.sse.ServerSentEvent;
import ratpack.sse.ServerSentEvents;
import ratpack.stream.Streams;
import ratpack.util.internal.ByteBufWriteThroughOutputStream;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import static io.netty.util.CharsetUtil.UTF_8;

public class ServerSentEventsRenderer extends RendererSupport<ServerSentEvents> {

  public static final TypeToken<Renderer<ServerSentEvents>> TYPE = new TypeToken<Renderer<ServerSentEvents>>() {};

  private static final byte[] EVENT_TYPE_PREFIX = "event: ".getBytes(UTF_8);
  private static final byte[] EVENT_DATA_PREFIX = "data: ".getBytes(UTF_8);
  private static final byte[] EVENT_ID_PREFIX = "id: ".getBytes(UTF_8);
  private static final byte[] NEWLINE = "\n".getBytes(UTF_8);

  private static final CharSequence TEXT_EVENT_STREAM_CHARSET_UTF_8 = HttpHeaders.newEntity("text/event-stream;charset=UTF-8");

  private final Encoder encoder;

  public ServerSentEventsRenderer(ByteBufAllocator bufferAllocator) {
    this.encoder = new Encoder(bufferAllocator);
  }

  @Override
  public void render(Context context, ServerSentEvents object) throws Exception {
    Response response = context.getResponse();
    response.getHeaders().add(HttpHeaderConstants.CONTENT_TYPE, TEXT_EVENT_STREAM_CHARSET_UTF_8);
    response.getHeaders().add(HttpHeaderConstants.CACHE_CONTROL, HttpHeaderConstants.NO_CACHE_FULL);
    response.getHeaders().add(HttpHeaderConstants.PRAGMA, HttpHeaderConstants.NO_CACHE);
    response.sendStream(context, Streams.map(object.getPublisher(), encoder));
  }

  public static class Encoder implements Function<ServerSentEvent, ByteBuf> {
    private final ByteBufAllocator bufferAllocator;

    public Encoder(ByteBufAllocator bufferAllocator) {
      this.bufferAllocator = bufferAllocator;
    }

    @Override
    public ByteBuf apply(ServerSentEvent serverSentEvent) throws Exception {
      ByteBuf buffer = bufferAllocator.buffer();

      OutputStream outputStream = new ByteBufWriteThroughOutputStream(buffer);
      Writer writer = new OutputStreamWriter(outputStream, UTF_8);

      String eventType = serverSentEvent.getEventType();
      if (eventType != null) {
        outputStream.write(EVENT_TYPE_PREFIX);
        writer.append(eventType).flush();
        outputStream.write(NEWLINE);
      }

      String eventData = serverSentEvent.getEventData();
      if (eventData != null) {
        outputStream.write(EVENT_DATA_PREFIX);
        for (Character character : Lists.charactersOf(eventData)) {
          if (character == '\n') {
            outputStream.write(NEWLINE);
            outputStream.write(EVENT_DATA_PREFIX);
          } else {
            writer.append(character).flush();
          }
        }
        outputStream.write(NEWLINE);
      }


      String eventId = serverSentEvent.getEventId();
      if (eventId != null) {
        outputStream.write(EVENT_ID_PREFIX);
        writer.append(eventId).flush();
        outputStream.write(NEWLINE);
      }

      outputStream.write(NEWLINE);
      writer.flush();
      writer.close();
      return buffer;
    }
  }
}
