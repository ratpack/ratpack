package ratpack.exec.internal;

import ratpack.exec.ExecController;

import java.util.Optional;

public class ExecControllerThreadBinding {

  private static final ThreadLocal<ExecController> THREAD_BINDING = new ThreadLocal<>();

  static void set(ExecController execController) {
    THREAD_BINDING.set(execController);
  }

  public static Optional<ExecController> get() {
    return Optional.ofNullable(THREAD_BINDING.get());
  }

}
