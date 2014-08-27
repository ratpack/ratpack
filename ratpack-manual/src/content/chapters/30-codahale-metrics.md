# Coda Hale Metrics

The `ratpack-codahale-metrics` jar provides integration with [Coda Hale's Metrics library](http://metrics.codahale.com/).

Coda Hale Metrics is one of the best metrics libraries out there for the JVM.
It provides a toolkit of metric types and metric reporters that will give a deep insight into your application's performance, whether that be at development time or real time in production.
It allows you to easily capture statistics like the number of requests served or response times, and also more generic information like the state of your internal collections, queues or how many times some portion of code is being executed.
By measuring your code you will know exactly what your code is doing when it runs and be able to make informed optimization decisions.

Ratpack's integration with Coda Hale Metrics means that many of these key metrics are captured already for you, simply by registering the Guice module.
Should you require further insight then Ratpack also makes it easy for you to capture additional metrics using the library's many metric types and then report all of these metrics to your required output using the library's metric reporters.

See [`CodaHaleMetricsModule`](api/ratpack/codahale/metrics/CodaHaleMetricsModule.html) for detailed usage information.

## Built-in metrics

Ratpack provides built-in metric collectors for key metrics.
When metrics are enabled within your application using [`CodaHaleMetricsModule.metrics()`](api/ratpack/codahale/metrics/CodaHaleMetricsModule.html#metrics\(\)), the built-in metric collectors will automatically be enabled too.

Ratpack has built-in metric collectors for:

* Request timing
* Background operation timing

Ratpack also has support for Coda Hale Metric's JVM instrumentation.
See [`CodaHaleMetricsModule.jvmMetrics()`](api/ratpack/codahale/metrics/CodaHaleMetricsModule.html#jvmMetrics\(\)) for usage information.

## Custom metrics

Ratpack enables you to capture your own application metrics in two ways:

1. Obtaining the `MetricRegistry` via dependency injection or context registry lookup and registering your own metrics with it.
2. Add metrics annotations to your Guice injected classes.

See [`CodaHaleMetricsModule.metrics()`](api/ratpack/codahale/metrics/CodaHaleMetricsModule.html#metrics\(\)) for more details.

## Own registries

Ratpack allows you to use your own registries, so you can report different metrics to different endpoints. You can inject the `SharedMetricRegistries` via Guice and use the `createOrGet()` method to use your own `MetricRegistry` instances.

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