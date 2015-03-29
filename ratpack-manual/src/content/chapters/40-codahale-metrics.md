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
When metrics are enabled within your application using [`CodaHaleMetricsModule`](api/ratpack/codahale/metrics/CodaHaleMetricsModule.Config.html), the built-in metric collectors will automatically be enabled too.

Ratpack has built-in metric collectors for:

* Request timing
* Background operation timing

Ratpack also has support for Coda Hale Metric's JVM instrumentation.
See [`CodaHaleMetricsModule.Config.jvmMetrics(boolean)`](api/ratpack/codahale/metrics/CodaHaleMetricsModule.Config.html#jvmMetrics-boolean-) for usage information.

## Custom metrics

Ratpack enables you to capture your own application metrics in two ways:

1. Obtaining the `MetricRegistry` via dependency injection or context registry lookup and registering your own metrics with it.
2. Add metrics annotations to your Guice injected classes.

See [`CodaHaleMetricsModule`](api/ratpack/codahale/metrics/CodaHaleMetricsModule.html) for more details.

## Reporting metrics

Ratpack supports metric reporters for the following outputs:

* [JMX](api/ratpack/codahale/metrics/CodaHaleMetricsModule.Config.html#jmx-ratpack.func.Action-)
* [Console](api/ratpack/codahale/metrics/CodaHaleMetricsModule.Config.html#console-ratpack.func.Action-)
* [CSV](api/ratpack/codahale/metrics/CodaHaleMetricsModule.Config.html#csv-ratpack.func.Action-)
* [Websockets](api/ratpack/codahale/metrics/CodaHaleMetricsModule.Config.html#webSocket-ratpack.func.Action-)

For an example of how to consume real-time metrics with websockets, see the [example-books](https://github.com/ratpack/example-books/blob/master/src/ratpack/ratpack.groovy) project.
