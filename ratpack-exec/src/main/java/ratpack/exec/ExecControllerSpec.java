/*
 * Copyright 2020 the original author or authors.
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


import io.netty.channel.EventLoopGroup;
import ratpack.func.Action;
import ratpack.func.BiFunction;
import ratpack.func.Function;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

public interface ExecControllerSpec {

  ExecControllerSpec interceptor(ExecInterceptor interceptor);

  ExecControllerSpec initializer(ExecInitializer initializer);

  ExecControllerSpec compute(Action<? super ComputeSpec> definition);

  ExecControllerSpec blocking(Action<? super BlockingSpec> definition);

  <E extends Enum<E> & ExecutionType> ExecControllerSpec binding(E executionType, Action<? super BindingSpec> definition);

  interface BindingSpec {
    BindingSpec threads(int threads);

    BindingSpec threadFactory(ThreadFactory threadFactory);

    BindingSpec eventLoopGroup(BiFunction<Integer, ThreadFactory, EventLoopGroup> eventLoopGroupFactory);

    BindingSpec executor(Function<ThreadFactory, ExecutorService> executorFactory);
  }

  interface ComputeSpec {
    ComputeSpec threads(int number);

    ComputeSpec prefix(String prefix);

    ComputeSpec priority(int priority);

    ComputeSpec eventLoopGroup(BiFunction<Integer, ThreadFactory, EventLoopGroup> eventLoopGroupFactory);

  }

  interface BlockingSpec {
    BlockingSpec prefix(String prefix);

    BlockingSpec priority(int priority);

    BlockingSpec executor(Function<ThreadFactory, ExecutorService> executorFactory);
  }
}
