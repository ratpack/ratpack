# Launching

This chapter introduces how to launch a Ratpack application, and the associated launch time configuration.

## Launch configuration

To launch a Ratpack application, you must create a [`LaunchConfig`](api/ratpack/launch/LaunchConfig.html).
This can then be used with the [`RatpackServerBuilder`](api/ratpack/server/RatpackServerBuilder.html#build\(ratpack.launch.LaunchConfig\)) method to create a [`RatpackServer`](api/ratpack/server/RatpackServer.html) that can then be started.

Note that one of the `LaunchConfig` object's responsibilities is to provide a [`HandlerFactory`](api/ratpack/launch/HandlerFactory.html) implementation.
This factory is responsible for creating the handler that is effectively the Ratpack application.
See the [chapter on handlers](handlers.html) for more details.

One option for building a `LaunchConfig` is to use the [`LaunchConfigBuilder`](api/ratpack/launch/LaunchConfigBuilder.html).
Another option is to use [`LaunchConfigs`](api/ratpack/launch/LaunchConfigs.html) which is able to build a launch config from system properties and a properties file.

## RatpackMain

The [`RatpackMain`](api/ratpack/launch/RatpackMain.html) class is a convenient entry point (i.e. main class) that bootstraps a Ratpack application.
When [building with Gradle](gradle.html), this is the default main class.

The minimum requirement for launching a `RatpackMain` based application, is to ensure there is a `ratpack.properties` file on the JVM classpath that indicates the application [`HandlerFactory`](api/ratpack/launch/HandlerFactory.html)…

```
handlerFactory=org.my.HandlerFactory
```

All aspects of the LaunchConfig can be specified in this properties file.
Aspects can also be environmentally overridden by JVM system properties.
See [`RatpackMain`](api/ratpack/launch/RatpackMain.html) for details.