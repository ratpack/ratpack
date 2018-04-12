# Hystrix

If your application is part of a distributed architecture like microservices, has dependencies on other distributed services or a client library that can potentially result in network requests, then it is essential that you defend your application 
with [Hystrix](https://github.com/Netflix/Hystrix/wiki). Part of the [Netflix OSS Platform](https://github.com/Netflix), Hystrix is a library that provides fault tolerance and greater control in the face of failure resulting in reduced latency, 
increased responsiveness and resilience for your application.  See the [Hystrix Wiki](https://github.com/Netflix/Hystrix/wiki/How-To-Use#Common-Patterns) for common usage patterns.
  
Hystrix can also help you reduce the number of external network calls your application makes by de-duping (Request Caching) and batching (Request Collapsing) your requests within a request context.  The `ratpack-hystrix` JAR provides and registers
a [Hystrix Concurrency Strategy](http://netflix.github.io/Hystrix/javadoc/index.html?com/netflix/hystrix/strategy/concurrent/HystrixConcurrencyStrategy.html) with Hystrix that allows the [Ratpack Registry](api/ratpack/registry/Registry.html) 
to be used for managing the [Hystrix Request Context](http://netflix.github.io/Hystrix/javadoc/index.html?com/netflix/hystrix/strategy/concurrent/HystrixRequestContext.html).  What all that means is that you **do not** need to initialise a Hystrix Request Context
(`HystrixRequestContext context = HystrixRequestContext.initializeContext();`) before a request begins as detailed in the [Hystrix wiki](https://github.com/Netflix/Hystrix/wiki/How-To-Use#RequestContextSetup).

One of the best features of Hystrix is the metrics that are captured and the [dashboard](https://github.com/Netflix-Skunkworks/hystrix-dashboard/wiki) provided for viewing them real-time.  This is achieved by streaming metrics out of your application over SSE and 
the `ratpack-hystrix` JAR provides a [handler](api/ratpack/hystrix/HystrixMetricsEventStreamHandler.html) that will do this for you.
    
The `ratpack-hystrix` module as of @ratpack-version@ is built against (and depends on) RxJava @versions-hystrix@.

## Initialization

If you do not need to use any Hystrix request scoped features (request caching, request collapsing, request log) or the Ratpack handler for streaming metrics then you can just include Hystrix as a dependency and there is no initialization required.  If you do
want to use these features however, then your application should depend on the `ratpack-hystrix` module and simply register the Guice module, [HystrixModule](api/ratpack/hystrix/HystrixModule.html).
 
## Which Command execution to use

Hystrix provides three types of Command execution, [synchronous](https://github.com/Netflix/Hystrix/wiki/How-To-Use#Synchronous-Execution), [asynchronous](https://github.com/Netflix/Hystrix/wiki/How-To-Use#asynchronous-execution) and [reactive](https://github.com/Netflix/Hystrix/wiki/How-To-Use#reactive-execution).
Out of the three only reactive is actually non-blocking.  Synchronous and asynchronous command execution will work with Ratpack, as is demonstrated in the [integration tests](https://github.com/ratpack/ratpack/blob/master/ratpack-hystrix/src/test/groovy/ratpack/hystrix/HystrixRequestCachingSpec.groovy#L128), 
but for maximum performance only reactive execution is recommended.  If you do not wish to use Observables however, then you can convert them to Ratpack Promises instead using [RxRatpack#promise](api/ratpack/rx/RxRatpack.html#promise-rx.Observable-)
or [RxRatpack#promiseSingle](api/ratpack/rx/RxRatpack.html#promiseSingle-rx.Observable-).

```language-java
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;
import ratpack.exec.Promise;
import ratpack.rx.RxRatpack;
import rx.Observable;
public class CommandFactory {

  public static Promise<String> fooCommand() {
    Observable<String> command = new HystrixObservableCommand<String>(HystrixCommandGroupKey.Factory.asKey("foo-command")) {
      @Override
      protected Observable<String> construct() {
        return Observable.just("foo");
      }
    }.toObservable();

    return RxRatpack.promiseSingle(command);
  }
}
```

There is a fourth command execution under construction, [HystrixAsyncCommand](https://github.com/Netflix/Hystrix/issues/321) that will also be non-blocking and hopefully available in 1.4.0 Final.

## Executing blocking code in a command

When you have a command that needs to execute blocking code, e.g. for an external resource that you do not have a non blocking client for, then inject `ExecControl` into your command and observe
`ExecControl#blocking`.  You can see an example of how to do this in [Example-Books](https://github.com/ratpack/example-books/blob/master/src/main/groovy/ratpack/example/books/BookDbCommands.groovy#L37).

## Streaming metrics

TODO

### Turbine

TODO
