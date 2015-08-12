<!--
This file contains the in progress release notes during the cycle.
It should not be considered the final announcement for any release at any time.
-->

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

Redis session storage is now supported in the `ratpack-session-redis` library. See ["RedisSessionModule"](http://ratpack.io/manual/1.0.0/api/index.html?ratpack/session/store/RedisSessionModule.html) for details.
