/*
 * Copyright 2016 the original author or authors.
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

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import ratpack.exec.ExecSpec;
import ratpack.exec.Execution;
import ratpack.func.Action;

public class ForkingSubscription<T> implements Subscription {

  private final Action<? super ExecSpec> execConfig;
  private final Publisher<T> publisher;
  private final BufferedWriteStream<T> write;

  private volatile boolean started;
  private volatile Subscription upstream;
  private volatile boolean cancelled;

  public ForkingSubscription(Action<? super ExecSpec> execConfig, Publisher<T> publisher, BufferedWriteStream<T> write) {
    this.execConfig = execConfig;
    this.publisher = publisher;
    this.write = write;
  }

  @Override
  public void request(long n) {
    if (!started) {
      started = true;
      try {
        execConfig.with(Execution.fork())
          .start(e ->
            publisher.subscribe(new Subscriber<T>() {
              @Override
              public void onSubscribe(Subscription s) {
                upstream = s;
                if (cancelled) {
                  upstream.cancel();
                } else {
                  s.request(Long.MAX_VALUE);
                }
              }

              @Override
              public void onNext(T t) {
                write.item(t);
              }

              @Override
              public void onError(Throwable t) {
                write.error(t);
              }

              @Override
              public void onComplete() {
                write.complete();
              }
            })
          );
      } catch (Exception e) {
        write.error(e);
      }
    }
  }

  @Override
  public void cancel() {
    cancelled = true;
    if (upstream != null) {
      upstream.cancel();
    }
  }
}
