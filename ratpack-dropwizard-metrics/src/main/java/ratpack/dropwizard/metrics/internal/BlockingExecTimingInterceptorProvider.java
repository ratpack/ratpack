/*
 * Copyright 2015 the original author or authors.
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

package ratpack.dropwizard.metrics.internal;

import com.codahale.metrics.MetricRegistry;
import ratpack.dropwizard.metrics.BlockingExecTimingInterceptor;
import ratpack.dropwizard.metrics.BlockingExecTimingInterceptorConfig;
import ratpack.dropwizard.metrics.DropwizardMetricsConfig;
import ratpack.exec.ExecInterceptor;
import ratpack.exec.Execution;
import ratpack.func.Block;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Optional;

/**
 * Provide a timer for blocking executions.
 */
public class BlockingExecTimingInterceptorProvider implements Provider<BlockingExecTimingInterceptor> {

    private final MetricRegistry metricRegistry;
    private final DropwizardMetricsConfig config;

    @Inject
    public BlockingExecTimingInterceptorProvider(MetricRegistry metricRegistry, DropwizardMetricsConfig config) {
        this.metricRegistry = metricRegistry;
        this.config = config;
    }

    @Override
    public BlockingExecTimingInterceptor get() {
        BlockingExecTimingInterceptor execInterceptor = (Execution execution, ExecInterceptor.ExecType execType, Block executionSegment) -> executionSegment.execute();
        Optional<BlockingExecTimingInterceptorConfig> o = config.getInterceptor();
        if (o.isPresent()) {
            BlockingExecTimingInterceptorConfig interceptorConfig = o.get();
            if (interceptorConfig.isEnabled()) {
                execInterceptor = interceptorConfig.getInterceptor().orElse(new DefaultBlockingExecTimingInterceptor(metricRegistry, config));
            }
        }
        return execInterceptor;
    }

}
