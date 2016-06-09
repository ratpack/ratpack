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

package ratpack.exec.internal;

import io.netty.util.internal.PlatformDependent;
import ratpack.exec.*;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class CachingUpstream<T> implements Upstream<T> {

  private Upstream<? extends T> upstream;
  private final AtomicBoolean fired = new AtomicBoolean();
  private final Queue<Downstream<? super T>> waiting = PlatformDependent.newMpscQueue();
  private final AtomicBoolean draining = new AtomicBoolean();
  private final AtomicReference<ExecResult<? extends T>> result = new AtomicReference<>();

  public CachingUpstream(Upstream<? extends T> upstream) {
    this.upstream = upstream;
  }

  private void tryDrain() {
    if (draining.compareAndSet(false, true)) {
      try {
        ExecResult<? extends T> result = this.result.get();
        Downstream<? super T> downstream = waiting.poll();
        while (downstream != null) {
          downstream.accept(result);
          downstream = waiting.poll();
        }
      } finally {
        draining.set(false);
      }
    }
    if (!draining.get() && !waiting.isEmpty()) {
      tryDrain();
    }
  }

  @Override
  public void connect(Downstream<? super T> downstream) throws Exception {
    if (fired.compareAndSet(false, true)) {
      upstream.connect(new Downstream<T>() {
        @Override
        public void error(Throwable throwable) {
          receiveResult(ExecResult.of(Result.<T>error(throwable)));
          downstream.error(throwable);
        }

        @Override
        public void success(T value) {
          receiveResult(ExecResult.of(Result.success(value)));
          downstream.success(value);
        }

        @Override
        public void complete() {
          receiveResult(CompleteExecResult.get());
          downstream.complete();
        }
      });
    } else {
      ExecResult<? extends T> result = this.result.get();
      if (result == null) {
        Promise.<T>async(waiting::add).result(downstream::accept);
      } else {
        downstream.accept(result);
      }
    }
  }

  private void receiveResult(ExecResult<T> newValue) {
    result.set(newValue);
    this.upstream = null; // release
    DefaultExecution.require().getEventLoop().execute(this::tryDrain);
  }

}

