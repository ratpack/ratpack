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

package ratpack.exec;

import ratpack.api.UncheckedException;
import ratpack.exec.util.ParallelBatch;
import ratpack.func.*;
import ratpack.util.Exceptions;
import ratpack.util.Types;

public class Promises {

  // zip

  public static <T1, T2, R> Promise<R> zip(Promise<T1> p1, Promise<T2> p2, BiFunction<T1, T2, R> zipper) {
    return zipAll(p1, p2, (ExecResult<T1> t1, ExecResult<T2> t2) -> {
      throwIfError(t1, t2);
      return zipper.apply(t1.getValue(), t2.getValue());
    }).flatMapError(Promises::unpack);
  }

  public static <T1, T2, T3, R> Promise<R> zip(Promise<T1> p1, Promise<T2> p2, Promise<T3> p3, Function3<T1, T2, T3, R> zipper) {
    return zipAll(p1, p2, p3, (ExecResult<T1> t1, ExecResult<T2> t2, ExecResult<T3> t3) -> {
      throwIfError(t1, t2, t3);
      return zipper.apply(t1.getValue(), t2.getValue(), t3.getValue());
    }).flatMapError(Promises::unpack);
  }

  public static <T1, T2, T3, T4, R> Promise<R> zip(Promise<T1> p1, Promise<T2> p2, Promise<T3> p3, Promise<T4> p4, Function4<T1, T2, T3, T4, R> zipper) {
    return zipAll(p1, p2, p3, p4, (ExecResult<T1> t1, ExecResult<T2> t2, ExecResult<T3> t3, ExecResult<T4> t4) -> {
      throwIfError(t1, t2, t3, t4);
      return zipper.apply(t1.getValue(), t2.getValue(), t3.getValue(), t4.getValue());
    }).flatMapError(Promises::unpack);
  }

  public static <T1, T2, T3, T4, T5, R> Promise<R> zip(Promise<T1> p1, Promise<T2> p2, Promise<T3> p3, Promise<T4> p4, Promise<T5> p5, Function5<T1, T2, T3, T4, T5, R> zipper) {
    return zipAll(p1, p2, p3, p4, p5, (ExecResult<T1> t1, ExecResult<T2> t2, ExecResult<T3> t3, ExecResult<T4> t4, ExecResult<T5> t5) -> {
      throwIfError(t1, t2, t3, t4, t5);
      return zipper.apply(t1.getValue(), t2.getValue(), t3.getValue(), t4.getValue(), t5.getValue());
    }).flatMapError(Promises::unpack);
  }

  public static <T1, T2, T3, T4, T5, T6, R> Promise<R> zip(Promise<T1> p1, Promise<T2> p2, Promise<T3> p3, Promise<T4> p4, Promise<T5> p5, Promise<T6> p6, Function6<T1, T2, T3, T4, T5, T6, R> zipper) {
    return zipAll(p1, p2, p3, p4, p5, p6, (ExecResult<T1> t1, ExecResult<T2> t2, ExecResult<T3> t3, ExecResult<T4> t4, ExecResult<T5> t5, ExecResult<T6> t6) -> {
      throwIfError(t1, t2, t3, t4, t5, t6);
      return zipper.apply(t1.getValue(), t2.getValue(), t3.getValue(), t4.getValue(), t5.getValue(), t6.getValue());
    }).flatMapError(Promises::unpack);
  }

  public static <T1, T2, T3, T4, T5, T6, T7, R> Promise<R> zip(Promise<T1> p1, Promise<T2> p2, Promise<T3> p3, Promise<T4> p4, Promise<T5> p5, Promise<T6> p6, Promise<T7> p7, Function7<T1, T2, T3, T4, T5, T6, T7, R> zipper) {
    return zipAll(p1, p2, p3, p4, p5, p6, p7, (ExecResult<T1> t1, ExecResult<T2> t2, ExecResult<T3> t3, ExecResult<T4> t4, ExecResult<T5> t5, ExecResult<T6> t6, ExecResult<T7> t7) -> {
      throwIfError(t1, t2, t3, t4, t5, t6, t7);
      return zipper.apply(t1.getValue(), t2.getValue(), t3.getValue(), t4.getValue(), t5.getValue(), t6.getValue(), t7.getValue());
    }).flatMapError(Promises::unpack);
  }

