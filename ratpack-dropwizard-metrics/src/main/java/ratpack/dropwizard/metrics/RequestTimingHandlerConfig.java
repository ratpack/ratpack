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
    private Optional<RequestTimingHandler> handler = Optional.empty();

    /**
     * Get state of blocking exec timer.
     * @return
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enable blocker exec timer.
     * @param enabled
     * @return
     */
    public RequestTimingHandlerConfig enable(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Get custom provided request timing handler.
     * @return
     */
    public Optional<RequestTimingHandler> getHandler() {
        return handler;
    }

    /**
     * Provide custom request timing interceptor. This will replace
     * the default.
     * @param requestTimingHandler
     */
    public RequestTimingHandlerConfig handler(RequestTimingHandler requestTimingHandler) {
        this.handler = Optional.of(requestTimingHandler);
        return this;
    }
}
