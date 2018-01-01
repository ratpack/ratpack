package ratpack.rx2.internal;

import ratpack.exec.ExecController;

import java.util.concurrent.ExecutorService;

public class ExecControllerBackedScheduler extends ExecutorBackedScheduler {

  public ExecControllerBackedScheduler(ExecController execController) {
    super(execController);
  }

  @Override
  ExecutorService getExecutor() {
    return execController.getEventLoopGroup();
  }
}
