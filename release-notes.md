<!--
This file contains the in progress release notes during the cycle.
It should not be considered the final announcement for any release at any time.
-->


# v2.0.0
## Module removals
* `ratpack-base`

## Package relocations
* `ratpack.api` → `ratpack.exec.api`
* `ratpack.func` → `ratpack.exec.func`
* `ratpack.registry` → `ratpack.exec.registry`
* `ratpack.stream` → `ratpack.exec.stream`
* `ratpack.util` → `ratpack.exec.util`

## TODOs
* `SiteMain` had to inline ratpack-asset-pipeline code to handle package renames.
* `ratpack-asset-pipeline` version in `ratpack-site` is very old, but not a problem due to above TODO.
