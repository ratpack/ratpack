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

package ratpack.site.github;

import ratpack.exec.Downstream;
import ratpack.exec.Upstream;
import ratpack.exec.internal.CachingUpstream;
import ratpack.func.Function;
import ratpack.func.Predicate;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class TimedFaultTolerantCachingPromiseTransform<T> implements Function<Upstream<T>, Upstream<T>> {

  private final Duration cacheFor;
  private final Duration errorTimeout;
  private final AtomicLong nextUpdate = new AtomicLong(Long.MAX_VALUE);
  private final AtomicReference<Upstream<T>> effectiveUpstream = new AtomicReference<>();
  private final AtomicBoolean updatePending = new AtomicBoolean();

  public TimedFaultTolerantCachingPromiseTransform(Duration cacheFor, Duration errorTimeout) {
    this.cacheFor = cacheFor;
    this.errorTimeout = errorTimeout;
  }

  @Override
  public Upstream<T> apply(Upstream<T> upstream) throws Exception {
    return downstream -> {
      if (effectiveUpstream.compareAndSet(null, new CachingUpstream<>(upstream, Predicate.TRUE))) {
        effectiveUpstream.get().connect(new Downstream<T>() {
          @Override
          public void success(T value) {
            nextUpdate.set(System.currentTimeMillis() + cacheFor.toMillis());
            downstream.success(value);
          }

          @Override
          public void error(Throwable throwable) {
            nextUpdate.set(System.currentTimeMillis() + errorTimeout.toMillis());
            downstream.error(throwable);
          }

          @Override
          public void complete() {
            downstream.complete();
          }
        });
      } else {
        final long nextUpdateValue = nextUpdate.get();
        if (nextUpdateValue < System.currentTimeMillis()) {
          if (updatePending.compareAndSet(false, true)) {
            Upstream<T> potentialNewUpstream = new CachingUpstream<>(upstream, Predicate.TRUE);
            potentialNewUpstream.connect(new Downstream<T>() {
              @Override
              public void success(T value) {
                effectiveUpstream.set(potentialNewUpstream);
                nextUpdate.set(System.currentTimeMillis() + cacheFor.toMillis());
                downstream.success(value);
              }

              @Override
              public void error(Throwable throwable) {
                nextUpdate.set(System.currentTimeMillis() + errorTimeout.toMillis());
                try {
                  effectiveUpstream.get().connect(downstream);
                } catch (Exception e) {
                  downstream.error(e);
                }
              }

              @Override
              public void complete() {
                effectiveUpstream.set(potentialNewUpstream);
                nextUpdate.set(System.currentTimeMillis() + cacheFor.toMillis());
                downstream.complete();
              }
            });
          } else {
            // The value is timed out, but there's a new value on the way
            // We are using the previous value here
            // We should be “waiting” for the new value
            effectiveUpstream.get().connect(downstream);
          }
        } else {
          effectiveUpstream.get().connect(downstream);
        }
      }
    };
  }

}
