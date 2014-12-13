# Launching

This chapter introduces how to launch a Ratpack application, and the associated launch time configuration.

## Configuring and starting a Ratpack application

A Ratpack application is configured and started via the [`RatpackLauncher`](api/ratpack/launch/RatpackLauncher.html). The [`launcher`][api/ratpack/launch/RatpackLauncher.html#launcher-ratpack.func.Action`]
provides access to the base [`Registry`][api/ratpack/registry/Registry.html] that is used to configure the Ratpack application.

The base registry must provide the [`ServerConfig`][api/ratpack/launch/ServerConfig.html], the [`ExecController`][api/ratpack/exec/ExecController.html], and the default Netty `ByteBufAllocator`. 
If not provided, defaults will be added to the Registry. All subsequent Registries will inherit from this base registry.

The `launcher` method will return an instance of `RatpackLauncher`. This instance provides the [`config`][api/ratpack/launch/RatpackLauncher.html#config-ratpack.func.action] convenience method to
configure the default `ServerConfig`.

Calling the [`build`](api/ratpack/launch/RatpackLauncher.html#build-ratpack.launch.HandlerFactory] will construct a [`RatpackServer`][api/ratpack/server/RatpackServer.html] that can be started.
Note that the `build` method accepts a [`HandlerFactory`][api/ratpack/launch/HandlerFactory.html]. This factory is responsible for create the handler that is effectively the Ratpack application.
See the [chapter on handlers](handlers.html) for more details.

The Ratpack application can be customized by adding a custom `ServerConfig` object to the `Registry. This instance can be constructed using the [`ServerConfigBuilder`][api/ratpack/launch/ServerConfigBuilder.html].
For example, 

```language-java
import ratpack.handling.Handler;
import ratpack.handling.Context;
import ratpack.launch.RatpackLauncher;
import ratpack.launch.ServerConfig;
import ratpack.launch.ServerConfigBuilder;
import ratpack.server.RatpackServer;

public class ApplicationMain {
    public static void main(String[] args) {
        RatpackServer server = RatpackLauncher.launcher(r -> {
            ServerConfig config = ServerConfigBuilder.noBaseDir().port(6060).build();
            r.add(ServerConfig.class, config);
        }).build(r -> new HelloWorld());
    }
    
    private static class HelloWorld implements Handler {
        public void handle(Context context) {
            context.getResponse().send("Hellow world!");
        }
    }
}
```

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
