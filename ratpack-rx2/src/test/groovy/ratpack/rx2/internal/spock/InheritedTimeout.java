package ratpack.rx2.internal.spock;

import org.spockframework.runtime.extension.ExtensionAnnotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ExtensionAnnotation(InheritedTimeoutExtension.class)
@Inherited
public @interface InheritedTimeout {

  /**
   * Returns the duration after which the execution of the annotated feature or fixture
   * method times out.
   *
   * @return the duration after which the execution of the annotated feature or
   * fixture method times out
   */
  int value();

  /**
   * Returns the duration's time unit.
   *
   * @return the duration's time unit
   */
  TimeUnit unit() default TimeUnit.SECONDS;
}
