package ratpack.rx2.internal

import groovy.transform.CompileStatic
import io.netty.util.ResourceLeakDetector

import java.util.concurrent.atomic.AtomicReference

@CompileStatic
class FlaggingResourceLeakDetector<T> extends ResourceLeakDetector<T> {

  private final AtomicReference<Boolean> flag

  FlaggingResourceLeakDetector(Class<T> resourceType, int samplingInterval, long maxActive, AtomicReference<Boolean> flag) {
    super(resourceType, samplingInterval, maxActive)
    this.flag = flag
  }


  @Override
  protected void reportTracedLeak(String resourceType, String records) {
    flag.set(true)
    super.reportTracedLeak(resourceType, records)
  }

  @Override
  protected void reportUntracedLeak(String resourceType) {
    flag.set(true)
    super.reportUntracedLeak(resourceType)
  }

  @Override
  protected void reportInstancesLeak(String resourceType) {
    flag.set(true)
    super.reportInstancesLeak(resourceType)
  }
}
