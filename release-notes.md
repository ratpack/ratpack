<!--
This file contains the in progress release notes during the cycle.
It should not be considered the final announcement for any release at any time.
-->

Finally… the Ratpack 1.0.0 release candidate.
It's been a two and half year journey, but we are finally here.
We plan to release 1.0.0 _final_ roughly two weeks after the RC release, depending on the feedback.

First off, thank you to everyone who has supported the project in any way over the recent years.
Ratpack is a volunteer effort; no one gets paid to work on it and no one gets paid by people using it.
We've received many pull requests, bug reports, feature suggestions and praise which provide the motivation to keep going.
You can see the list of people who have contributed code to the project in the [credits section of the manual](http://ratpack.io/manual/1.0.0/about-the-project.html#credits).

There are quite a few changes in this release from 0.9.19.

The development time reloading strategy has changed from using runtime class patching (using SpringLoaded), to using [Gradle's Continuous Build feature](https://docs.gradle.org/current/userguide/continuous_build.html).
This is a far more reliable strategy and greatly simplifies IDE integration for Ratpack apps (i.e. no special integration is needed).
Try it out by running `./gradlew run --continuous` on your project, after upgrading to Gradle 2.6 (Ratpack now requires this version).
The Ratpack runtime has no knowledge of Gradle.
It is entirely theoretically possible to integrate Ratpack with the equivalent feature in another build tool should it exist.

The request body can now only be read once during a request.
Its byte buffer can now also be eagerly released to free the memory if desired.
This change is in preparation for the coming feature in Ratpack 1.1.0 of deferring the reading of the body over the wire until it is requested.  
 
The `ratpack-jackson-guice` library has been removed.
The functionality that it provided is now part of `ratpack-core`.
That is, renderers and parsers for working with the types of [`Jackson`](http://ratpack.io/manual/1.0.0/api/ratpack/jackson/Jackson.html) are now available automatically.
The `ratpack-core` library already depended on Jackson for the [config mechanism](http://ratpack.io/manual/1.0.0/config.html).
Besides the removal of the library and the `JacksonModule` class, the main change you will need to make is how you customise Jackson.
See [“Configuring Jackson”](http://ratpack.io/manual/1.0.0/jackson.html#configuring_jackson).

The [`Parser`](http://ratpack.io/manual/1.0.0/api/ratpack/parse/Parser.html) API has also simplified.
For parse operations with no options, the parse objects now returns no options instead of a `NullParseOpts` marker.
Also, parsers are no longer restricted to a content type.
An implementation can now potentially parse multiple content types.

[`RemoteControl`](http://ratpack.io/manual/1.0.0/api/ratpack/test/remote/RemoteControl.html) now accepts `UnserializableResultStrategy`.
The default value is `io.remotecontrol.client.UnserializableResultStrategy.throw`.
It is now also possible to specify `uses` closures (i.e. external closures that are used by the command closure) as well. 

`ReceivedResponse#send` and `StreamedResponse#send` have been changed to `ReceivedResponse#forwardTo` and `StreamedResponse#forwardTo`

Redis session storage is now supported in the `ratpack-session-redis` library. See ["RedisSessionModule"](http://ratpack.io/manual/1.0.0/api/index.html?ratpack/session/store/RedisSessionModule.html) for details.

The `ratpack-codahale-metrics` module has been renamed to `ratpack-dropwizard-metrics`.  
The configuration classes associated with this module are no longer inner classes of the module, you will need to use `ratpack.dropwizard.metrics.DropwizardMetricsConfig`

The 1.0.0 release doesn't mean that Ratpack is “done”.
There are still things to add, and no doubt bugs to squash, but it's both a symbolic and practical milestone.
It's symbolic in that we are asserting that it's at a certain level of maturity and is applicable to a good number of projects.
It's practical in that we are now striving for API compatibility (i.e. no more breaking changes).
What this means is that you should be able to _upgrade_ your app on the 1.x line in order to incorporate new features and fixes without your existing code breaking.
We will be taking this very seriously, but of course we are mere humans and uncaught mistakes may be made.

1.0.x releases will occur as frequently as needed in response to bug fixes and internal improvements.
After the release of 1.0.0, improving the documentation will be a key focus.

Please try the release candidate on your projects promptly and let us know of any problems so they can be fixed before the 1.0.0 final release.

We hope you are as excited as we are about Ratpack 1.0.0.

-- 

**Team Ratpack**
