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

package ratpack.file.internal;

import io.netty.buffer.ByteBuf;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import ratpack.exec.Downstream;
import ratpack.exec.Promise;

import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;

public class FileWritingSubscriber implements Subscriber<ByteBuf> {

  private final AsynchronousFileChannel out;
  private final Downstream<? super Long> downstream;

  private final long startAt;
  private long position;
  private Subscription s;

  private boolean cancelled;

  public FileWritingSubscriber(AsynchronousFileChannel out, long position, Downstream<? super Long> downstream) {
    this.position = position;
    this.startAt = position;
    this.downstream = downstream;
    this.out = out;

    if (position < 0) {
      throw new IllegalArgumentException("position must be >= 0");
    }
  }

  @Override
  public void onSubscribe(Subscription s) {
    this.s = s;
    s.request(1);
  }

  @Override
  public void onNext(ByteBuf byteBuf) {
    int toWrite = byteBuf.readableBytes();
    if (cancelled || toWrite < 1) {
      byteBuf.release();
      if (!cancelled) {
        s.request(1);
      }
      return;
    }

    Promise.<Integer>async(down ->
      out.write(byteBuf.nioBuffer(), position, null, new CompletionHandler<Integer, Void>() {
        @Override
        public void completed(Integer result, Void attachment) {
          byteBuf.release();
          down.success(result);
        }

        @Override
        public void failed(Throwable exc, Void attachment) {
          byteBuf.release();
          down.error(exc);
        }
      })
    )
      .onError(e -> {
        cancelled = true;
        s.cancel();
        downstream.error(e);
      })
      .then(i -> {
        position += i;
        s.request(1);
      });
  }

  @Override
  public void onError(Throwable t) {
    if (!cancelled) {
      downstream.error(t);
    }
  }

  @Override
  public void onComplete() {
    downstream.success(position - startAt);
  }
}
