/*
 * Copyright 2021 the original author or authors.
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

package ratpack.test.internal.testname;

import org.spockframework.runtime.IRunListener;
import org.spockframework.runtime.extension.IAnnotationDrivenExtension;
import org.spockframework.runtime.model.ErrorInfo;
import org.spockframework.runtime.model.FeatureInfo;
import org.spockframework.runtime.model.IterationInfo;
import org.spockframework.runtime.model.SpecInfo;

import java.util.Optional;

public final class TrackCurrentTestNameExtension implements IAnnotationDrivenExtension<TrackCurrentTestName> {

    @Override
    public void visitSpecAnnotation(TrackCurrentTestName unroll, SpecInfo spec) {
        spec.addListener(new IRunListener() {
            @Override
            public void beforeSpec(SpecInfo spec) {
                nameSpec(spec);
            }

            @Override
            public void beforeFeature(FeatureInfo feature) {
                nameFeature(feature);
            }

            @Override
            public void beforeIteration(IterationInfo iteration) {
                CurrentTestName.set(Optional.of(iteration.getFeature().getSpec().getName() + "." + iteration.getName()));
            }

            @Override
            public void afterIteration(IterationInfo iteration) {
                nameFeature(iteration.getFeature());
            }

            @Override
            public void afterFeature(FeatureInfo feature) {
                nameSpec(feature.getSpec());
            }

            @Override
            public void afterSpec(SpecInfo spec) {
                CurrentTestName.set(Optional.empty());
            }

            @Override
            public void error(ErrorInfo error) {

            }

            @Override
            public void specSkipped(SpecInfo spec) {

            }

            @Override
            public void featureSkipped(FeatureInfo feature) {

            }
        });
    }

    private static void nameSpec(SpecInfo spec) {
        CurrentTestName.set(Optional.of(spec.getName()));
    }

    private static void nameFeature(FeatureInfo feature) {
        CurrentTestName.set(Optional.of(feature.getSpec().getName() + "." + feature.getName()));
    }

}

