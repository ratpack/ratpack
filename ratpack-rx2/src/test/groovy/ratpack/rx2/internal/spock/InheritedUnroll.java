package ratpack.rx2.internal.spock;

import org.spockframework.runtime.extension.ExtensionAnnotation;

import java.lang.annotation.*;

/**
 * Annotation analogous in behavior to {@link spock.lang.Unroll}, but
 * in addition to the original annotation, this annotation is inherited.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Inherited
@ExtensionAnnotation(InheritedUnrollExtension.class)
public @interface InheritedUnroll {

  String value() default "";

}
