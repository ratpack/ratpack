package ratpack.rx2.internal;

import io.reactivex.Scheduler;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import ratpack.exec.ExecController;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class ExecutorBackedScheduler extends Scheduler {

  protected final ExecController execController;
  protected final Scheduler delegateScheduler;

  protected ExecutorBackedScheduler(ExecController execController) {
    this.execController = execController;
    this.delegateScheduler = Schedulers.from(getExecutor());
  }

  abstract ExecutorService getExecutor();

  @Override
  public long now(@NonNull TimeUnit unit) {
    return delegateScheduler.now(unit);
  }

  @Override
  public Worker createWorker() {
    return new Worker() {
      private final Worker delegateWorker = delegateScheduler.createWorker();

      class ExecutionWrappedAction implements Runnable {
        private final Runnable delegate;

        ExecutionWrappedAction(Runnable delegate) {
          this.delegate = delegate;
        }

        @Override
        public void run() {
          execController.fork().start(execution -> delegate.run());
        }
      }

      @Override
      public Disposable schedule(@NonNull Runnable run) {
        return delegateWorker.schedule(new ExecutionWrappedAction(run));
      }

      @Override
      public Disposable schedule(@NonNull Runnable run, long delayTime, TimeUnit unit) {
        return delegateWorker.schedule(new ExecutionWrappedAction(run), delayTime, unit);
      }

      @Override
      public void dispose() {
        delegateWorker.dispose();
      }

      @Override
      public boolean isDisposed() {
        return delegateWorker.isDisposed();
      }

      @Override
      public long now(@NonNull TimeUnit unit) {
        return delegateWorker.now(unit);
      }
    };
  }
}
