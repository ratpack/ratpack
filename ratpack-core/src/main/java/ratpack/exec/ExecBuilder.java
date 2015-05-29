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

package ratpack.exec;

import io.netty.channel.EventLoop;
import ratpack.func.Action;
import ratpack.func.BiAction;
import ratpack.registry.RegistrySpec;

/**
 * Effectively a builder for a new {@link Execution}.
 *
 * @see ExecControl#fork()
 */
public interface ExecBuilder {

  ExecBuilder onError(Action<? super Throwable> onError);

  ExecBuilder onError(BiAction<? super Execution, ? super Throwable> onError);

  ExecBuilder onComplete(Action<? super Execution> onComplete);

  ExecBuilder register(Action<? super RegistrySpec> registry);

  ExecBuilder eventLoop(EventLoop eventLoop);

  void start(Action<? super Execution> action);

}
