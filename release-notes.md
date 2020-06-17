<!--
This file contains the in progress release notes during the cycle.
It should not be considered the final announcement for any release at any time.
-->


# v2.0.0
## Module removals
* `ratpack-base`

## Package relocations
* `ratpack.api` → `ratpack.exec.api`
* `ratpack.config` → `ratpack.core.config`
* `ratpack.error` → `ratpack.core.error`
* `ratpack.file` → `ratpack.core.file`
* `ratpack.form` → `ratpack.core.form`
* `ratpack.func` → `ratpack.exec.func`
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

## TODOs
* `SiteMain` had to inline ratpack-asset-pipeline code to handle package renames.
* `ratpack-asset-pipeline` version in `ratpack-site` is very old, but not a problem due to above TODO.
* Remove handling for `ratpack.core.server.LaunchException`
