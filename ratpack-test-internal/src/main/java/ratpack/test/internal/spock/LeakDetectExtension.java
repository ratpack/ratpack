/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.test.internal.spock;

import org.spockframework.runtime.extension.IAnnotationDrivenExtension;
import org.spockframework.runtime.extension.IMethodInterceptor;
import org.spockframework.runtime.extension.IMethodInvocation;
import org.spockframework.runtime.model.FeatureInfo;
import org.spockframework.runtime.model.SpecInfo;
import ratpack.test.internal.leak.FlaggingResourceLeakDetectorFactory;

import java.util.ArrayList;
import java.util.List;

public class LeakDetectExtension implements IAnnotationDrivenExtension<LeakDetect> {

  public LeakDetectExtension() {

  }

  @Override
  public void visitSpecAnnotation(LeakDetect annotation, SpecInfo spec) {
    for (FeatureInfo feature : spec.getFeatures()) {
      if (!feature.getFeatureMethod().getReflection().isAnnotationPresent(LeakDetect.class)) {
        visitFeatureAnnotation(annotation, feature);
      }
    }
  }

  @Override
  public void visitFeatureAnnotation(LeakDetect annotation, FeatureInfo feature) {
    feature.getFeatureMethod().addInterceptor(new LeakDetectInterceptor(annotation));
  }

  private static class LeakDetectInterceptor implements IMethodInterceptor {

    private final LeakDetect annotation;

    public LeakDetectInterceptor(LeakDetect annotation) {
      this.annotation = annotation;
    }

    @Override
    public void intercept(final IMethodInvocation invocation) throws Throwable {
      FlaggingResourceLeakDetectorFactory.install();
      checkLeak();
      invocation.proceed();
      checkLeak();
    }

    private void checkLeak() {
      String leak = FlaggingResourceLeakDetectorFactory.LEAKS.poll();
      if (leak != null && !leak.isEmpty()) {
        List<String> leaks = new ArrayList<>();
        leaks.add(leak);
        while (leak != null && !leak.isEmpty()) {
          leak = FlaggingResourceLeakDetectorFactory.LEAKS.poll();
          leaks.add(leak);
        }
        throw new IllegalStateException("RESOURCE LEAKS" + String.join("\n\n", leaks));
      }
    }
  }
}
