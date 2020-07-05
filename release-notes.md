<!--
This file contains the in progress release notes during the cycle.
It should not be considered the final announcement for any release at any time.
-->


# v2.0.0
## New modules
* `ratpack-config`
* `ratpack-func`

## Module removals
* `ratpack-base` (collapsed into `ratpack-exec`)
* `ratpack-hystrix` (Hystrix is no longer maintained, relies on `ratpack-rx` which is removed)
* `ratpack-pac4j` (in favor of https://github.com/pac4j/ratpack-pac4j)
* `ratpack-remote` (obsolete due to additions of `Imposition` and `RatpackServer.getRegistry()`, URLClassLoader no longer supported in Java > 9)
* `ratpack-remote-test` (obsolete due to additions of `Imposition` and `RatpackServer.getRegistry()`, URLClassLoader no longer supported in Java > 9)
* `ratpack-rx` (in favor of `ratpack-rx2`)
* `ratpack-thymeleaf` (in favor of `ratpack-thymeleaf3`)

## Package relocations
* `ratpack.api` → `ratpack.exec.api`
* `ratpack.config` → moved to `ratpack-config` module
* `ratpack.error` → `ratpack.core.error`
* `ratpack.file` → `ratpack.core.file`
* `ratpack.form` → `ratpack.core.form`
* `ratpack.func` → moved to `ratpack-func` module
* `ratpack.handling` → `ratpack.core.handling`
* `ratpack.health` → `ratpack.core.health`
* `ratpack.http` → `ratpack.core.http`
* `ratpack.impose` → `ratpack.core.impose`
* `ratpack.jackson` → `ratpack.core.jackson`
* `ratpack.logging` → `ratpack.core.logging`
* `ratpack.parse` → `ratpack.core.parse`
* `ratpack.path` → `ratpack.core.path`
* `ratpack.registry` → `ratpack.exec.registry`
* `ratpack.reload` → `ratpack.core.reload`
* `ratpack.render` → `ratpack.core.render`
* `ratpack.server` → `ratpack.core.server`
* `ratpack.service` → `ratpack.core.service`
* `ratpack.sse` → `ratpack.core.sse`
* `ratpack.ssl` → `ratpack.core.ssl`
* `ratpack.stream` → `ratpack.exec.stream`
* `ratpack.util` → `ratpack.exec.util`
* `ratpack.websocket` → `ratpack.core.websocket`

## Dependecy updates
* netty 4.1.48.FINAL → 4.1.50.FINAL
* netty-tcnative 2.0.30.FINAL → 2.0.31.FINAL
* guava 28.2-jre → 29.0-jre
* rxjava2 2.1.2 → 2.2.19
* reactor 3.1.8.RELEASE → 3.3.7.RELEASE
* jackson 2.10.3 → 2.11.1
* dropwizard metrics 4.1.6 → 4.1.9
* pegdown 1.5.0 → 1.6.0
* log4j 2.13.1 → 2.13.3
* newrelic 3.15.0 → 5.13.0
* reactiveStreams 1.0.2 → 1.0.3
* guice 4.1.0 → 4.2.3
* caffeine 2.8.1 → 2.8.5
* retrofit 2.8.1 → 2.9.0
* commons-codec 1.10 → 1.14
* snakeyaml 1.23 → 1.26
* commons-lang3 3.3.2 → 3.10


## TODOs
* `SiteMain` had to inline ratpack-asset-pipeline code to handle package renames.
* `ratpack-asset-pipeline` version in `ratpack-site` is very old, but not a problem due to above TODO.
* Remove handling for `ratpack.core.server.LaunchException`
