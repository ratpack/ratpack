<!--
This file contains the in progress release notes during the cycle.
It should not be considered the final announcement for any release at any time.
-->

# v1.6.0
* Upgrade to Jackson 2.9.5
* Upgrade to Caffeine 2.6.2
* Upgrade to Netty 4.1.23.Final
* Upgrade to Snake Yaml 1.20
* `ServerConfigBuilder.registerShutdownHook(boolean)` defaults to `true`
* `HttpClient` request/response interceptors
* Expose `HttpClient` pool queue size
* `Promise.flatOp`
* `Promise.mapError(Predicate,Function)` 
* `RatpackServer.getRegistry()`
* Fix for URL paths when serving files
* Configure `HttpClient` for Retrofit builders
* Check for null content-type in retrofit request
* `ByteBuf` metrics in `DropwizardMetricsMetrics`
