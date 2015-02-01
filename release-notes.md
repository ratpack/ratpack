<!-- 
This file contains the in progress release notes during the cycle.
It should not be considered the final announcement for any release at any time. 
-->

Starting 2015 with a bang, this release is probably the most significant to date.

Before jumping into why that is, some explanation is necessary as to why we have now shipped two releases depending on `4.1.0.Beta4-_SNAPSHOT_`.
Late during the 0.9.11 release we hit problems with the Netty 4.0.x version we were using.
While we could have worked around this problem, one way to resolve it was to upgrade to Netty 4.1.0-Beta4 which was not yet in final release.
The intention from the Netty team was to finalise this release before January 1st, before 0.9.12 was released.
We decided to upgrade and use the snapshot, banking on the final release being out before ours.
We find ourselves in the same situation for the 0.9.13 release.
If you're having issues due to this in your build, you can find some info on [this issue](https://github.com/ratpack/ratpack/issues/549).

The big story in this release is the complete rewrite in Ratpack's “launch” layer, and what this has enabled.
That is, how Ratpack applications are started has completely changed.
For most users of the existing Groovy entry point (i.e. what is set up by the `ratpack-groovy` Gradle plugin) not much will appear to have changed.
For Java users, this is a breaking change to your main class and how you started the application.
`LaunchConfig`, `HandlerFactory` and other types/concept previously used at launch are now gone.
Instead, the [`RatpackServer`](api/ratpack/server/RatpackServer.html) is the new entry point.
Groovy users can continue to use [`GroovyRatpackMain`](api/ratpack/groovy/GroovyRatpackMain.html) as the entry point (as setup by the `ratpack-groovy` Gradle plugin).

One of the fundamental changes at here is introducing a “server registry” into the definition of an application alongside the existing handler chain.
The “server registry” is the [`Registry`](api/ratpack/registry/Registry.html) that underpins the whole application.
Previously, registries were only defined inside the request handling layer.
This made it difficult to integrate with Ratpack on an application level, as the request handling layer effectively has a different lifecycle.
Now, the server registry is part of the base definition of the application and is tied to its lifecycle.
This makes it much easier to support background jobs, startup actions, shutdown actions, interceptors and much more.
Expect to see features in new releases enabled by this as well as improved API for existing features.
In this release, the new [`Service`](api/ratpack/server/Service.html) interface allows objects to be notified of server start and stop. 

Another driver for the change was the desire to have really great support for externalised configuration.
This is something we've been working on for some time.
This release introduces the `ratpack-config` library.
This library makes it easy to create Java objects based on configuration data sourced from JSON, YAML and properties files as well as JVM system properties and environment variables.
While not part of `ratpack-core`, use of `ratpack-config` will be strongly recommended going forward as it makes it very easy to externalise your configuration.
The documentation on the new configruation mechanism is a little sparse right now, but we are working on it.
In the meantime, you can start with the documentation for [`ConfigData`](api/ratpack/config/ConfigData.html).
We'll be following up with blog posts and more docs over the 0.9.14 development cycle.

The reworking of the launching mechanism was also undertaken to improve Ratpack's support for changing the application without redeploy while in development.
Expect to see improvements in this area in the next release.

Thanks to [Kyle Boon](https://github.com/kyleboon), [Jeff Blaisdell](https://github.com/jeff-blaisdell), [Glenn Saqui](https://github.com/gsaqui) and [Joe Kutner](https://github.com/jkutner) for contributions to this release.

We hope you enjoy this release, and are looking forward to the next one as much as we are.

-- Team Ratpack.

