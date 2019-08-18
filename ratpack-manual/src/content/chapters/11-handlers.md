# Handlers

This chapter introduces handlers, which are the fundamental components of a Ratpack application.

## What is a handler?

Conceptually, a handler ([`Handler`](api/ratpack/handling/Handler.html)) is just a function that acts on a handling context ([`Context`](api/ratpack/handling/Context.html)).

The “hello world” handler looks like this…

```language-java
import ratpack.handling.Handler;
import ratpack.handling.Context;

public class Example implements Handler {
  public void handle(Context context) {
      context.getResponse().send("Hello world!");
  }
}
```

As we saw in the [previous chapter](launching.html), one of the mandatory launch config properties is the HandlerFactory implementation
that provides the primary handler.
The handler that this factory creates is effectively the application.

This may seem limiting, until we recognise that a handler does not have to be an _endpoint_ (i.e. it can do other things than generate a HTTP response).
Handlers can also delegate to other handlers in a number of ways, serving more of a _routing_ function.
The fact that there is no framework level (i.e. type) distinction between a routing step and an endpoint offers much flexibility.
The implication is that any kind of custom request processing _pipeline_ can be built by _composing_ handlers.
This compositional approach is the canonical example of Ratpack's philosophy of being a toolkit instead of a magical framework.

The rest of this chapter discusses aspects of handlers that are beyond HTTP level concerns (e.g. reading headers, sending responses etc.), which is addressed in the [HTTP chapter](http.html).

## Handler delegation

If a handler is not going to generate a response, it must delegate to another handler.
It can either _insert_ one or more handlers, or simply defer to the _next_ handler.

Consider a handler that routes to one of two different handlers based on the request path.
This can be implemented as…

```language-groovy tested
import ratpack.handling.Handler;
import ratpack.handling.Context;

public class FooHandler implements Handler {
  public void handle(Context context) {
    context.getResponse().send("foo");
  }
}

public class BarHandler implements Handler {
  public void handle(Context context) {
    context.getResponse().send("bar");
  }
}

public class Router implements Handler {
  private final Handler fooHandler = new FooHandler();
  private final Handler barHandler = new BarHandler();

  public void handle(Context context) {
    String path = context.getRequest().getPath();
    if (path.equals("foo")) {
      context.insert(fooHandler);
    } else if (path.equals("bar")) {
      context.insert(barHandler);
    } else {
      context.next();
    }
  }
}
```

