# RxJava

The excellent [RxJava](https://github.com/Netflix/RxJava) can be used in Ratpack applications to elegantly compose asynchronous operations.

The `ratpack-rx` JAR provides with [`RxRatpack`](api/ratpack/rx/RxRatpack.html) class that provides static methods for adapting Ratpack promises to [RxJava's Observable](https://github.com/Netflix/RxJava/wiki/Observable).

The `ratpack-rx` module as of @ratpack-version@ is built against (and depends on) RxJava @versions-rxjava@.

## Initialization

The [`RxRatpack.initialize()`](api/ratpack/rx/RxRatpack.html#initialize\(\)) must be called to fully enable the integration.
This method only needs to be called once for the JVM's lifetime.

## Observing Ratpack

The integration is based on the [`RxRatpack.observe()`](api/ratpack/rx/RxRatpack.html#observe\(ratpack.exec.Promise\)) and [`RxRatpack.observeEach()`](api/ratpack/rx/RxRatpack.html#observeEach\(ratpack.exec.Promise\)) static methods.
These methods adapt Ratpack's promise type into an observable, which can then be used with all of the observable operators that RxJava offers.

For example, blocking operations can be easily observed.

```language-groovy tested
import ratpack.handling.Handler;
import ratpack.handling.Context;
import ratpack.exec.Promise;
import java.util.concurrent.Callable;
import rx.util.functions.Func1;
import rx.util.functions.Action1;

import static ratpack.rx.RxRatpack.observe;

public class ReactiveHandler implements Handler {
  public void handle(Context context) {
    Promise<String> promise = context.blocking(new Callable<String>() {
      public String call() {
        // do some blocking IO here
        return "hello world";
      }
    });

    observe(promise).map(new Func1<String, String>() {
      public String call(String input) {
        return input.toUpperCase();
      }
    }).subscribe(new Action1<String>() {
      public void call(String str) {
        context.render(str); // renders: HELLO WORLD
      }
    });
  }
}
```

A similar example in the Groovy DSL would look like:

```language-groovy groovy-handlers
import static ratpack.rx.RxRatpack.observe

handlers {
  handler {
    observe(blocking {
      // do some blocking IO
      "hello world"
    }) map { String input ->
      input.toUpperCase()
    } subscribe {
      render it // renders: HELLO WORLD
    }
  }
}
```

## Implicit error handling

A key feature of the RxJava integration is the implicit error handling.
All observable sequences have an implicit default error handling strategy of forwarding the exception to the execution context error handler.
In practice, this means that error handlers rarely need to be defined for observable sequences.

```language-groovy tested
import ratpack.handling.Handler;
import ratpack.handling.Context;
import ratpack.exec.Promise;
import rx.functions.Action1;
import java.util.concurrent.Callable;

import static ratpack.rx.RxRatpack.observe;

class ReactiveHandler implements Handler {
  public void handle(Context context) {
    Promise<String> promise = context.blocking(new Callable<String>() {
      public String call() throws Exception {
        throw new IOException("error!");
      }
    });

    observe(promise).subscribe(new Action1<String>() {
      public void call(String str) {
        // will never be called because of error
      }
    });
  }
}
```

In this case, the exception thrown during the blocking operation will be forwarded to the current [`ServerErrorHandler`](api/ratpack/error/ServerErrorHandler.html), which will probably render an error page to the response.
If the subscriber _does_ implement an error handling strategy, it will be used instead of the implicit error handler.

The implicit error handling applies to _all_ observables that are created on Ratpack managed threads.
It is _not_ restricted to observables that are backed by Ratpack promises.