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
* rxjava2 2.1.2 → 2.2.
* reactor 3.1.8.RELEASE → 3.3.6.RELEASE
* reactiveStreams 1.0.2 → 1.0.3

## TODOs
* `SiteMain` had to inline ratpack-asset-pipeline code to handle package renames.
* `ratpack-asset-pipeline` version in `ratpack-site` is very old, but not a problem due to above TODO.
* Remove handling for `ratpack.core.server.LaunchException`
