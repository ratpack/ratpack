package ratpack.rx2.internal;

import ratpack.exec.ExecController;

public class MultiExecControllerBackedScheduler extends MultiExecutorBackedScheduler {

  @Override
  public ExecutorBackedScheduler getExecutorBackedScheduler(ExecController execController) {
    return new ExecControllerBackedScheduler(execController);
  }
}
