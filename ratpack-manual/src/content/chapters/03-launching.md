# Launching

This chapter introduces how to launch a Ratpack application, and the associated launch time configuration.

## Launch configuration

To launch a Ratpack application, you must create a [`LaunchConfig`](api/ratpack/launch/LaunchConfig.html).
This can then be used with the [`RatpackServerBuilder`](api/ratpack/server/RatpackServerBuilder.html#build\(ratpack.launch.LaunchConfig\)) method to create a [`RatpackServer`](api/ratpack/server/RatpackServer.html) that can then be started.

Note that one of the `LaunchConfig` object's responsibilities is to provide a [`HandlerFactory`](api/ratpack/launch/HandlerFactory.html) implementation.
This factory is responsible for creating the handler that is effectively the Ratpack application.
See the [chapter on handlers](handlers.html) for more details.

One option for building a `LaunchConfig` is to use the [`LaunchConfigBuilder`](api/ratpack/launch/LaunchConfig.html).
Another option is to use the [`LaunchConfigFactory`](api/ratpack/launch/LaunchConfigFactory.html) which is able to build a launch config from system properties and a properties file.

## Main classes

Ratpack also provides some ready to use “main” classes that can be used to start the application.
These main classes build on top of `LaunchConfigFactory`.

The [`RatpackMain`](api/ratpack/launch/RatpackMain.html) is a bare bones entry point and is suitable for use in most cases.

The [`GroovyRatpackMain`](api/ratpack/groovy/launch/GroovyRatpackMain.html) entry point configures a Groovy based application to be launched.