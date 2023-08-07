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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.internal.ObjectPool;
import ratpack.bytebuf.ByteBufRef;
import ratpack.sse.ServerSentEvent;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class ServerSentEventEncoder {

  public static final ServerSentEventEncoder INSTANCE = new ServerSentEventEncoder();

  private static final ObjectPool<ByteBuf> EVENT_PREFIX_POOL = pooledConstant("event: ");

  private static final ObjectPool<ByteBuf> DATA_PREFIX_POOL = pooledConstant("data: ");

  private static final ObjectPool<ByteBuf> ID_PREFIX_POOL = pooledConstant("id: ");

  private static final ObjectPool<ByteBuf> COMMENT_PREFIX_POOL = pooledConstant(": ");


  private static final ByteBuf NEWLINE = DefaultServerSentEvent.NEWLINE_BYTE_BUF;

  private static final ObjectPool<ByteBuf> NEWLINE_POOL = pooledConstant(NEWLINE);


  public ByteBuf encode(ServerSentEvent sse) throws Exception {

    List<ByteBuf> comment = sse.getComment();
    ByteBuf id = sse.getId();
    ByteBuf event = sse.getEvent();
    List<ByteBuf> data = sse.getData();

    int count = 0;
    if (!comment.isEmpty()) {
      count += comment.size() * 3;
    }
    if (id.isReadable()) {
      count += 3;
    }
    if (event.isReadable()) {
      count += 3;
    }
    if (!data.isEmpty()) {
      count += data.size() * 3;
    }

    if (count == 0) {
      return Unpooled.EMPTY_BUFFER;
    } else {
      ByteBuf[] bufs = new ByteBuf[count + 1];
      int i = 0;

      for (int j = 0; j < comment.size(); ++j) {
        bufs[i++] = COMMENT_PREFIX_POOL.get();
        bufs[i++] = comment.get(j);
        bufs[i++] = NEWLINE_POOL.get();
      }

      if (id.isReadable()) {
        bufs[i++] = ID_PREFIX_POOL.get();
        bufs[i++] = id;
        bufs[i++] = NEWLINE_POOL.get();
      } else {
        id.release();
      }

      if (event.isReadable()) {
        bufs[i++] = EVENT_PREFIX_POOL.get();
        bufs[i++] = event;
        bufs[i++] = NEWLINE_POOL.get();
      } else {
        event.release();
      }

      for (int j = 0; j < data.size(); ++j) {
        bufs[i++] = DATA_PREFIX_POOL.get();
        bufs[i++] = data.get(j);
        bufs[i++] = NEWLINE_POOL.get();
      }

      bufs[i] = NEWLINE_POOL.get();

      return Unpooled.wrappedUnmodifiableBuffer(bufs);
    }
  }

  private static ObjectPool<ByteBuf> pooledConstant(String string) {
    return pooledConstant(string.getBytes(StandardCharsets.UTF_8));
  }

  private static ObjectPool<ByteBuf> pooledConstant(byte[] bytes) {
    return pooledConstant(Unpooled.wrappedBuffer(bytes));
  }

  private static ObjectPool<ByteBuf> pooledConstant(ByteBuf byteBuf) {
    return ObjectPool.newPool(new ObjectPool.ObjectCreator<ByteBuf>() {
      @Override
      public ByteBuf newObject(ObjectPool.Handle<ByteBuf> handle) {
        return new ByteBufRef(byteBuf.asReadOnly()) {
          @Override
          protected void deallocate() {
            delegate.resetReaderIndex();
            counted.reset();
            handle.recycle(this);
          }
        };
      }
    });
  }
}
