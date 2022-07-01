<!--
This file contains the in progress release notes during the cycle.
It should not be considered the final announcement for any release at any time.
-->
* Add support for basic HTTP proxy authentication

This release contains an important change to [`HttpClient`](https://ratpack.io/manual/current/api/ratpack/http/client/HttpClient.html), that is potentially breaking for some non standard usages.
Netty's DNS resolver is now used by instead of the JDK's resolver.
If you create a `HttpClient` yourself, you _may_ need to use the new [`execController()`](https://ratpack.io/manual/current/api/ratpack/http/client/HttpClientSpec.html#execController(ratpack.exec.ExecController)) method to
avoid the following error:

> Cannot build addressResolver as HttpClient is built on non managed thread, and execController not specified. Use HttpClientSpec.execController() or HttpClientSpec.useJdkAddressResolver().

If you build your client as part of a Guice module, or during the execution of your app, no changes are required.
