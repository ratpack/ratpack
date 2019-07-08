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

package ratpack.exec.util;

import ratpack.exec.ExecResult;
import ratpack.exec.Promise;
import ratpack.exec.util.internal.DefaultPromiseZipper;
import ratpack.func.*;

public interface PromiseZipper {


  <T1, T2, R> Promise<R> yield(BiFunction<T1, T2, R> function);

  <T1, T2, R> Promise<R> yieldAll(BiFunction<ExecResult<T1>, ExecResult<T2>, R> function);

  static <T1, T2> PromiseZipper zip(Promise<T1> p1, Promise<T2> p2) {
    return DefaultPromiseZipper.of(p1, p2);
  }

  static <T1, T2, T3> PromiseZipper3 zip(Promise<T1> p1, Promise<T2> p2, Promise<T3> p3) {
    return DefaultPromiseZipper.of(p1, p2, p3);
  }

  static <T1, T2, T3, T4> PromiseZipper4 zip(Promise<T1> p1, Promise<T2> p2, Promise<T3> p3, Promise<T4> p4) {
    return DefaultPromiseZipper.of(p1, p2, p3, p4);
  }

  static <T1, T2, T3, T4, T5> PromiseZipper5 zip(Promise<T1> p1, Promise<T2> p2, Promise<T3> p3, Promise<T4> p4, Promise<T5> p5) {
    return DefaultPromiseZipper.of(p1, p2, p3, p4, p5);
  }

  static <T1, T2, T3, T4, T5, T6> PromiseZipper6 zip(Promise<T1> p1, Promise<T2> p2, Promise<T3> p3, Promise<T4> p4, Promise<T5> p5, Promise<T6> p6) {
    return DefaultPromiseZipper.of(p1, p2, p3, p4, p5, p6);
  }

  static <T1, T2, T3, T4, T5, T6, T7> PromiseZipper7 zip(Promise<T1> p1, Promise<T2> p2, Promise<T3> p3, Promise<T4> p4, Promise<T5> p5, Promise<T6> p6, Promise<T7> p7) {
    return DefaultPromiseZipper.of(p1, p2, p3, p4, p5, p6, p7);
  }

  static <T1, T2, T3, T4, T5, T6, T7, T8> PromiseZipper8 zip(Promise<T1> p1, Promise<T2> p2, Promise<T3> p3, Promise<T4> p4, Promise<T5> p5, Promise<T6> p6, Promise<T7> p7, Promise<T8> p8) {
    return DefaultPromiseZipper.of(p1, p2, p3, p4, p5, p6, p7, p8);
  }

  interface PromiseZipper3 {
    <T1, T2, T3, R> Promise<R> yield(Function3<T1, T2, T3, R> function);

    <T1, T2, T3, R> Promise<R> yieldAll(Function3<ExecResult<T1>, ExecResult<T2>, ExecResult<T3>, R> function);
  }

  interface PromiseZipper4 {
    <T1, T2, T3, T4, R> Promise<R> yield(Function4<T1, T2, T3, T4, R> function);

    <T1, T2, T3, T4, R> Promise<R> yieldAll(Function4<ExecResult<T1>, ExecResult<T2>, ExecResult<T3>, ExecResult<T4>, R> function);
  }

  interface PromiseZipper5 {
    <T1, T2, T3, T4, T5, R> Promise<R> yield(Function5<T1, T2, T3, T4, T5, R> function);

    <T1, T2, T3, T4, T5, R> Promise<R> yieldAll(Function5<ExecResult<T1>, ExecResult<T2>, ExecResult<T3>, ExecResult<T4>, ExecResult<T5>, R> function);
  }

  interface PromiseZipper6 {
    <T1, T2, T3, T4, T5, T6, R> Promise<R> yield(Function6<T1, T2, T3, T4, T5, T6, R> function);

    <T1, T2, T3, T4, T5, T6, R> Promise<R> yieldAll(Function6<ExecResult<T1>, ExecResult<T2>, ExecResult<T3>, ExecResult<T4>, ExecResult<T5>, ExecResult<T6>, R> function);
  }

  interface PromiseZipper7 {
    <T1, T2, T3, T4, T5, T6, T7, R> Promise<R> yield(Function7<T1, T2, T3, T4, T5, T6, T7, R> function);

    <T1, T2, T3, T4, T5, T6, T7, R> Promise<R> yieldAll(Function7<ExecResult<T1>, ExecResult<T2>, ExecResult<T3>, ExecResult<T4>, ExecResult<T5>, ExecResult<T6>, ExecResult<T7>, R> function);
  }

  interface PromiseZipper8 {
    <T1, T2, T3, T4, T5, T6, T7, T8, R> Promise<R> yield(Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> function);

    <T1, T2, T3, T4, T5, T6, T7, T8, R> Promise<R> yieldAll(Function8<ExecResult<T1>, ExecResult<T2>, ExecResult<T3>, ExecResult<T4>, ExecResult<T5>, ExecResult<T6>, ExecResult<T7>, ExecResult<T8>, R> function);
  }
}
