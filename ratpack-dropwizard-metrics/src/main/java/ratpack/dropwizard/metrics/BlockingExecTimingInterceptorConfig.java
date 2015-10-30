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

package ratpack.dropwizard.metrics;

import java.util.Optional;

/**
 *
 */
public class BlockingExecTimingInterceptorConfig {
    private boolean enabled = true;
    private Optional<BlockingExecTimingInterceptor> interceptor = Optional.empty();

    /**
     *
     * @return
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     *
     * @param enabled
     * @return
     */
    public BlockingExecTimingInterceptorConfig enable(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     *
     * @return
     */
    public Optional<BlockingExecTimingInterceptor> getInterceptor() {
        return interceptor;
    }

    /**
     * Provide custom blocking exec timing interceptor. This will replace
     * the default.
     * @param interceptor
     */
    public BlockingExecTimingInterceptorConfig interceptor(BlockingExecTimingInterceptor interceptor) {
        this.interceptor = Optional.of(interceptor);
        return this;
    }

}
