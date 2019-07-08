/*
 * Copyright 2019 the original author or authors.
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

import ratpack.exec.ExecResult;
import ratpack.exec.Promise;
import ratpack.exec.util.ParallelBatch;
import ratpack.exec.util.PromiseZipper;
import ratpack.func.*;
import ratpack.util.Types;

import java.util.Arrays;
import java.util.List;

public class DefaultPromiseZipper extends AbstractPromiseZipper implements PromiseZipper {

  public DefaultPromiseZipper(List<Promise<?>> promises) {
    super(promises);
  }

  @Override
  public <T1, T2, R> Promise<R> yield(BiFunction<T1, T2, R> function) {
    return yieldAll((ExecResult<T1> r1, ExecResult<T2> r2) -> {
      throwIfError(r1, r2);
      return function.apply(r1.getValue(), r2.getValue());
    }).flatMapError(this::unpack);
  }

  @Override
  public <T1, T2, R> Promise<R> yieldAll(BiFunction<ExecResult<T1>, ExecResult<T2>, R> function) {
    Promise<T1> p1 = Types.cast(promises.get(0));
    Promise<T2> p2 = Types.cast(promises.get(1));
    return ParallelBatch.of(p1, p2).yieldAll().map(results -> {
      ExecResult<T1> r1 = Types.cast(results.get(0));
      ExecResult<T2> r2 = Types.cast(results.get(1));
      return function.apply(r1, r2);
    });
  }

  public static class DefaultPromiseZipper3 extends AbstractPromiseZipper implements PromiseZipper3 {

    public DefaultPromiseZipper3(List<Promise<?>> promises) {
      super(promises);
    }

    @Override
    public <T1, T2, T3, R> Promise<R> yield(Function3<T1, T2, T3, R> function) {
      return yieldAll((ExecResult<T1> r1, ExecResult<T2> r2, ExecResult<T3> r3) -> {
        throwIfError(r1, r2, r3);
        return function.apply(r1.getValue(), r2.getValue(), r3.getValue());
      }).flatMapError(this::unpack);
    }

    @Override
    public <T1, T2, T3, R> Promise<R> yieldAll(Function3<ExecResult<T1>, ExecResult<T2>, ExecResult<T3>, R> function) {
      Promise<T1> p1 = Types.cast(promises.get(0));
      Promise<T2> p2 = Types.cast(promises.get(1));
      Promise<T3> p3 = Types.cast(promises.get(2));
      return ParallelBatch.of(p1, p2, p3).yieldAll().map(results -> {
        ExecResult<T1> r1 = Types.cast(results.get(0));
        ExecResult<T2> r2 = Types.cast(results.get(1));
        ExecResult<T3> r3 = Types.cast(results.get(2));
        return function.apply(r1, r2, r3);
      });
    }
  }

  public static class DefaultPromiseZipper4 extends AbstractPromiseZipper implements PromiseZipper4 {

    public DefaultPromiseZipper4(List<Promise<?>> promises) {
      super(promises);
    }

    @Override
    public <T1, T2, T3, T4, R> Promise<R> yield(Function4<T1, T2, T3, T4, R> function) {
      return yieldAll((ExecResult<T1> r1, ExecResult<T2> r2, ExecResult<T3> r3, ExecResult<T4> r4) -> {
        throwIfError(r1, r2, r3, r4);
        return function.apply(r1.getValue(), r2.getValue(), r3.getValue(), r4.getValue());
      }).flatMapError(this::unpack);
    }

    @Override
    public <T1, T2, T3, T4, R> Promise<R> yieldAll(Function4<ExecResult<T1>, ExecResult<T2>, ExecResult<T3>, ExecResult<T4>, R> function) {
      Promise<T1> p1 = Types.cast(promises.get(0));
      Promise<T2> p2 = Types.cast(promises.get(1));
      Promise<T3> p3 = Types.cast(promises.get(2));
      Promise<T4> p4 = Types.cast(promises.get(3));
      return ParallelBatch.of(p1, p2, p3, p4).yieldAll().map(results -> {
        ExecResult<T1> r1 = Types.cast(results.get(0));
        ExecResult<T2> r2 = Types.cast(results.get(1));
        ExecResult<T3> r3 = Types.cast(results.get(2));
        ExecResult<T4> r4 = Types.cast(results.get(3));
        return function.apply(r1, r2, r3, r4);
      });
    }
  }

  public static class DefaultPromiseZipper5 extends AbstractPromiseZipper implements PromiseZipper5 {

    public DefaultPromiseZipper5(List<Promise<?>> promises) {
      super(promises);
    }

    @Override
    public <T1, T2, T3, T4, T5, R> Promise<R> yield(Function5<T1, T2, T3, T4, T5, R> function) {
      return yieldAll((ExecResult<T1> r1, ExecResult<T2> r2, ExecResult<T3> r3, ExecResult<T4> r4, ExecResult<T5> r5) -> {
        throwIfError(r1, r2, r3, r4, r5);
        return function.apply(r1.getValue(), r2.getValue(), r3.getValue(), r4.getValue(), r5.getValue());
      }).flatMapError(this::unpack);
    }

    @Override
    public <T1, T2, T3, T4, T5, R> Promise<R> yieldAll(Function5<ExecResult<T1>, ExecResult<T2>, ExecResult<T3>, ExecResult<T4>, ExecResult<T5>, R> function) {
      Promise<T1> p1 = Types.cast(promises.get(0));
      Promise<T2> p2 = Types.cast(promises.get(1));
      Promise<T3> p3 = Types.cast(promises.get(2));
      Promise<T4> p4 = Types.cast(promises.get(3));
      Promise<T5> p5 = Types.cast(promises.get(4));
      return ParallelBatch.of(p1, p2, p3, p4, p5).yieldAll().map(results -> {
        ExecResult<T1> r1 = Types.cast(results.get(0));
        ExecResult<T2> r2 = Types.cast(results.get(1));
        ExecResult<T3> r3 = Types.cast(results.get(2));
        ExecResult<T4> r4 = Types.cast(results.get(3));
        ExecResult<T5> r5 = Types.cast(results.get(4));
        return function.apply(r1, r2, r3, r4, r5);
      });
    }
  }

  public static class DefaultPromiseZipper6 extends AbstractPromiseZipper implements PromiseZipper6 {

    public DefaultPromiseZipper6(List<Promise<?>> promises) {
      super(promises);
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, R> Promise<R> yield(Function6<T1, T2, T3, T4, T5, T6, R> function) {
      return yieldAll((ExecResult<T1> r1, ExecResult<T2> r2, ExecResult<T3> r3, ExecResult<T4> r4, ExecResult<T5> r5, ExecResult<T6> r6) -> {
        throwIfError(r1, r2, r3, r4, r5, r6);
        return function.apply(r1.getValue(), r2.getValue(), r3.getValue(), r4.getValue(), r5.getValue(), r6.getValue());
      }).flatMapError(this::unpack);
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, R> Promise<R> yieldAll(Function6<ExecResult<T1>, ExecResult<T2>, ExecResult<T3>, ExecResult<T4>, ExecResult<T5>, ExecResult<T6>, R> function) {
      Promise<T1> p1 = Types.cast(promises.get(0));
      Promise<T2> p2 = Types.cast(promises.get(1));
      Promise<T3> p3 = Types.cast(promises.get(2));
      Promise<T4> p4 = Types.cast(promises.get(3));
      Promise<T5> p5 = Types.cast(promises.get(4));
      Promise<T6> p6 = Types.cast(promises.get(5));
      return ParallelBatch.of(p1, p2, p3, p4, p5, p6).yieldAll().map(results -> {
        ExecResult<T1> r1 = Types.cast(results.get(0));
        ExecResult<T2> r2 = Types.cast(results.get(1));
        ExecResult<T3> r3 = Types.cast(results.get(2));
        ExecResult<T4> r4 = Types.cast(results.get(3));
        ExecResult<T5> r5 = Types.cast(results.get(4));
        ExecResult<T6> r6 = Types.cast(results.get(5));
        return function.apply(r1, r2, r3, r4, r5, r6);
      });
    }
  }

  public static class DefaultPromiseZipper7 extends AbstractPromiseZipper implements PromiseZipper7 {

    public DefaultPromiseZipper7(List<Promise<?>> promises) {
      super(promises);
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, T7, R> Promise<R> yield(Function7<T1, T2, T3, T4, T5, T6, T7, R> function) {
      return yieldAll((ExecResult<T1> r1, ExecResult<T2> r2, ExecResult<T3> r3, ExecResult<T4> r4, ExecResult<T5> r5, ExecResult<T6> r6, ExecResult<T7> r7) -> {
        throwIfError(r1, r2, r3, r4, r5, r6, r7);
        return function.apply(r1.getValue(), r2.getValue(), r3.getValue(), r4.getValue(), r5.getValue(), r6.getValue(), r7.getValue());
      }).flatMapError(this::unpack);
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, T7, R> Promise<R> yieldAll(Function7<ExecResult<T1>, ExecResult<T2>, ExecResult<T3>, ExecResult<T4>, ExecResult<T5>, ExecResult<T6>, ExecResult<T7>, R> function) {
      Promise<T1> p1 = Types.cast(promises.get(0));
      Promise<T2> p2 = Types.cast(promises.get(1));
      Promise<T3> p3 = Types.cast(promises.get(2));
      Promise<T4> p4 = Types.cast(promises.get(3));
      Promise<T5> p5 = Types.cast(promises.get(4));
      Promise<T6> p6 = Types.cast(promises.get(5));
      Promise<T7> p7 = Types.cast(promises.get(6));
      return ParallelBatch.of(p1, p2, p3, p4, p5, p6, p7).yieldAll().map(results -> {
        ExecResult<T1> r1 = Types.cast(results.get(0));
        ExecResult<T2> r2 = Types.cast(results.get(1));
        ExecResult<T3> r3 = Types.cast(results.get(2));
        ExecResult<T4> r4 = Types.cast(results.get(3));
        ExecResult<T5> r5 = Types.cast(results.get(4));
        ExecResult<T6> r6 = Types.cast(results.get(5));
        ExecResult<T7> r7 = Types.cast(results.get(6));
        return function.apply(r1, r2, r3, r4, r5, r6, r7);
      });
    }
  }

  public static class DefaultPromiseZipper8 extends AbstractPromiseZipper implements PromiseZipper8 {

    public DefaultPromiseZipper8(List<Promise<?>> promises) {
      super(promises);
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, T7, T8, R> Promise<R> yield(Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> function) {
      return yieldAll((ExecResult<T1> r1, ExecResult<T2> r2, ExecResult<T3> r3, ExecResult<T4> r4, ExecResult<T5> r5, ExecResult<T6> r6, ExecResult<T7> r7, ExecResult<T8> r8) -> {
        throwIfError(r1, r2, r3, r4, r5, r6, r7, r8);
        return function.apply(r1.getValue(), r2.getValue(), r3.getValue(), r4.getValue(), r5.getValue(), r6.getValue(), r7.getValue(), r8.getValue());
      }).flatMapError(this::unpack);
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, T7, T8, R> Promise<R> yieldAll(Function8<ExecResult<T1>, ExecResult<T2>, ExecResult<T3>, ExecResult<T4>, ExecResult<T5>, ExecResult<T6>, ExecResult<T7>, ExecResult<T8>, R> function) {
      Promise<T1> p1 = Types.cast(promises.get(0));
      Promise<T2> p2 = Types.cast(promises.get(1));
      Promise<T3> p3 = Types.cast(promises.get(2));
      Promise<T4> p4 = Types.cast(promises.get(3));
      Promise<T5> p5 = Types.cast(promises.get(4));
      Promise<T6> p6 = Types.cast(promises.get(5));
      Promise<T7> p7 = Types.cast(promises.get(6));
      Promise<T8> p8 = Types.cast(promises.get(7));
      return ParallelBatch.of(p1, p2, p3, p4, p5, p6, p7, p8).yieldAll().map(results -> {
        ExecResult<T1> r1 = Types.cast(results.get(0));
        ExecResult<T2> r2 = Types.cast(results.get(1));
        ExecResult<T3> r3 = Types.cast(results.get(2));
        ExecResult<T4> r4 = Types.cast(results.get(3));
        ExecResult<T5> r5 = Types.cast(results.get(4));
        ExecResult<T6> r6 = Types.cast(results.get(5));
        ExecResult<T7> r7 = Types.cast(results.get(6));
        ExecResult<T8> r8 = Types.cast(results.get(7));
        return function.apply(r1, r2, r3, r4, r5, r6, r7, r8);
      });
    }

  }

  public static DefaultPromiseZipper of(Promise<?> p1, Promise<?> p2) {
    return new DefaultPromiseZipper(Arrays.asList(p1, p2));
  }

  public static DefaultPromiseZipper3 of(Promise<?> p1, Promise<?> p2, Promise<?> p3) {
    return new DefaultPromiseZipper3(Arrays.asList(p1, p2, p3));
  }

  public static DefaultPromiseZipper4 of(Promise<?> p1, Promise<?> p2, Promise<?> p3, Promise<?> p4) {
    return new DefaultPromiseZipper4(Arrays.asList(p1, p2, p3, p4));
  }

  public static DefaultPromiseZipper5 of(Promise<?> p1, Promise<?> p2, Promise<?> p3, Promise<?> p4, Promise<?> p5) {
    return new DefaultPromiseZipper5(Arrays.asList(p1, p2, p3, p4, p5));
  }

  public static DefaultPromiseZipper6 of(Promise<?> p1, Promise<?> p2, Promise<?> p3, Promise<?> p4, Promise<?> p5, Promise<?> p6) {
    return new DefaultPromiseZipper6(Arrays.asList(p1, p2, p3, p4, p5, p6));
  }

  public static DefaultPromiseZipper7 of(Promise<?> p1, Promise<?> p2, Promise<?> p3, Promise<?> p4, Promise<?> p5, Promise<?> p6, Promise<?> p7) {
    return new DefaultPromiseZipper7(Arrays.asList(p1, p2, p3, p4, p5, p6, p7));
  }

  public static DefaultPromiseZipper8 of(Promise<?> p1, Promise<?> p2, Promise<?> p3, Promise<?> p4, Promise<?> p5, Promise<?> p6, Promise<?> p7, Promise<?> p8) {
    return new DefaultPromiseZipper8(Arrays.asList(p1, p2, p3, p4, p5, p6, p7, p8));
  }
}
