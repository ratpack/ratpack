# Launching

This chapter describes how Ratpack applications are started, effectively detailing the entry points to the Ratpack API.

## RatpackServer

The [`RatpackServer`](api/ratpack/server/RatpackServer.html) type is the Ratpack entry point.
You write your own main class that uses this API to launch the application.
 
```language-java hello-world
package my.app;

import ratpack.server.RatpackServer;
import ratpack.server.ServerConfig;
import java.net.URI;

public class Main {
  public static void main(String... args) throws Exception {
    RatpackServer.start(server -> server
      .serverConfig(ServerConfig.embedded().publicAddress(new URI("http://company.org")))
      .registryOf(registry -> registry.add("World!"))
      .handlers(chain -> chain
        .get(ctx -> ctx.render("Hello " + ctx.get(String.class)))
        .get(":name", ctx -> ctx.render("Hello " + ctx.getPathTokens().get("name") + "!"))     
      )
    );
  }
}
```

Applications are defined as the function given to the `of()` or `start()` static methods of this interface.
The function takes a [`RatpackServerSpec`](api/ratpack/server/RatpackServerSpec.html) which can be used to specify the three fundamental aspects of Ratpack apps (i.e. server config, base registry, root handler).

> Most examples in this manual and the API reference use [`EmbeddedApp`](api/ratpack/test/embed/EmbeddedApp.html) instead of `RatpackServer` to create applications.
> This is due to the “testing” nature of the examples.
> Please see [this section](intro.html#code_samples) for more information regarding the code samples.

### Server Config

The [`ServerConfig`](api/ratpack/server/ServerConfig.html) defines the configuration settings that are needed in order to start the server.
The static methods of `ServerConfig` can be used to create instances.
 
#### Base dir

An important aspect of the server config is the [base dir](api/ratpack/server/ServerConfig.html#getBaseDir--).
The base dir is effectively the root of the file system for the application.
All relative paths will be resolved via the base dir.
Static assets (e.g. images, scripts) are typically served via the base dir.

### Registry

A [`registry`](api/ratpack/registry/Registry.html) is a store of objects stored by type.
There may be many different registries within an application, but all applications are backed a “server registry”.
A server registry is just the name given to the registry that backs the application and is defined at launch time.

### Handler

The server [handler](handlers.html) receives all incoming HTTP requests.
Handlers are composable, and very few applications are actually comprised of only one handler.
The server handler for most applications is a composite handler, typically created by using the [`handlers(Action)`](api/ratpack/server/RatpackServerSpec.html#handlers-ratpack.func.Action-) method,
that uses the [`Chain`](api/ratpack/handling/Chain.html) DSL to create the composite handler.
