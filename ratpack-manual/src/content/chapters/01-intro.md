# Introduction

Ratpack is a set of Java libraries that facilitate fast, efficient, evolvable and well tested HTTP applications.

It is built on the highly performant and efficient Netty event-driven networking engine.

Ratpack focuses on allowing HTTP applications to be efficient, modular, adaptive to new requirements and technologies, and well-tested over time.

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
However, the [`EmbeddedApp`](api/ratpack/test/embed/EmbeddedApp.html) is not the entry point that is typically used for applications proper (see the next chapter for details on that).
`EmbeddedApp` is testing oriented.
It makes it easy to start/stop very small (or fully fledged) apps during a larger application, and provides a convenient way to make HTTP requests against the app.
It is used in the examples to keep the amount of bootstrapping to a minimum in order to focus on the API being demonstrated.

Another key testing utility that is used in many examples is [`ExecHarness`](api/ratpack/test/exec/ExecHarness.html).

```language-java
import com.google.common.io.Files;
import ratpack.test.exec.ExecHarness;

import java.io.File;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class Example {
  public static void main(String... args) throws Exception {
    File tmpFile = File.createTempFile("ratpack", "test");
    Files.write("Hello World!", tmpFile, StandardCharsets.UTF_8);
    tmpFile.deleteOnExit();

    String content = ExecHarness.yieldSingle(e ->
        e.blocking(() -> Files.toString(tmpFile, StandardCharsets.UTF_8))
    ).getValueOrThrow();

    assertEquals("Hello World!", content);
  }
}
```

Where `EmbeddedApp` supports creating an entire Ratpack application, `ExecHarness` provides just the infrastructure for Ratpack's execution model.
It is typically used to unit test asynchronous code that uses Ratpack constructs like [`Promise`](api/ratpack/exec/Promise.html) (see the [“Asynchronous & Non Blocking”](async.html) chapter for more info on the execution model).

Being able to understand how to read the examples littered throughout the documentation is very important.
 
