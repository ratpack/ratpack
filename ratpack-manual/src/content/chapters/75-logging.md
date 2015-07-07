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

Ratpack ships with an implementation of the [NCSA Common Log Format](http://publib.boulder.ibm.com/tividd/td/ITWSA/ITWSA_info45/en_US/HTML/guide/c-logs.html#common) for logging requests.
The request logs are output using the same SLF4J API and logging library as described above.
Specifically, the request log is always written to the [`RequestLog`](api/ratpack/handling/RequestLog.html) logger.

Request logging is enabled by adding the `RequestLog` handler to the application.


```language-java
import ratpack.handling.RequestLog;
import ratpack.http.client.ReceivedResponse;
import ratpack.test.embed.EmbeddedApp;
import static org.junit.Assert.*;

public class Example {
  public static void main(String... args) throws Exception {
    EmbeddedApp.fromHandlers(chain -> chain
      .all(RequestLog.log())
      .all(ctx -> {
        ctx.render("ok");
      })
    ).test(httpClient -> {
      ReceivedResponse response = httpClient.get();
      assertEquals("ok", response.getBody().getText());

      // Check log output: [ratpack-compute-213-4] INFO ratpack.handling.RequestLog - 127.0.0.1 - - [30/Jun/2015:11:01:18 -0500] "GET / HTTP/1.1" 200 2
    });
  }
}
```

The request log format follows the NCSA Common Log Format.
A custom request log format can be supplied by adding an implementation of the [`RequestLog`](api/ratpack/handling/RequestLog.html) interface to the application's registry.

