package ratpack.micrometer.metrics.internal;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import ratpack.exec.Promise;

import javax.inject.Inject;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static ratpack.micrometer.metrics.internal.TagUtils.exception;

public class TimedAnnotationMethodInterceptor implements MethodInterceptor {
  @Inject
  MeterRegistry meterRegistry;

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    Object result;
    Method method = invocation.getMethod();

    Timed timed = invocation.getMethod().getAnnotation(Timed.class);
    String annotationName = timed.value();

    Function<Exception, Timer> timer = e -> {
      Tags exceptionAndExtraTags = Tags.of(timed.extraTags()).and(exception(e));
      return annotationName.isEmpty() ?
        meterRegistry.timer(method.getDeclaringClass().getName() + "." + method.getName(),
          exceptionAndExtraTags) :
        meterRegistry.timer(annotationName,
          Tags.of(
            Tag.of("method", method.getName()),
            Tag.of("class", method.getDeclaringClass().getName())
          ).and(exceptionAndExtraTags)
        );
    };

    Timer.Sample sample = Timer.start(meterRegistry);

    try {
      result = invocation.proceed();
      if (result instanceof Promise<?>) {
        result = ((Promise<?>) result).time(duration ->
          timer.apply(null).record(duration.getNano(), TimeUnit.NANOSECONDS)
        );
      } else {
        sample.stop(timer.apply(null));
      }
    } catch (Exception e) {
      sample.stop(timer.apply(e));
      throw e;
    }
    return result;
  }
}
