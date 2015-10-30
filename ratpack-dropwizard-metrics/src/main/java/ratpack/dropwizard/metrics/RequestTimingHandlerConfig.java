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
