package ratpack.kotlin

/*
 * TODO Add SSL support:
 * https://ratpack.io/manual/current/api/index.html?ratpack/ssl/package-summary.html
 * https://forum.ratpack.io/Ratpack-HTTPS-td1164.html
 * https://github.com/ratpack/ratpack/blob/master/ratpack-core/src/test/groovy/ratpack/ssl/HttpsTruststoreSpec.groovy
 * https://github.com/ratpack/ratpack/blob/master/ratpack-core/src/main/java/ratpack/ssl/SSLContexts.java#L62
 */

import ratpack.server.RatpackServer

fun serverOf(cb: KServerSpec.() -> Unit) = RatpackServer.of { KServerSpec(it).(cb)() }
fun serverStart(cb: KServerSpec.() -> Unit) = RatpackServer.start { KServerSpec(it).(cb)() }
