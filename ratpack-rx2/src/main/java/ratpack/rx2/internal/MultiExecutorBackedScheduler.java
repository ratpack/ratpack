/*
 * Copyright 2017 the original author or authors.
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

package ratpack.rx2.internal;

import com.google.common.collect.MapMaker;
import io.reactivex.Scheduler;
import io.reactivex.annotations.NonNull;
import io.reactivex.schedulers.Schedulers;
import ratpack.exec.ExecController;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public abstract class MultiExecutorBackedScheduler extends Scheduler {
  private final ConcurrentMap<ExecController, Scheduler> map = new MapMaker().weakKeys().weakValues().makeMap();
  private final AtomicReference<Scheduler> fallback = new AtomicReference<>();

  public abstract ExecutorBackedScheduler getExecutorBackedScheduler(ExecController execController);

  private Scheduler getDelegateScheduler() {
    return ExecController.current()
      .map(c -> map.computeIfAbsent(c, this::getExecutorBackedScheduler))
      .orElseGet(() -> {
        if (fallback.get() == null) {
          int nThreads = Runtime.getRuntime().availableProcessors();
          ExecutorService executor = Executors.newFixedThreadPool(nThreads);
          Scheduler scheduler = Schedulers.from(executor);
          fallback.compareAndSet(null, scheduler);
        }
        return fallback.get();
      });
  }

  @Override
  public Scheduler.Worker createWorker() {
    return getDelegateScheduler().createWorker();
  }

  @Override
  public long now(@NonNull TimeUnit unit) {
    return getDelegateScheduler().now(unit);
  }
}
