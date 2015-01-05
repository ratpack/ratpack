# Launching

This chapter introduces how to launch a Ratpack application, and the associated launch time configuration.

## Configuring and starting a Ratpack application

A Ratpack application is configured and started via the [`RatpackLauncher`](api/ratpack/launch/RatpackLauncher.html). The 
[`with`][api/ratpack/launch/RatpackLauncher.html#with-ratpack.server.ServerConfig`] methods creates an instance of the launcher that is backed with the provided server configuration. 

The launcher then provides access to configure the user [`Registry`][api/ratpack/registry/Registry.html] that is used to configure the Ratpack application via 
the [`bindings`][api/ratpack/launch/RatpackLauncher.html#bindings-ratpack.func.Function] method. The launcher adds the necessary default objects to the registry 
before building the server instance. Objects in the user registry will take supersede the defaults items.

Calling the [`build`](api/ratpack/launch/RatpackLauncher.html#build-ratpack.launch.HandlerFactory] will construct a [`RatpackServer`][api/ratpack/server/RatpackServer.html] that can be started.
Note that the `build` method accepts a [`HandlerFactory`][api/ratpack/launch/HandlerFactory.html]. This factory is responsible for create the handler that is effectively the Ratpack application.
See the [chapter on handlers](handlers.html) for more details.

The Ratpack application can be customized by configuring the `ServerConfig` object used to create the launcher. This instance can be constructed using the [`ServerConfigBuilder`][api/ratpack/launch/ServerConfigBuilder.html].
For example, 

```language-java
import ratpack.handling.Handler;
import ratpack.handling.Context;
import ratpack.server.ServerConfig;
import ratpack.server.RatpackServer;

public class ApplicationMain {
    public static void main(String[] args) {
        RatpackServer server = RatpackServer.with(ServerConfig.noBaseDir().port(6060).build())
          .build(r -> new HelloWorld());
    }
    
    private static class HelloWorld implements Handler {
        public void handle(Context context) {
            context.getResponse().send("Hellow world!");
        }
    }
}
```

Alternatively, if you wish to use the default server configuration for your application, call the [`withDefaults`][api/ratpack/launch/RatpackLauncher.html#withDefaults] method.

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