The key to delegation is the [`context.insert()`](api/ratpack/handling/Context.html#insert-ratpack.handling.Handler...-) method that passes control to one or more linked handlers.
The [`context.next()`](api/ratpack/handling/Context.html#next--) method passes control to the next linked handler.

Consider the following…

```language-groovy tested
import ratpack.handling.Handler;
import ratpack.handling.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrintThenNextHandler implements Handler {
  private final String message;
  private final static Logger LOGGER = LoggerFactory.getLogger(PrintThenNextHandler.class);


  public PrintThenNextHandler(String message) {
    this.message = message;
  }

  public void handle(Context context) {
    LOGGER.info(message);
    context.next();
  }
}

public class Application implements Handler {
  public void handle(Context context) {
    context.insert(
      new PrintThenNextHandler("a"),
      new PrintThenNextHandler("b"),
      new PrintThenNextHandler("c")
    );
  }
}
```

Given that `Application` is the primary handler (i.e. the one returned by the launch config's `HandlerFactory`),
when this application receives a request the following will be written to `System.out`…

```
a
b
c
```

And then what?
What happens when the “c” handler delegates to its next?
The last handler is _always_ an internal handler that issues a HTTP 404 client error (via `context.clientError(404)` which is discussed later).

Consider that inserted handlers can themselves insert more handlers…

```language-groovy tested
import ratpack.handling.Handler;
import ratpack.handling.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrintThenInsertOrNextHandler implements Handler {
  private final String message;
  private final Handler[] handlers;
  private final static Logger LOGGER = LoggerFactory.getLogger(PrintThenInsertOrNextHandler.class);

  public PrintThenInsertOrNextHandler(String message, Handler... handlers) {
    this.message = message;
    this.handlers = handlers;
  }

  public void handle(Context context) {
    LOGGER.info(message);
    if (handlers.length == 0) {
      context.next();
    } else {
      context.insert(handlers);
    }
  }
}

public class Application implements Handler {
  public void handle(Context context) {
    context.insert(
      new PrintThenInsertOrNextHandler("a",
        new PrintThenInsertOrNextHandler("a.1"),
        new PrintThenInsertOrNextHandler("a.2"),
      ),
      new PrintThenInsertOrNextHandler("b",
        new PrintThenInsertOrNextHandler("b.1",
          new PrintThenInsertOrNextHandler("b.1.1")
        ),
      ),
      new PrintThenInsertOrNextHandler("c")
    );
  }
}
```

This would write the following to `System.out`…

```
a
a.1
a.2
b
b.1
b.1.1
c
```

This demonstrates how the _next_ handler of the handler that inserts the handlers becomes the _next_ handler of the last of the inserted handlers.
You might need to read that sentence more than once.

You should be able to see a certain nesting capability emerge.
This is important for composibility, and also for scoping which will be important when considering the registry context later in the chapter.

It would be natural at this point to think that it looks like a lot of work to build a handler structure for a typical web application
(i.e. one that dispatches requests matching certain request paths to endpoints).
Read on.

## Building handler chains

A chain ([`Chain`](api/ratpack/handling/Chain.html)) is a builder for composing (or _chaining_) handlers.
The chain itself doesn't respond to a request, but instead passes a request around to it's attached handlers.

Consider again the Foo-Bar router example…

```language-groovy tested
import ratpack.handling.Chain
import ratpack.handling.Handler;
import ratpack.handling.Context;
import ratpack.func.Action;

public class FooHandler implements Handler {
    public void handle(Context context) {
        context.getResponse().send("foo");
    }
}

public class BarHandler implements Handler {
    public void handle(Context context) {
        context.getResponse().send("bar");
    }
}

public class RouterChain implements Action<Chain> {
    private final Handler fooHandler = new FooHandler();
    private final Handler barHandler = new BarHandler();

    @Override
    void execute(Chain chain) throws Exception {
        chain.path("foo", fooHandler)
        chain.path("bar", barHandler)
    }
}
```

This time, we didn't have to manually check the path and handle each code branch.
The result, however, is the same.
This chain will eventually be treated as handler.
This handler will be setup to read the path from a request and first compare it with "foo", then "bar".
If either of the matches, it will `context.insert()` the given handler.
Otherwise, it will call `context.next()`.

Like the handler, the context aims not to be a piece of magic.
Instead it is a powerful tool built from the more flexible tool, the handler.

### Adding Handlers and Chains

So the chain can most simply be thought of as a list of handlers.
The most basic way to add a handler to the chain's list is the [`all(Handler)`](api/ratpack/handling/Chain.html#all-ratpack.handling.Handler-) method.
The word "all" represents that all requests reaching this point in the chain will flow through the given handler.

If we stretch our minds a little and think of the chain as a handler (one that is just specialized in inserting handlers), then it also stands to reason that we can add additional chains to a chain.
In fact, we can, and to match the `all(Handler)` method, you may use the [`insert(Action<Chain>)`](api/ratpack/handling/Chain.html#insert-ratpack.func.Action-) method.
Likewise, this inserts a chain through which all requests are routed.

Now, the chain wouldn't be very useful if it just handled a list of handlers, calling each in a row, so there are also several methods than can perform conditional inserts of handlers and chains:

* [`path(String,Handler)`](api/ratpack/handling/Chain.html#path-java.lang.String-ratpack.handling.Handler-), used in the previous example, is particularly useful for routing to different handlers based upon the request path.
  It also comes in a [`path(Handler)`](api/ratpack/handling/Chain.html#path-ratpack.handling.Handler-) flavor to easily match the empty "" path.
* [`onlyIf(Predicate<Context>, Handler)`](api/ratpack/handling/Chain.html#onlyIf-ratpack.func.Predicate-ratpack.handling.Handler-) can be used to route based upon a programmatic behavior.
* [`host(String, Action<Chain>)`](api/ratpack/handling/Chain.html#host-java.lang.String-ratpack.func.Action-) inserts another chain when a request has a specific Host header value.
* [`when(Predicate<Context>, Action<Chain>)`](api/ratpack/handling/Chain.html#when-ratpack.func.Predicate-ratpack.func.Action-) will insert a chain when a programmatic behavior is met.

### Registry

TODO (A technical definition can be found on the [`Chain`](api/ratpack/handling/Chain.html) javadocs)

### Path Bindings

(i.e. /player/:id )

TODO (A technical definition can be found on the [`Chain`](api/ratpack/handling/Chain.html) javadocs)

### Path and Method Bindings

TODO (A technical definition can be found on the [`Chain`](api/ratpack/handling/Chain.html) javadocs)
