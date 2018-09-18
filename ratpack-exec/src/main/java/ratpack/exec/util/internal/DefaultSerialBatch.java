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
import ratpack.exec.ExecResult;
import ratpack.exec.Operation;
import ratpack.exec.Promise;
import ratpack.exec.util.SerialBatch;
import ratpack.func.BiAction;
import ratpack.func.BiFunction;
import ratpack.stream.Streams;
import ratpack.stream.TransformablePublisher;
import ratpack.util.Types;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DefaultSerialBatch<T> implements SerialBatch<T> {

  private final Iterable<? extends Promise<T>> promises;

  public DefaultSerialBatch(Iterable<? extends Promise<? extends T>> promises) {
    this.promises = Types.cast(promises);
  }

  @Override
  public Promise<List<? extends ExecResult<T>>> yieldAll() {
    List<Promise<T>> promises = Lists.newArrayList(this.promises);
    List<ExecResult<T>> results = Types.cast(promises);
    return Promise.async(d ->
      yieldPromise(promises.iterator(), 0,
        results::set,
        (i, r) -> {
          results.set(i, r);
          return true;
        },
        () -> d.success(results)
      )
    );
  }

  @Override
  public Promise<List<T>> yield() {
    List<T> results = new ArrayList<>();
    return Promise.async(d ->
      forEach((i, r) -> results.add(r))
        .onError(d::error)
        .then(() -> d.success(results))
    );
  }

  @Override
  public Operation forEach(BiAction<? super Integer, ? super T> consumer) {
    return Promise.<Void>async(d ->
      yieldPromise(promises.iterator(), 0, (i, r) -> consumer.execute(i, r.getValue()), (i, r) -> {
        d.error(r.getThrowable());
        return false;
      }, () -> d.success(null))
    ).operation();
  }

  @Override
  public TransformablePublisher<T> publisher() {
    Iterator<? extends Promise<T>> iterator = promises.iterator();
    return Streams.flatYield(r -> {
      if (iterator.hasNext()) {
        return iterator.next();
      } else {
        return Promise.ofNull();
      }
    });
  }

  private static <T> void yieldPromise(Iterator<? extends Promise<T>> promises, int i, BiAction<Integer, ExecResult<T>> withItem, BiFunction<Integer, ExecResult<T>, Boolean> onError, Runnable onComplete) {
    if (promises.hasNext()) {
      promises.next().result(r -> {
        if (r.isError()) {
          if (!onError.apply(i, r)) {
            return;
          }
        } else {
          withItem.execute(i, r);
        }
        yieldPromise(promises, i + 1, withItem, onError, onComplete);
      });
    } else {
      onComplete.run();
    }
  }

}
