<!-- 
This file contains the in progress release notes during the cycle.
It should not be considered the final announcement for any release at any time. 
-->

Coming off the back of the monstrous 0.9.13 release, and given a short month, this release is a little lighter than the last few.
However, there's good stuff in 0.9.14.

The Guice integration has changed in this release to be better integrated with the new server registry mechanism introduced in the previous.
Objects bound with Guice are now part of the server registry.
This is mostly a transparent change to users, but will make it easier to leverage more features going forward.

As of this release Ratpack now uses Groovy 2.4.1.
This Groovy version fixes several issues with static compilation that would often surface for Ratpack applications.

There are also fixes and improvements for Metrics support, config loading, reactive streams and more.

Thanks to [Robert Zakrzewski](https://github.com/zedar) for contributing [Slf4j MDC](http://www.slf4j.org/apidocs/org/slf4j/MDC.html) support.
This integrates the MDC concept with Ratpack's execution model.
See [`MDCInterceptor`](api/ratpack/logging/MDCInterceptor.html) for details.

We hope you enjoy this release, and are looking forward to the next one as much as we are.

-- Team Ratpack.