  public static <T1, T2, T3, T4, T5, T6, T7, T8, R> Promise<R> zip(Promise<T1> p1, Promise<T2> p2, Promise<T3> p3, Promise<T4> p4, Promise<T5> p5, Promise<T6> p6, Promise<T7> p7, Promise<T8> p8, Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> zipper) {
    return zipAll(p1, p2, p3, p4, p5, p6, p7, p8, (ExecResult<T1> t1, ExecResult<T2> t2, ExecResult<T3> t3, ExecResult<T4> t4, ExecResult<T5> t5, ExecResult<T6> t6, ExecResult<T7> t7, ExecResult<T8> t8) -> {
      throwIfError(t1, t2, t3, t4, t5, t6, t7, t8);
      return zipper.apply(t1.getValue(), t2.getValue(), t3.getValue(), t4.getValue(), t5.getValue(), t6.getValue(), t7.getValue(), t8.getValue());
    }).flatMapError(Promises::unpack);
  }

  // zipAll

  public static <T1, T2, R> Promise<R> zipAll(Promise<T1> p1, Promise<T2> p2, BiFunction<ExecResult<T1>, ExecResult<T2>, R> zipper) {
    return ParallelBatch.of(p1, p2).yieldAll().map(results -> {
      ExecResult<T1> r1 = Types.cast(results.get(0));
      ExecResult<T2> r2 = Types.cast(results.get(1));
      return zipper.apply(r1, r2);
    });
  }

  public static <T1, T2, T3, R> Promise<R> zipAll(Promise<T1> p1, Promise<T2> p2, Promise<T3> p3, Function3<ExecResult<T1>, ExecResult<T2>, ExecResult<T3>, R> zipper) {
    return ParallelBatch.of(p1, p2, p3).yieldAll().map(results -> {
      ExecResult<T1> r1 = Types.cast(results.get(0));
      ExecResult<T2> r2 = Types.cast(results.get(1));
      ExecResult<T3> r3 = Types.cast(results.get(2));
      return zipper.apply(r1, r2, r3);
    });
  }

  public static <T1, T2, T3, T4, R> Promise<R> zipAll(Promise<T1> p1, Promise<T2> p2, Promise<T3> p3, Promise<T4> p4, Function4<ExecResult<T1>, ExecResult<T2>, ExecResult<T3>, ExecResult<T4>, R> zipper) {
    return ParallelBatch.of(p1, p2, p3, p4).yieldAll().map(results -> {
      ExecResult<T1> r1 = Types.cast(results.get(0));
      ExecResult<T2> r2 = Types.cast(results.get(1));
      ExecResult<T3> r3 = Types.cast(results.get(2));
      ExecResult<T4> r4 = Types.cast(results.get(3));
      return zipper.apply(r1, r2, r3, r4);
    });
  }

  public static <T1, T2, T3, T4, T5, R> Promise<R> zipAll(Promise<T1> p1, Promise<T2> p2, Promise<T3> p3, Promise<T4> p4, Promise<T5> p5, Function5<ExecResult<T1>, ExecResult<T2>, ExecResult<T3>, ExecResult<T4>, ExecResult<T5>, R> zipper) {
    return ParallelBatch.of(p1, p2, p3, p4, p5).yieldAll().map(results -> {
      ExecResult<T1> r1 = Types.cast(results.get(0));
      ExecResult<T2> r2 = Types.cast(results.get(1));
      ExecResult<T3> r3 = Types.cast(results.get(2));
      ExecResult<T4> r4 = Types.cast(results.get(3));
      ExecResult<T5> r5 = Types.cast(results.get(4));
      return zipper.apply(r1, r2, r3, r4, r5);
    });
  }

