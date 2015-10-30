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
