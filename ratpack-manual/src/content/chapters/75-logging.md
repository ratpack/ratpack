# Logging

Ratpack uses SLF4J for logging, which allows you to easily bind your favorite logging library at compile time. 

Library options include:

* No-Op - discards all logs (default)
* Log4J
* Logback - native SLF4J implementation with "zero memory and computational overhead"
* Java Util Logging
* Simple - logs messages at INFO level and higher to System.err
* Jakarta Commons Logging

Simply add <em>one</em> logging library as a dependency and use slf4j syntax to log.
If you are using another logging library, SLF4J provides a [migration tool](http://www.slf4j.org/migrator.html).  
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