  public static <T1, T2, T3, T4, T5, T6, R> Promise<R> zipAll(Promise<T1> p1, Promise<T2> p2, Promise<T3> p3, Promise<T4> p4, Promise<T5> p5, Promise<T6> p6, Function6<ExecResult<T1>, ExecResult<T2>, ExecResult<T3>, ExecResult<T4>, ExecResult<T5>, ExecResult<T6>, R> zipper) {
    return ParallelBatch.of(p1, p2, p3, p4, p5, p6).yieldAll().map(results -> {
      ExecResult<T1> r1 = Types.cast(results.get(0));
      ExecResult<T2> r2 = Types.cast(results.get(1));
      ExecResult<T3> r3 = Types.cast(results.get(2));
      ExecResult<T4> r4 = Types.cast(results.get(3));
      ExecResult<T5> r5 = Types.cast(results.get(4));
      ExecResult<T6> r6 = Types.cast(results.get(5));
      return zipper.apply(r1, r2, r3, r4, r5, r6);
    });
  }

  public static <T1, T2, T3, T4, T5, T6, T7, R> Promise<R> zipAll(Promise<T1> p1, Promise<T2> p2, Promise<T3> p3, Promise<T4> p4, Promise<T5> p5, Promise<T6> p6, Promise<T7> p7, Function7<ExecResult<T1>, ExecResult<T2>, ExecResult<T3>, ExecResult<T4>, ExecResult<T5>, ExecResult<T6>, ExecResult<T7>, R> zipper) {
    return ParallelBatch.of(p1, p2, p3, p4, p5, p6, p7).yieldAll().map(results -> {
      ExecResult<T1> r1 = Types.cast(results.get(0));
      ExecResult<T2> r2 = Types.cast(results.get(1));
      ExecResult<T3> r3 = Types.cast(results.get(2));
      ExecResult<T4> r4 = Types.cast(results.get(3));
      ExecResult<T5> r5 = Types.cast(results.get(4));
      ExecResult<T6> r6 = Types.cast(results.get(5));
      ExecResult<T7> r7 = Types.cast(results.get(6));
      return zipper.apply(r1, r2, r3, r4, r5, r6, r7);
    });
  }

  public static <T1, T2, T3, T4, T5, T6, T7, T8, R> Promise<R> zipAll(Promise<T1> p1, Promise<T2> p2, Promise<T3> p3, Promise<T4> p4, Promise<T5> p5, Promise<T6> p6, Promise<T7> p7, Promise<T8> p8, Function8<ExecResult<T1>, ExecResult<T2>, ExecResult<T3>, ExecResult<T4>, ExecResult<T5>, ExecResult<T6>, ExecResult<T7>, ExecResult<T8>, R> zipper) {
    return ParallelBatch.of(p1, p2, p3, p4, p5, p6, p7, p8).yieldAll().map(results -> {
      ExecResult<T1> r1 = Types.cast(results.get(0));
      ExecResult<T2> r2 = Types.cast(results.get(1));
      ExecResult<T3> r3 = Types.cast(results.get(2));
      ExecResult<T4> r4 = Types.cast(results.get(3));
      ExecResult<T5> r5 = Types.cast(results.get(4));
      ExecResult<T6> r6 = Types.cast(results.get(5));
      ExecResult<T7> r7 = Types.cast(results.get(6));
      ExecResult<T8> r8 = Types.cast(results.get(7));
      return zipper.apply(r1, r2, r3, r4, r5, r6, r7, r8);
    });
  }

  private static void throwIfError(ExecResult<?>... execResult) {
    for (int i = 0; i < execResult.length; i++) {
      if (execResult[i].getThrowable() != null) {
        throw Exceptions.uncheck(execResult[i].getThrowable());
      }
    }
  }

  private static <R> Promise<R> unpack(Throwable t) {
    if (t instanceof UncheckedException) {
      return Promise.error(t.getCause());
    }
    return Promise.error(t);
  }

}
