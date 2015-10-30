package ratpack.dropwizard.metrics.internal;

import com.codahale.metrics.MetricRegistry;
import ratpack.dropwizard.metrics.DropwizardMetricsConfig;
import ratpack.dropwizard.metrics.RequestTimingHandlerConfig;
import ratpack.dropwizard.metrics.RequestTimingHandler;
import ratpack.handling.Context;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Optional;

/**
 * Provide an instance of a request timing handler.
 */
public class RequestTimingHandlerProvider implements Provider<RequestTimingHandler> {

    private final MetricRegistry metricRegistry;
    private final DropwizardMetricsConfig config;

    @Inject
    public RequestTimingHandlerProvider(MetricRegistry metricRegistry, DropwizardMetricsConfig config) {
        this.metricRegistry = metricRegistry;
        this.config = config;
    }

    @Override
    public RequestTimingHandler get() {
        RequestTimingHandler handler = Context::next;
        Optional<RequestTimingHandlerConfig> o = config.getHandler();
        if (o.isPresent()) {
            RequestTimingHandlerConfig handlerConfig = o.get();
            if (handlerConfig.isEnabled()) {
                handler = handlerConfig.getHandler().orElse(new DefaultRequestTimingHandler(metricRegistry, config));
            }
        }
        return handler;
    }
}
