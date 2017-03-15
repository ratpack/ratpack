package ratpack.rx2.internal

import groovy.transform.CompileStatic
import io.netty.util.ResourceLeakDetector
import io.netty.util.ResourceLeakDetectorFactory

import java.util.concurrent.atomic.AtomicReference

@CompileStatic
class FlaggingResourceLeakDetectorFactory extends ResourceLeakDetectorFactory {

  private final AtomicReference<Boolean> flag

  FlaggingResourceLeakDetectorFactory(AtomicReference<Boolean> flag) {
    this.flag = flag
  }

  @Override
  @SuppressWarnings("UnnecessaryPublicModifier")
  public <T> ResourceLeakDetector<T> newResourceLeakDetector(Class<T> resource, int samplingInterval, long maxActive) {
    return new FlaggingResourceLeakDetector<T>(resource, samplingInterval, maxActive, flag)
  }

}
