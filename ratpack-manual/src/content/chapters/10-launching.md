# Launching

This chapter introduces how to launch a Ratpack application, and the associated launch time configuration.

## Configuring and starting a Ratpack application

A Ratpack application is configured and started via the [`RatpackServer`](api/ratpack/server/RatpackServer.html). The 
[`of`](api/ratpack/server/RatpackServer.html#of-ratpack.func.Function-) method is passed a function that takes an instance of a [`Builder`](api/ratpack/server/RatpackServer.Definition.Builder.html) 
that is used to configure settings for the application a returns a [`Definition`](api/ratpack/server/RatpackServer.Definition.html).

The builder provides access to configure the user [`Registry`](api/ratpack/registry/Registry.html) that is used to configure the Ratpack application via 
the [`registry`](api/ratpack/server/RatpackServer.Definition.Builder.html#registry-ratpack.func.Action-) method. The builder adds the necessary default objects to the registry 
before building the server instance. Objects in the user registry will supersede the default items.

Calling the [`build`](api/ratpack/server/RatpackServer.Definition.Builder.html#build-ratpack.func.Function-) will construct a [`RatpackServer`](api/ratpack/server/RatpackServer.html) that can be started.
The function passed to the `build` method is responsible for creating the handler that is effectively the Ratpack application.
See the [chapter on handlers](handlers.html) for more details.

The Ratpack application can be customized by configuring the `ServerConfig` object used to create the server. 
This instance can be constructed using the [`ServerConfigBuilder`](api/ratpack/server/ServerConfigBuilder.html) and is passed to the 
[`config`](api/ratpack/server/RatpackServer.Definition.Builder.html#config-ratpack.server.ServerConfig-) method on the builder.

```language-java
import ratpack.handling.Handler;
import ratpack.handling.Context;
import ratpack.server.ServerConfig;
import ratpack.server.RatpackServer;

public class ApplicationMain {
    public static void main(String[] args) {
        RatpackServer server = RatpackServer.of(spec -> spec
          .config(ServerConfig.noBaseDir().port(6060).build())
          .build(r -> new HelloWorld()));
    }
    
    private static class HelloWorld implements Handler {
        public void handle(Context context) {
            context.getResponse().send("Hello world!");
        }
    }
}
```

If no server configuration is provided via the `config` method the application will be configured with default values.

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
