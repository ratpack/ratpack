# Logging

Ratpack uses SLF4J for logging, which allows you to easily bind your favorite logging library at compile time. 

Library options include:

* No-Op - discards all logs (default)
* Log4J
* Logback - native SLF4J implementation with "zero memory and computational overhead"
* Java Util Logging
* Simple - logs messages at INFO level and higher to System.err
* Jakarta Commons Logging

Simply add <em>one</em> logging library as a dependency and use SLF4J syntax to log.
If you are currently using another logging library, SLF4J provides a [migration tool](http://www.slf4j.org/migrator.html) to automate the transition.  
Examples for Java and Groovy are below and more details can be found in the [SLF4J manual](http://www.slf4j.org/manual.html).

## Java

```language-groovy tested
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogExample {
  private final static Logger LOGGER = LoggerFactory.getLogger(LogExample.class);
    
  public void log() {
    LOGGER.info("Start logging");
    LOGGER.warn("Logging with a {} or {}", "parameter", "two");
    LOGGER.error("Log an exception", new Exception("Example exception"));
    LOGGER.info("Stop logging");
  }
}
```

## Groovy

```language-groovy tested
import groovy.util.logging.Slf4j

@Slf4j
class LogExample {
  void log() {
    log.info "Start logging"
    log.warn "Logging with a {} or {}", "parameter", "two"
    log.debug "Detailed information"
    log.info "Stop logging"
  }
}
```

## Request Logging

Ratpack provides a mechanism for logging information about each request, [`RequestLogger`](api/ratpack/handling/RequestLogger.html).
The request logger is a handler.
Each request that flows through it will be logged, when the request completes.
Typically, it is placed early in the handler chain and added with the `Chain.all(Handler)` method so that all requests are logged.
 
Ratpack provides the [`RequestLogger.ncsa()`](api/ratpack/handling/RequestLogger.html#ncsa--) method, that logs in the [NCSA Common Log Format](https://en.wikipedia.org/wiki/Common_Log_Format).
This implementation logs to an slf4j logger named `ratpack.requests` 
(the [`RequestLogger.ncsa(Logger)`](api/ratpack/handling/RequestLogger.html#ncsa-org.slf4j.Logger-) method allows an alternative logger to be specified).  

```language-java
import ratpack.handling.RequestLogger;
import ratpack.http.client.ReceivedResponse;
import ratpack.test.embed.EmbeddedApp;
import static org.junit.Assert.*;

public class Example {
  public static void main(String... args) throws Exception {
    EmbeddedApp.fromHandlers(c -> c
      .all(RequestLogger.ncsa())
      .all(ctx -> ctx.render("ok"))
    ).test(httpClient -> {
      ReceivedResponse response = httpClient.get();
      assertEquals("ok", response.getBody().getText());

      // Check log output: [ratpack-compute-1-1] INFO ratpack.requests - 127.0.0.1 - - [30/Jun/2015:11:01:18 -0500] "GET / HTTP/1.1" 200 2
    });
  }
}
```

See the documentation of [`RequestLogger`](api/ratpack/handling/RequestLogger.html) for information on creating a logger with an alternative format.
