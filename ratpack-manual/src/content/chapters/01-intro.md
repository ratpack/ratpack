# Introduction

Ratpack is a set of Java libraries that facilitate fast, efficient, evolvable and well tested HTTP applications.
It is built on the highly performant and efficient Netty event-driven networking engine.

Ratpack is purely a runtime.
There is no installable package and no coupled build tooling (e.g. Rails, Play, Grails).
To build Ratpack applications, you can use any JVM build tool.
The Ratpack project provides specific support for [Gradle](http://www.gradle.org) through plugins, but any could be used.

Ratpack is published as a set of library JARs.
The `ratpack-core` library is the only strictly required library.
Others such as `ratpack-groovy`, `ratpack-guice`, `ratpack-jackson`, `ratpack-test` etc. are optional.

## Goals

Ratpack's goals are:
  
1. To be fast, scalable, and efficient
2. To allow applications to evolve in complexity without compromise
3. To leverage the benefits of non-blocking programming and reduce the costs
4. To be flexible and unopinionated when it comes to integrating other tools and libraries
5. To allow applications to be easily and thoroughly tested
 
Ratpacks's goals are **not**:

1. To be a fully integrated, “full stack” solution
2. Provide every feature you might need in a neat box
3. To provide an architecture or framework for “business logic”

## About this documentation

The documentation for Ratpack is spread over this manual and the [Javadoc API reference](api/).
The manual introduces topics and concepts at a high level and links through to the Javadoc for detailed API information.
The majority of the information is contained within the Javadoc.
It is expected that once you have an understanding of the core Ratpack concepts, the manual becomes less useful and the Javadoc more useful.

### Code samples

All of the code samples in the documentation are tested, and most are complete programs that you can copy/paste and run yourself (given the right classpath etc.).

Most of the samples are given as tiny embedded Ratpack applications, under test.
The following is the “Hello World” of Ratpack code samples.

```language-java
import ratpack.test.embed.EmbeddedApp;
import static org.junit.Assert.assertEquals;
 
public class Example {
  public static void main(String... args) throws Exception {
    EmbeddedApp.fromHandler(ctx -> 
      ctx.render("Hello World!")
    ).test(httpClient -> 
      assertEquals("Hello World!", httpClient.getText())
    );
  }
}
```

The `import` statements are collapsed by default for clarity.
Click them to show/hide them.

This example is a complete Ratpack application.
However, the [`EmbeddedApp`](api/ratpack/test/embed/EmbeddedApp.html) is not the entry point that is typically used for proper applications (see the [Launching chapter](launching.html) for details on the typical entry point).
`EmbeddedApp` is testing oriented.
It makes it easy to start/stop very small (or fully fledged) apps during a larger application, and provides a convenient way to make HTTP requests against the app.
It is used in the examples to keep the amount of bootstrapping to a minimum in order to focus on the API being demonstrated.

In this example we are starting a Ratpack server on an ephemeral port with default configuration that responds to all HTTP requests with the plain text string “Hello World”.
The [`test()`](api/ratpack/test/CloseableApplicationUnderTest.html#test-ratpack.func.Action-) method being used here provides a [`TestHttpClient`](api/ratpack/test/http/TestHttpClient.html) to the given function, that is configured to make requests of the server under test.
This example and all others like it are making HTTP requests to a Ratpack server.
[`EmbeddedApp`](api/ratpack/test/embed/EmbeddedApp.html) and [`TestHttpClient`](api/ratpack/test/http/TestHttpClient.html) are provided as part of Ratpack's [testing support](testing.html).

Another key testing utility that is used in many examples is [`ExecHarness`](api/ratpack/test/exec/ExecHarness.html).

```language-java
import com.google.common.io.Files;
import ratpack.test.exec.ExecHarness;
import ratpack.exec.Blocking;

import java.io.File;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class Example {
  public static void main(String... args) throws Exception {
    File tmpFile = File.createTempFile("ratpack", "test");
    Files.write("Hello World!", tmpFile, StandardCharsets.UTF_8);
    tmpFile.deleteOnExit();

    String content = ExecHarness.yieldSingle(e ->
        Blocking.get(() -> Files.toString(tmpFile, StandardCharsets.UTF_8))
    ).getValueOrThrow();

    assertEquals("Hello World!", content);
  }
}
```

Where `EmbeddedApp` supports creating an entire Ratpack application, `ExecHarness` provides just the infrastructure for Ratpack's execution model.
It is typically used to unit test asynchronous code that uses Ratpack constructs like [`Promise`](api/ratpack/exec/Promise.html) (see the [“Asynchronous & Non Blocking”](async.html) chapter for more info on the execution model).
[`ExecHarness`](api/ratpack/test/exec/ExecHarness.html) is also provided as part of Ratpack's [testing support](testing.html).

#### Java 8 style

Ratpack is built on, and requires, Java 8. The code samples extensively use Java 8 constructs such as lambda expressions and method references.
If you are experienced with Java but not the new constructs in Java 8, you may find the examples “exotic”.
