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

package ratpack.session.store.internal;

import com.google.common.cache.Cache;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import ratpack.exec.ExecControl;
import ratpack.exec.Promise;
import ratpack.session.internal.SessionId;
import ratpack.session.store.SessionStoreAdapter;

public class LocalMemorySessionStoreAdapter implements SessionStoreAdapter {

  private final ExecControl execControl;
  private final Cache<String, ByteBuf> cache;

  public LocalMemorySessionStoreAdapter(Cache<String, ByteBuf> cache, ExecControl execControl) {
    this.cache = cache;
    this.execControl = execControl;
  }

  @Override
  public Promise<Boolean> store(SessionId sessionId, ByteBufAllocator bufferAllocator, ByteBuf sessionData) {
    ByteBuf oldValue = cache.asMap().put(sessionId.getValue(), Unpooled.unmodifiableBuffer(sessionData.copy()));
    if (oldValue != null) {
      oldValue.release();
      return execControl.promiseOf(true);
    } else {
      return execControl.promiseOf(false);
    }
  }

  @Override
  public Promise<ByteBuf> load(SessionId sessionId, ByteBufAllocator bufferAllocator) {
    ByteBuf value = cache.getIfPresent(sessionId.getValue());
    if (value != null) {
      return execControl.promiseOf(Unpooled.unreleasableBuffer(value));
    } else {
      return execControl.promiseOf(Unpooled.buffer(0, 0));
    }
  }

  @Override
  public long size() {
    return cache.size();
  }

  @Override
  public Promise<Boolean> remove(SessionId sessionId) {
    ByteBuf oldValue = cache.asMap().remove(sessionId.getValue());
    if (oldValue != null) {
      oldValue.release();
      return execControl.promiseOf(true);
    } else {
      return execControl.promiseOf(false);
    }
  }
}
