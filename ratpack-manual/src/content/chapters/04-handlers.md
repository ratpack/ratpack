# Handlers

This chapter introduces handlers, which are the fundamental components of a Ratpack application.

## What is a handler?

Conceptually, a handler ([`Handler`](api/ratpack/handling/Handler.html)) is just a function that acts on a handling context ([`Context`](api/ratpack/handling/Context.html)).

The “hello world” handler looks like this…

```language-groovy tested
import ratpack.handling.Handler;
import ratpack.handling.Context;

public class HelloWorld implements Handler {
  public void handle(Context context) {
      context.getResponse().send("Hello world!");
  }
}
```

As we saw in the [previous chapter](launching.html), one of the mandatory launch config properties is the [`HandlerFactory`](api/ratpack/launch/HandlerFactory.html) implementation
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

import static java.util.Collections.singletonList;

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
  private final List<? extends Handler> fooHandler = singletonList(new FooHandler());
  private final List<? extends Handler> barHandler = singletonList(new BarHandler());
      
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

The key to delegation is the [`context.insert()`](api/ratpack/handling/Context.html#insert\(java.util.List\)) method that passes control to one or more linked handlers.
The [`context.next()`](api/ratpack/handling/Context.html#next\(\)) method passes control to the next linked handler.

Consider the following…

```language-groovy tested
import ratpack.handling.Handler;
import ratpack.handling.Context;

import static java.util.Arrays.asList;

public class PrintThenNextHandler implements Handler {
  private final String message;
  
  public PrintThenNextHandler(String message) {
    this.message = message;
  } 
  
  public void handle(Context context) {
    System.out.println(message);
    context.next();
  }
}

public class Application implements Handler {    
  public void handle(Context context) {
    context.insert(asList(
      new PrintThenNextHandler("a"),
      new PrintThenNextHandler("b"),
      new PrintThenNextHandler("c")
    ));
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

import static java.util.Arrays.asList;

public class PrintThenInsertOrNextHandler implements Handler {
  private final String message;
  private final List<Handler> handlers;

  public PrintThenInsertOrNextHandler(String message, Handler... handlers) {
    this.message = message;
    this.handlers = asList(handlers);
  }

  public void handle(Context context) {
    System.out.println(message);
    if (handlers.isEmpty()) {
      context.next();
    } else {
      context.insert(handlers);
    }
  }
}

public class Application implements Handler {
  public void handle(Context context) {
    context.insert(asList(
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
    ));
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

Ratpack provides a suite of routing type handlers out of the box that make it easy to compose dispatch logic.
These are available via the static methods of the [`Handlers`](api/ratpack/handling/Handlers.html) class.

For example, the [`path(String, List<Handler>)`](api/ratpack/handling/Handlers.html#path\(java.lang.String, java.util.List\)) method can be used for path based routing.

```language-groovy tested
import ratpack.handling.Handler;
import ratpack.handling.Context;
import ratpack.launch.LaunchConfig;
import ratpack.launch.HandlerFactory;

import static ratpack.handling.Handlers.path;
import static ratpack.handling.Handlers.get;
import static java.util.Arrays.asList;

public class SomeHandler implements Handler {
  public void handle(Context context) {
      // do some application work
  }
}

public class Application implements HandlerFactory {
  public Handler create(LaunchConfig launchConfig) {
    return path("foo/bar", asList(get(), new SomeHandler()));
  }
}
```

Here we have a [`HandlerFactory`](api/ratpack/launch/HandlerFactory.html) that can be used when launching an app (see previous chapter).
For this “application”:

1. a GET request to `/foo/bar` would be routed to the `SomeHandler`
2. a non-GET request to `/foo/bar` would produce a HTTP 405 (method not allowed)
3. anything else would produce a HTTP 404

This is easier than doing it all yourself, but we can do better.
We can use the [`chain()`](api/ratpack/handling/Handlers.html#chain\(ratpack.launch.LaunchConfig,%20ratpack.util.Action\)) method and the [`Chain`](api/ratpack/handling/Chain.html) DSL.

```language-groovy tested
import ratpack.handling.Handler;
import ratpack.handling.Context;
import ratpack.handling.Chain;
import ratpack.util.Action;
import ratpack.launch.LaunchConfig;
import ratpack.launch.HandlerFactory;

import static ratpack.handling.Handlers.chain;

public class Application implements HandlerFactory {
  public Handler create(LaunchConfig launchConfig) {
    return chain(launchConfig, new Action<Chain>() {
      void execute(Chain chain) {
        chain.
          prefix("api", new Action<Chain>() {
            void execute(Chain apiChain) {
              apiChain.
                delete("someResource", new Handler() {
                  public void handle(Context context) {
                    // delete the resource
                  }
                });
            }
          }).
          assets("public").
          get("foo/bar", new Handler() {
            public void handle(Context context) {
              // do stuff
            }
          });
      }
    });
  }
}
```

(note: the use of inner classes adds a lot of syntactic bloat here, things are more concise with Java 8 lambdas)

The chain DSL is built on the existing delegation methods that have been presented so far.
It is merely syntactic sugar.
The Groovy version of this DSL is extremely sweet…

```language-groovy tested
import ratpack.handling.Handler
import ratpack.launch.LaunchConfig
import ratpack.launch.HandlerFactory

import static ratpack.groovy.Groovy.chain

class Application implements HandlerFactory {
  Handler create(LaunchConfig launchConfig) {
    chain(launchConfig) {
      prefix("api") {
        delete("someResource") {
          // delete the resource
        }
      }
      assets("public")
      get("foo/bar") {
        // do stuff
      }
    }
  }
}
```

See the [chapter on Groovy](groovy.html) for more information on using Groovy with Ratpack.
