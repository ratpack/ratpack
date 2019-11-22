package ratpack.micrometer.metrics.internal;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.util.StringUtils;

public class TagUtils {
  private static final Tag EXCEPTION_NONE = Tag.of("exception", "None");

  public static Tag exception(Throwable exception) {
    if (exception != null) {
      String simpleName = exception.getClass().getSimpleName();
      return Tag.of("exception", StringUtils.isBlank(simpleName) ? exception.getClass().getName() : simpleName);
    }
    return EXCEPTION_NONE;
  }
}
