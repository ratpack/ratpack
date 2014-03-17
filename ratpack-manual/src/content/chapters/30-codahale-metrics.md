# Coda Hale Metrics

The `ratpack-codahale-metrics` jar provides integration with [Coda Hale's Metrics library](http://metrics.codahale.com/).
The jar has a Guice module that exposes configuration options for metric collection, metric reporting and health checks.

See [`CodaHaleMetricsModule`](api/ratpack/codahale/metrics/CodaHaleMetricsModule.html) for detailed usage information.

## Built-in metrics

Ratpack provides built-in metric collectors for a deeper insight into how your application is performing.
When metrics are enabled within your application using [`CodaHaleMetricsModule.metrics()`](api/ratpack/codahale/metrics/CodaHaleMetricsModule.html#metrics\(\)), the built-in metric collectors will automatically be enabled too.

Ratpack has built-in metric collectors for:

* Request timing
* Background operation timing

Ratpack also has support for Coda Hale Metric's JVM instrumentation.  See [`CodaHaleMetricsModule.jvmMetrics()`](api/ratpack/codahale/metrics/CodaHaleMetricsModule.html#jvmMetrics\(\)) for usage information.

## Custom metrics

Ratpack enables you to capture your own application metrics in two ways:

1. Obtaining the `MetricRegistry` via dependency injection or context registry lookup and registering your own metrics with it.
2. Add metrics annotations to your Guice injected classes.

See [`CodaHaleMetricsModule.metrics()`](api/ratpack/codahale/metrics/CodaHaleMetricsModule.html#metrics\(\)) for more details.

## Reporting metrics

Ratpack supports metric reporters for the following outputs:

* [JMX](api/ratpack/codahale/metrics/CodaHaleMetricsModule.html#jmx\(\))
* [Console](api/ratpack/codahale/metrics/CodaHaleMetricsModule.html#console\(\))
* [CSV](api/ratpack/codahale/metrics/CodaHaleMetricsModule.html#csv\(java.io.File\))
* [Websockets](api/ratpack/codahale/metrics/CodaHaleMetricsModule.html#websocket\(\))

For an example of how to consume real-time metrics with websockets, see the [example-books](https://github.com/ratpack/example-books/blob/master/src/ratpack/Ratpack.groovy) project.

## Health checks

Health checks verify that your application components or responsibilities are performing as expected.

For detailed information on how to create your own application health checks and how to run them, see [`CodaHaleMetricsModule.healthChecks()`](api/ratpack/codahale/metrics/CodaHaleMetricsModule.html#healthChecks\(\)).