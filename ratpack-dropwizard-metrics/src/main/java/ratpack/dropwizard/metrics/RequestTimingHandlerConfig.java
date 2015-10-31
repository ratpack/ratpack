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
public class RequestTimingHandlerConfig {
    private boolean enabled = true;
    private Optional<Class<RequestTimingHandler>> handler = Optional.empty();

    /**
     *
     * @return whether or not recording request timing metrics is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     *
     * @param enabled whether or not to enable recording request timing metrics.
     * @return this
     */
    public RequestTimingHandlerConfig enable(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     *
     * @return The request timing handler.
     */
    public Optional<Class<RequestTimingHandler>> getHandler() {
        return handler;
    }

    /**
     * Provide custom request timing interceptor. This will replace
     * the default.
     *
     * @param requestTimingHandler The request timing handler.
     * @return this
     */
    public RequestTimingHandlerConfig handler(Class<RequestTimingHandler> requestTimingHandler) {
        this.handler = Optional.of(requestTimingHandler);
        return this;
    }
}
