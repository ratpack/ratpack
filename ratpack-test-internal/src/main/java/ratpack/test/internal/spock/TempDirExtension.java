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

import org.spockframework.runtime.InvalidSpecException;
import org.spockframework.runtime.extension.IAnnotationDrivenExtension;
import org.spockframework.runtime.model.FeatureInfo;
import org.spockframework.runtime.model.FieldInfo;
import org.spockframework.runtime.model.SpecInfo;
import org.spockframework.tempdir.TempDirConfiguration;

public class TempDirExtension implements IAnnotationDrivenExtension<TempDir> {
  private final TempDirConfiguration configuration;

  public TempDirExtension(TempDirConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public void visitFieldAnnotation(TempDir annotation, FieldInfo field) {
    Class<?> fieldType = field.getType();
    if (!fieldType.isAssignableFrom(TemporaryFolder.class)) {
      throw new InvalidSpecException("@TempDir can only be used on TemporaryFolder");
    }
    TempDirInterceptor interceptor = new TempDirInterceptor(fieldType, field, configuration.baseDir, configuration.keep);

    // attach interceptor
    SpecInfo specInfo = field.getParent();
    if (field.isShared()) {
      specInfo.addInterceptor(interceptor);
    } else {
      for (FeatureInfo featureInfo : specInfo.getBottomSpec().getAllFeatures()) {
        featureInfo.addIterationInterceptor(interceptor);
      }
    }
  }

}
