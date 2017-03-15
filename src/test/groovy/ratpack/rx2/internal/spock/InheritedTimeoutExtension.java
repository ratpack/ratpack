package ratpack.rx2.internal.spock;

import org.spockframework.runtime.extension.AbstractAnnotationDrivenExtension;
import org.spockframework.runtime.extension.builtin.TimeoutInterceptor;
import org.spockframework.runtime.model.FeatureInfo;
import org.spockframework.runtime.model.MethodInfo;
import org.spockframework.runtime.model.SpecInfo;
import spock.lang.Timeout;

import java.lang.annotation.Annotation;
import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

public final class InheritedTimeoutExtension extends AbstractAnnotationDrivenExtension<InheritedTimeout> {

  @Override
  public void visitSpecAnnotation(InheritedTimeout timeout, SpecInfo spec) {
    if (isDebuggerAttached()) {
      return;
    }

    for (FeatureInfo feature : spec.getFeatures()) {
      if (!feature.getFeatureMethod().getReflection().isAnnotationPresent(InheritedTimeout.class)) {
        visitFeatureAnnotation(timeout, feature);
      }
    }
  }

  @Override
  public void visitFeatureAnnotation(InheritedTimeout timeout, FeatureInfo feature) {
    feature.getFeatureMethod().addInterceptor(new TimeoutInterceptor(adapt(timeout)));
  }

  @Override
  public void visitFixtureAnnotation(InheritedTimeout timeout, MethodInfo fixtureMethod) {
    fixtureMethod.addInterceptor(new TimeoutInterceptor(adapt(timeout)));
  }

  private static Timeout adapt(InheritedTimeout inheritedTimeout) {
    return new Timeout() {
      @Override
      public int value() {
        return inheritedTimeout.value();
      }

      @Override
      public TimeUnit unit() {
        return inheritedTimeout.unit();
      }

      @Override
      public Class<? extends Annotation> annotationType() {
        return Timeout.class;
      }
    };
  }

  private static boolean isDebuggerAttached() {
    return ManagementFactory.getRuntimeMXBean().getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;
  }

}
