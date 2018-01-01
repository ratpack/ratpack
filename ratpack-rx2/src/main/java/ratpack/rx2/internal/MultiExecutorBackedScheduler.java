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
