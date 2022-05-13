# Launching

This chapter describes how Ratpack applications are started, effectively detailing the entry points to the Ratpack API.

## RatpackServer

The [`RatpackServer`](api/ratpack/core/server/RatpackServer.html) type is the Ratpack entry point.
You write your own main class that uses this API to launch the application.
 
```language-java hello-world
package my.app;

import ratpack.core.server.RatpackServer;
import ratpack.core.server.ServerConfig;
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
The function takes a [`RatpackServerSpec`](api/ratpack/core/server/RatpackServerSpec.html) which can be used to specify the three fundamental aspects of Ratpack apps (i.e. server config, base registry, root handler).

> Most examples in this manual and the API reference use [`EmbeddedApp`](api/ratpack/test/embed/EmbeddedApp.html) instead of `RatpackServer` to create applications.
> This is due to the “testing” nature of the examples.
> Please see [this section](intro.html#code_samples) for more information regarding the code samples.

### Server Config

The [`ServerConfig`](api/ratpack/core/server/ServerConfig.html) defines the configuration settings that are needed in order to start the server.
The static methods of `ServerConfig` can be used to create instances.
 
#### Base dir

An important aspect of the server config is the [base dir](api/ratpack/core/server/ServerConfig.html#getBaseDir%28%29).
The base dir is effectively the root of the file system for the application, providing a portable file system.
All relative paths to be resolved to files during runtime will be resolved relative to the base dir.
Static assets (e.g. images, scripts) are typically served via the base dir using relative paths.
 
The [baseDir(Path)](api/ratpack/core/server/ServerConfigBuilder.html#baseDir%28java.nio.file.Path%29) method allows setting the base dir to some known location.
In order to achieve portability across environments, if necessary, the code that calls this is responsible for determining what the base dir should be for a given runtime.

It is more common to use [BaseDir.find()](api/ratpack/core/server/BaseDir.html#find%28%29) that supports finding the base dir on the classpath, providing better portability across environments.
This method searches for a resource on the classpath at the path `"/.ratpack"`. 

> To use a different path than the `/.ratpack` default, use the [BaseDir.find(String)](api/ratpack/core/server/BaseDir.html#find%28java.lang.String%29) method.

The contents of the marker file are entirely ignored.
It is just used to find the enclosing directory, which will be used as the base dir.
The file may be within a JAR that is on the classpath, or within a directory that is on the classpath.

The following example demonstrates using `BaseDir.find()` to discover the base dir from the classpath.

```language-java
import ratpack.core.server.ServerConfig;
import ratpack.test.embed.EphemeralBaseDir;
import ratpack.test.embed.EmbeddedApp;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

The [port(int)](api/ratpack/core/server/ServerConfigBuilder.html#port%28int%29) method allows setting the port used to connect to the server.
If not configured, the default value is 5050.

#### SSL

By default, the Ratpack server will listen for HTTP traffic on the configuration port.
To enable HTTPS traffic, the [ssl(SslContext)](api/ratpack/core/server/ServerConfigBuilder.html#ssl%28io.netty.handler.ssl.SslContext%29) method allows for the SSL certificate and key.

As of v2.0, Ratpack also supports selecting SSL configurations based on the requested host using Server Name Indicators (SNI).
The [ssl(SslContext, Action)](api/ratpack/core/server/ServerConfigBuilder.html#ssl%28io.netty.handler.ssl.SslContext,ratpack.func.Action%29) is used to specify the default SSL configuration and any additional domain mappings with alternative SSL configuration.
The domains specified in the mapping support [DNS Wildcard](https://tools.ietf.org/search/rfc6125#section-6.4) and will match at most one level deep in the domain hierarchy (e.g. `*.ratpack.io` will match `api.ratpack.io` but not `docs.api.ratpack.io`).

Configuring SSL settings via system properties or environment variables requires special handling to specify the domain names.
The following table shows how to specify the default SSl configuration and the configuration for subdomains.

| System Property                                | Environment Variable                             | Description                                                                     |
|------------------------------------------------|--------------------------------------------------|---------------------------------------------------------------------------------|
| `ratpack.server.ssl.keystoreFile`              | `RATPACK_SERVER__SSL__KEYSTORE_FILE`             | Specifies the path to the JKS containing the server certificate and private key |
| `ratpack.server.ssl.keystorePassword`          | `RATPACK_SERVER__SSL__KEYSTORE_PASSWORD`         | Specifies the password for the keystore JKS                                     |
| `ratpack.server.ssl.truststoreFile`            | `RATPACK_SERVER__SSL__TRUSTSTORE_FILE`           | Specifies the path to the JKS containing the trusted certificates               |
| `ratpack.server.ssl.truststorePassword`        | `RATPACK_SERVER__SSL__TRUSTSTORE_PASSWORD`       | Specifies the password for the truststore JKS                                   |
| `ratpack.server.ssl.ratpack_io.keystoreFile`   | `RATPACK_SERVER__SSL__RATPACK_IO__KEYSTORE_FILE` | Specifies the path to the keystore for the domain `ratpack.io`                  |
| `ratpack.server.ssl.*_ratpack_io.kyestoreFile` | `RATPACK_SERVER__SSL___RATPACK_IO_KEYSTORE_FILE` | Specifies the path to the keystore for the domain `*.ratpack.io`                |

Note the following special rules:

1. In both system properties and environment variables, domain name separators (`.`) are converted into underscores (`_`)
2. In environment variables, the domain wildcard character (`*`) is specified using an underscore (`_`). This results in 3 underscores preceding the domain name (`___RATPACK_IO`).

### Registry

A [`registry`](api/ratpack/exec/registry/Registry.html) is a store of objects stored by type.
There may be many different registries within an application, but all applications are backed by a “server registry”.
A server registry is just the name given to the registry that backs the application and is defined at launch time.

### Handler

The server [handler](handlers.html) receives all incoming HTTP requests.
Handlers are composable, and very few applications are actually comprised of only one handler.
The server handler for most applications is a composite handler, typically created by using the [`handlers(Action)`](api/ratpack/core/server/RatpackServerSpec.html#handlers%28ratpack.func.Action%29) method,
that uses the [`Chain`](api/ratpack/core/handling/Chain.html) DSL to create the composite handler.

### Start and stop actions

The [`Service`](api/ratpack/core/service/Service.html) interface allows hooking in to the application lifecycle.
Before accepting any requests, Ratpack will notify all services and allow them to perform any initialization.
Conversely, when the application stops, Ratpack will notify all services and allow them to perform any cleanup or termination. 
