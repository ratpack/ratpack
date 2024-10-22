/*
 * Copyright 2015 the original author or authors.
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

import ratpack.exec.*;
import ratpack.func.Block;
import ratpack.func.Function;

public class DefaultOperation implements Operation {

  private final Upstream<Void> upstream;

  public static Operation of(Upstream<Void> upstream) {
    if (upstream instanceof DefaultOperation) {
      return (Operation) upstream;
    } else {
      return new DefaultOperation(upstream);
    }
  }

  private DefaultOperation(Upstream<Void> upstream) {
    this.upstream = upstream;
  }

  @Override
  public Promise<Void> promise() {
    return Promise.async(upstream);
  }

  @Override
  public Operation transform(Function<? super Upstream<Void>, ? extends Upstream<Void>> upstreamTransformer) {
    Upstream<Void> transformedUpstream;
    try {
      transformedUpstream = upstreamTransformer.apply(upstream);
    } catch (Throwable e) {
      return Operation.error(e);
    }
    return new DefaultOperation(transformedUpstream);
  }

  @Override
  public void connect(Downstream<? super Void> downstream) {
    ExecThreadBinding.requireComputeThread("Operation.connect() can only be called on a compute thread");
    try {
      upstream.connect(downstream);
    } catch (ExecutionException e) {
      throw e;
    } catch (Throwable e) {
      DefaultExecution.throwError(e);
    }
  }

  @Override
  public void then(Block block) {
    try {
      upstream.connect(new Downstream<Object>() {
        @Override
        public void success(Object value) {
          try {
            block.execute();
          } catch (Throwable e) {
            DefaultExecution.throwError(e);
          }
        }

        @Override
        public void error(Throwable throwable) {
          DefaultExecution.throwError(throwable);
        }

        @Override
        public void complete() {

        }
      });
    } catch (ExecutionException e) {
      throw e;
    } catch (Throwable e) {
      DefaultExecution.throwError(e);
    }
  }

}
