<!-- 
This file contains the in progress release notes during the cycle.
It should not be considered the final announcement for any release at any time. 
-->

Finally… no more Netty snapshot dependencies.
Netty 4.1b4 final was released during this cycle so there is no longer any need to add snapshot repositories to your build or any such nastiness.
We apologise again for the inconvenience this caused.

Beyond this glorious news, there's some cool stuff in this release.

Ratpack's [`Promise`](api/ratpack/exec/Promise.html) type has been simplified.
What was previously 3 different types has been collapsed to just `Promise`.
This generally makes the API simpler to understand and easier to use as a return value in your API.
New operations have also been added such as `apply()` and `mapError()`.
The implementation of promises underwent some optimization and now creates fewer interim objects.

It's now possible to get hold of a [global execution control](api/ratpack/exec/ExecControl.html#execControl--) that binds to the current execution on demand.
This makes it potentially more convenient to perform async ops outside of the handler layer by removing the need to inject or pass in an exec control.
We all know that global statics are problematic in many cases, a prominent one being testing.
Ratpack's [`ExecControl`](api/ratpack/exec/ExecControl.html) is not something you are going to mock or double at test time.
Instead, you can use the [`ExecHarness`](api/ratpack/test/exec/ExecHarness.html) to unit test async code.
The global execution control will work just fine with an exec harness.

The methods of the [RxJava](api/ratpack/rx/RxRatpack.html) and [Reactive Streams](api/ratpack/stream/Streams.html) types are now installed as [Groovy extension methods](http://mrhaki.blogspot.com.au/2013/01/groovy-goodness-adding-extra-methods.html).
This makes working with RxJava and Reactive Streams more convenient when using Groovy.

The methods of the [RxJava integration](api/ratpack/rx/RxRatpack.html) has been revamped and simplified in this release.

Ratpack now compresses responses by default if the client requested compression.
To suppress compression, you can call the [`response.noCompress()`](api/ratpack/http/Response.html#noCompress--) before sending the response.

Along with steady general improvements to the docs, significant progress has been made on the [documentation for the `ratpack-config` module](config.html) introduced some releases back.

The [launching API](api/ratpack/server/RatpackServer.html#of-ratpack.func.Action-) has been greatly simplified in this release to use fewer types and generally be more friendly. 

[Glen Schrader](https://github.com/gschrader) contributed improvements to the Coda Hale Metrics integration.

[Robert Zakrzewski](https://github.com/zedar) contributed support for [asynchronous health checks](api/ratpack/health/HealthCheck.html) in core.

[Jason Winnebeck](https://github.com/gillius) contributed some documentation for Ratpack's WebSocket support.

We hope you enjoy this Ratpack release.

-- **Team Ratpack**
