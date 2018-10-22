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

package ratpack.exec.util.internal;

import com.google.common.collect.Lists;
import org.reactivestreams.Subscription;
import ratpack.exec.ExecResult;
import ratpack.exec.Execution;
import ratpack.exec.Operation;
import ratpack.exec.Promise;
import ratpack.exec.util.ParallelBatch;
import ratpack.func.Action;
import ratpack.func.BiAction;
import ratpack.stream.TransformablePublisher;
import ratpack.stream.internal.BufferingPublisher;
import ratpack.util.Types;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class DefaultParallelBatch<T> implements ParallelBatch<T> {

  private final Iterable<? extends Promise<T>> promises;
  private final Action<? super Execution> execInit;

  public DefaultParallelBatch(Iterable<? extends Promise<? extends T>> promises, Action<? super Execution> execInit) {
    this.promises = Types.cast(promises);
    this.execInit = execInit;
  }

  @Override
  public ParallelBatch<T> execInit(Action<? super Execution> execInit) {
    return new DefaultParallelBatch<>(promises, execInit);
  }

  @Override
  public Promise<List<? extends ExecResult<T>>> yieldAll() {
    List<Promise<T>> promises = Lists.newArrayList(this.promises);
    if (promises.isEmpty()) {
      return Promise.value(Collections.emptyList());
    }

    List<ExecResult<T>> results = Types.cast(promises);
    AtomicInteger counter = new AtomicInteger(promises.size());

    return Promise.async(d -> {
      for (int i = 0; i < promises.size(); ++i) {
        final int finalI = i;
        //noinspection CodeBlock2Expr
        Execution.fork()
          .onStart(execInit)
          .onComplete(e -> {
            if (counter.decrementAndGet() == 0) {
              d.success(results);
            }
          })
          .start(e ->
            promises.get(finalI).result(t -> {
              results.set(finalI, t);
            })
          );
      }
    });

  }

  @Override
  public Promise<List<T>> yield() {
    List<Promise<T>> promises = Lists.newArrayList(this.promises);
    if (promises.isEmpty()) {
      return Promise.value(Collections.emptyList());
    }

    List<T> results = Types.cast(promises);
    return Promise.async(d -> forEach(results::set).onError(d::error).then(() -> d.success(results)));
  }

  @Override
  public Operation forEach(BiAction<? super Integer, ? super T> consumer) {
    AtomicReference<Throwable> error = new AtomicReference<>();
    AtomicBoolean done = new AtomicBoolean();
    AtomicInteger wip = new AtomicInteger();

    return Promise.async(d -> {
      int i = 0;
      Iterator<? extends Promise<T>> iterator = promises.iterator();
      while (iterator.hasNext()) {
        Promise<T> promise = iterator.next();
        final int finalI = i++;
        wip.incrementAndGet();
        if (!iterator.hasNext()) {
          done.set(true);
        }

        Execution.fork()
          .onStart(execInit)
          .onComplete(e -> {
            if (wip.decrementAndGet() == 0 && done.get()) {
              Throwable t = error.get();
              if (t == null) {
                d.success(null);
              } else {
                d.error(t);
              }
            }
          })
          .start(e -> {
            //noinspection ThrowableResultOfMethodCallIgnored
            if (error.get() == null) {
              promise.result(t -> {
                if (t.isError()) {
                  Throwable thisError = t.getThrowable();
                  if (!error.compareAndSet(null, thisError)) {
                    //noinspection ThrowableResultOfMethodCallIgnored
                    Throwable firstError = error.get();
                    if (firstError != thisError) {
                      firstError.addSuppressed(thisError);
                    }
                  }
                } else {
                  consumer.execute(finalI, t.getValue());
                }
              });
            }
          });
      }
      if (i == 0) {
        d.success(null);
      }
    }).operation();
  }

  @Override
  public TransformablePublisher<T> publisher() {
    Iterator<? extends Promise<T>> iterator = promises.iterator();
    return new BufferingPublisher<>(Action.noop(), write -> {
      return new Subscription() {
        volatile boolean cancelled;
        volatile boolean complete;
        final AtomicLong finished = new AtomicLong();
        volatile long started;

        @Override
        public void request(long n) {
          while (n-- > 0 && !cancelled) {
            if (iterator.hasNext()) {
              ++started;
              Promise<T> promise = iterator.next();
              if (!iterator.hasNext()) {
                complete = true;
              }
              Execution.fork()
                .onStart(execInit)
                .onComplete(e -> {
                  long finished = this.finished.incrementAndGet();
                  if (finished == started && complete && !cancelled) {
                    write.complete();
                  }
                })
                .start(e -> promise.onError(write::error).then(write::item));
            } else {
              if (started == 0) {
                write.complete();
              }
              return;
            }
          }
        }

        @Override
        public void cancel() {
          cancelled = true;
        }
      };
    });
  }

}
