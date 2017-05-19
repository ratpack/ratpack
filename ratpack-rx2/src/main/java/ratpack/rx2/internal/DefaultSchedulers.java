package ratpack.rx2.internal;

import io.reactivex.Scheduler;

public class DefaultSchedulers {

  private static Scheduler computationScheduler = new MultiExecControllerBackedScheduler();
  private static Scheduler ioScheduler = new MultiExecControllerBackedScheduler();

  public static Scheduler getComputationScheduler() {
    return computationScheduler;
  }

  public static Scheduler getIoScheduler() {
    return ioScheduler;
  }

}
