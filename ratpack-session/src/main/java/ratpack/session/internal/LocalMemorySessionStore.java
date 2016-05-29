/*
 * Copyright 2015 the original author or authors.
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

package ratpack.session.internal;

import com.google.common.cache.Cache;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.AsciiString;
import ratpack.exec.Operation;
import ratpack.exec.Promise;
import ratpack.session.SessionStore;

import java.util.concurrent.atomic.AtomicLong;

public class LocalMemorySessionStore implements SessionStore {

  private final Cache<AsciiString, ByteBuf> cache;
  private final AtomicLong lastCleanup = new AtomicLong(System.currentTimeMillis());

  public LocalMemorySessionStore(Cache<AsciiString, ByteBuf> cache) {
    this.cache = cache;
  }

  @Override
  public Operation store(AsciiString sessionId, ByteBuf sessionData) {
    return Operation.of(() -> {
      maybeCleanup();
      ByteBuf retained = sessionData.retain().asReadOnly();
      cache.put(sessionId, retained);
    });
  }

  @Override
  public Promise<ByteBuf> load(AsciiString sessionId) {
    return Promise.sync(() -> {
      maybeCleanup();
      ByteBuf value = cache.getIfPresent(sessionId);
      if (value != null) {
        return Unpooled.unreleasableBuffer(value.slice());
      } else {
        return Unpooled.EMPTY_BUFFER;
      }
    });
  }

  @Override
  public Promise<Long> size() {
    return Promise.sync(cache::size);
  }

  @Override
  public Operation remove(AsciiString sessionId) {
    return Operation.of(() -> {
      maybeCleanup();
      cache.invalidate(sessionId);
    });
  }

  @Override
  public void onStop(@SuppressWarnings("deprecation") ratpack.server.StopEvent event) throws Exception {
    cache.invalidateAll();
  }

  private void maybeCleanup() {
    long now = System.currentTimeMillis();
    long last = lastCleanup.get();
    if (now - last > 1000 * 10) {
      if (lastCleanup.compareAndSet(last, now)) {
        cache.cleanUp();
      }
    }
  }
}
