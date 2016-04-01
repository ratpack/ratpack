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
The base dir is effectively the root of the file system for the application, providing a portable file system.
All relative paths to be resolved to files during runtime will be resolved relative to the base dir.
Static assets (e.g. images, scripts) are typically served via the base dir using relative paths.
 
The [baseDir(Path)](api/ratpack/server/ServerConfigBuilder.html#baseDir-java.nio.file.Path-) method allows setting the base dir to some known location.
In order to achieve portability across environments, if necessary, the code that calls this is responsible for determining what the base dir should be for a given runtime.

It is more common to use [BaseDir.find()](api/ratpack/server/BaseDir.html#find--) that supports finding the base dir on the classpath, providing better portability across environments.
This method searches for a resource on the classpath at the path `"/.ratpack"`. 

> To use a different path than the `/.ratpack` default, use the [BaseDir.find(String)](api/ratpack/server/BaseDir.html#find-java.lang.String-) method.

The contents of the marker file are entirely ignored.
It is just used to find the enclosing directory, which will be used as the base dir.
The file may be within a JAR that is on the classpath, or within a directory that is on the classpath.

The following example demonstrates using `BaseDir.find()` to discover the base dir from the classpath.

```language-java
import ratpack.server.ServerConfig;
import ratpack.test.embed.EphemeralBaseDir;
import ratpack.test.embed.EmbeddedApp;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

public class Example {
  public static void main(String... args) throws Exception {
    EphemeralBaseDir.tmpDir().use(baseDir -> {
      baseDir.write("mydir/.ratpack", "");
      baseDir.write("mydir/assets/message.txt", "Hello Ratpack!");
      Path mydir = baseDir.getRoot().resolve("mydir");

      ClassLoader classLoader = new URLClassLoader(new URL[]{mydir.toUri().toURL()});
      Thread.currentThread().setContextClassLoader(classLoader);

      EmbeddedApp.of(serverSpec -> serverSpec
        .serverConfig(c -> c.baseDir(mydir))
        .handlers(chain ->
          chain.files(f -> f.dir("assets"))
        )
      ).test(httpClient -> {
        String message = httpClient.getText("message.txt");
        assertEquals("Hello Ratpack!", message);
      });
    });
  }
}
```

The use of [`EphemeralBaseDir`](api/ratpack/test/embed/EphemeralBaseDir.html) and the construction of a new context class loader are in the example above are purely to make the example self contained.
A real main method would simply call `BaseDir.find()`, relying on whatever launched the Ratpack application JVM to have launched with the appropriate classpath.

Ratpack accesses the base dir via the Java 7 [Path](http://docs.oracle.com/javase/8/docs/api/java/nio/file/Path.html) API,
allowing transparent use of JAR contents as the file system.

#### Port

The [port(int)](api/ratpack/server/ServerConfigBuilder.html#port-int-) method allows setting the port used to connect to the server.
If not configured, the default value is 5050.

### Registry

A [`registry`](api/ratpack/registry/Registry.html) is a store of objects stored by type.
There may be many different registries within an application, but all applications are backed by a “server registry”.
A server registry is just the name given to the registry that backs the application and is defined at launch time.

### Handler

The server [handler](handlers.html) receives all incoming HTTP requests.
Handlers are composable, and very few applications are actually comprised of only one handler.
The server handler for most applications is a composite handler, typically created by using the [`handlers(Action)`](api/ratpack/server/RatpackServerSpec.html#handlers-ratpack.func.Action-) method,
that uses the [`Chain`](api/ratpack/handling/Chain.html) DSL to create the composite handler.

### Start and stop actions

The [`Service`](api/ratpack/service/Service.html) interface allows hooking in to the application lifecycle.
Before accepting any requests, Ratpack will notify all services and allow them to perform any initialization.
Conversely, when the application stops, Ratpack will notify all services and allow them to perform any cleanup or termination. 
