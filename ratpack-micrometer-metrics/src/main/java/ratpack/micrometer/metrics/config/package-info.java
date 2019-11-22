/**
 * This package provides two forms of config for each registry. For registry implementation Foo,
 * there will be a RatpackFooConfig that allows the registry to be configured in the Ratpack
 * {@link ratpack.server.ServerConfig} without assuming that Foo's {@link io.micrometer.core.instrument.MeterRegistry}
 * implementation is present on the classpath. RatpackFooConfig is mapped to MappedFooConfig just
 * before FooMeterRegistry is instantiated, and implements the FooConfig interface from the
 * corresponding Micrometer dependency.
 */
package ratpack.micrometer.metrics.config;
