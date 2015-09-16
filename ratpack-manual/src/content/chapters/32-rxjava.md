# RxJava

The excellent [RxJava](https://github.com/Netflix/RxJava) can be used in Ratpack applications to elegantly compose asynchronous operations.

The `ratpack-rx` JAR provides with [`RxRatpack`](api/ratpack/rx/RxRatpack.html) class that provides static methods for adapting Ratpack promises to [RxJava's Observable](https://github.com/Netflix/RxJava/wiki/Observable).

The `ratpack-rx` module as of @ratpack-version@ is built against (and depends on) RxJava @versions-rxjava@.

## Initialization

The [`RxRatpack.initialize()`](api/ratpack/rx/RxRatpack.html#initialize--) must be called to fully enable the integration.
This method only needs to be called once for the JVM's lifetime.

## Observing Ratpack

The integration is based on the [`RxRatpack.observe()`](api/ratpack/rx/RxRatpack.html#observe-ratpack.exec.Promise-) and [`RxRatpack.observeEach()`](api/ratpack/rx/RxRatpack.html#observeEach-ratpack.exec.Promise-) static methods.
These methods adapt Ratpack's promise type into an observable, which can then be used with all of the observable operators that RxJava offers.

For example, blocking operations can be easily observed.

```language-java
import ratpack.exec.Promise;
import ratpack.exec.Blocking;
import ratpack.test.handling.HandlingResult;

import static org.junit.Assert.assertEquals;
import static ratpack.rx.RxRatpack.observe;
import static ratpack.test.handling.RequestFixture.requestFixture;

public class Example {
  public static void main(String... args) throws Exception {
    HandlingResult result = requestFixture().handle(context -> {
      Promise<String> promise = Blocking.get(() -> "hello world");
      observe(promise).map(String::toUpperCase).subscribe(context::render);
    });

    assertEquals("HELLO WORLD", result.rendered(String.class));
  }
}
```

## Implicit error handling

A key feature of the RxJava integration is the implicit error handling.
All observable sequences have an implicit default error handling strategy of forwarding the exception to the execution context error handler.
In practice, this means that error handlers rarely need to be defined for observable sequences.

```language-java
import ratpack.error.ServerErrorHandler;
import ratpack.rx.RxRatpack;
import ratpack.test.handling.RequestFixture;
import ratpack.test.handling.HandlingResult;
import rx.Observable;

import static org.junit.Assert.assertEquals;

public class Example {
  public static void main(String... args) throws Exception {
    RxRatpack.initialize(); // must be called once per JVM

    HandlingResult result = RequestFixture.requestFixture().handleChain(chain -> {
      chain.register(registry ->
          registry.add(ServerErrorHandler.class, (context, throwable) ->
              context.render("caught by error handler: " + throwable.getMessage())
          )
      );

      chain.get(ctx -> Observable.<String>error(new Exception("!")).subscribe((s) -> {}));
    });

    assertEquals("caught by error handler: !", result.rendered(String.class));
  }
}
```

In this case, the throwable thrown during the blocking operation will be forwarded to the current [`ServerErrorHandler`](api/ratpack/error/ServerErrorHandler.html), which will probably render an error page to the response.
If the subscriber _does_ implement an error handling strategy, it will be used instead of the implicit error handler.

The implicit error handling applies to _all_ observables that are created on Ratpack managed threads.
It is _not_ restricted to observables that are backed by Ratpack promises.
