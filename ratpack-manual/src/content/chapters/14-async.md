# Asynchronous & Non Blocking

Ratpack is designed for “asynchronous” & “non blocking” request processing.
Its internal IO (e.g. HTTP request and response transfer) is all performed in a non blocking fashion (thanks to [Netty](http://netty.io/)).
This approach yields higher throughput, lower resource usage, and importantly, more predictable behaviour under load.
This programming model has become increasingly popular of late due to the [Node.js](http://nodejs.org) platform.
Ratpack is built on the same non blocking, event driven, model as Node.js.

Asynchronous programming is notoriously tricky.
One of Ratpack's key value propositions is that it provides constructs and abstractions to tame the asynchronous beast, yielding better performance while keeping implementation simple.

## Comparison to blocking frameworks & containers

The Java Servlet API, that underpins most JVM web frameworks and containers, along with the majority of the JDK is fundamentally based on a synchronous programming model.
Most JVM programmers are very familiar and comfortable with this programming model.
In this model, when IO needs to be performed the calling thread will simply _sleep_ until the operation is complete and the result is available.
This model requires a reasonably large pool of threads.
In a web application context, this usually means that each request is bound to a thread from the large pool and that the application can process «X» number of parallel requests, where «X» is the size of the thread pool.

> Version 3.0 of the Servlet API does facilitate asynchronous request processing.
> However, retrofitting asynchronous support as an opt-in option is a different proposition to a completely asynchronous approach.
> Ratpack is asynchronous from the ground up.

The benefit of this model is that synchronous programming is unarguably “simpler”.
The drawback of this model, opposed to a non blocking model, is that it demands greater resource usage and yields lower throughput.
In order to server more requests in parallel, the size of the thread pool has to be increased.
This creates more contention for compute resources and more cycles are lost to managing the scheduling of these threads, not to mention the increased memory consumption.
Modern operating systems, and the JVM, are very good at managing this contention; however, it is still a scaling bottleneck.
Moreover, it demands greater resource allocation, which is a serious consideration for modern pay-for-what-you-use deployment environments.

The asynchronous, non blocking, model does not require a large thread pool.
This is possible because threads are never blocked waiting for IO.
If IO needs to be performed, the calling thread registers a _callback_ of some sort that will be invoked when the IO is done.
This allows the thread to be used for other processing while the IO is occurring.
Under this model, the thread pool is sized according to the number of processing cores available.
Since the threads are always busy with computation, there is no point in having more threads.

> Many Java APIs (`InputStream`, `JDBC`, etc.) are predicated on a blocking IO model.
> Ratpack provides a mechanism for using such API while minimizing the blocking cost (discussed below).

Ratpack is fundamentally asynchronous in two key ways…

1. HTTP IO is event driven / non blocking (thanks to [Netty](http://netty.io/))
2. Request handling is organised as a pipeline of asynchronous functions

The HTTP IO being event driver is largely transparent when using Ratpack.
Netty just does its thing.

The second point is _the_ key characteristic of Ratpack.
It does not expect your code to be synchronous.
Many web frameworks that have opt in asynchronous support have serious constraints and gotchas that become apparent when trying to perform complex (i.e. real world) async operations.
Ratpack is asynchronous from the ground up.
Moreover, it provides constructs and abstractions that facilitate complex asynchronous processing.

## Performing blocking operations (e.g. IO)

Most applications are going to have to perform some kind of blocking IO.
Many Java APIs do not offer asynchronous options (e.g. JDBC).
Ratpack provides a simple mechanism for executing blocking operations in a _separate thread pool_.
This avoids blocking request processing (i.e. compute) threads (which is a good thing), but does incur some overhead due to thread contention.
If you have to use blocking IO APIs there is unfortunately no other option.

Let's consider a contrived data store API.
It is conceivable that communication with the actual data store requires IO (or if it is in memory, then its access requires waiting on one or more locks which has the same blocking effect).
The API methods cannot be called on a request processing thread because they will block.
Instead, we need to use the “blocking” API…

```language-groovy tested
import ratpack.handling.InjectionHandler;
import ratpack.handling.Context;
import ratpack.func.Action;
import java.util.concurrent.Callable;

public interface Datastore {
  int deleteOlderThan(int days) throws IOException;
}

public class DeletingHandler extends InjectionHandler {
  void handle(final Context context, final Datastore datastore) {
    final int days = context.getPathTokens().asInt("days");
    context.blocking(new Callable<Integer>() {
      public Integer call() {
        return datastore.deleteOlderThan(days);
      }
    }).then(new Action<Integer>() {
      public void execute(Integer result) {
        context.render(result + " records deleted");
      }
    });
  }
}
```

The use of anonymous inner classes adds some syntax weight here.
However, this API is very Java 8 [lambda syntax](http://docs.oracle.com/javase/tutorial/java/javaOO/lambdaexpressions.html) friendly.

In Groovy, it's much nicer…

```language-groovy groovy-handlers
import ratpack.groovy.handling.GroovyHandler
import ratpack.groovy.handling.GroovyContext
import ratpack.func.Action
import java.util.concurrent.Callable

public interface Datastore {
  int deleteOlderThan(int days) throws IOException
}

class DeletingHandler extends GroovyHandler {
  void handle(GroovyContext context) {
    int days = context.pathTokens.asInt("days")
    def datastore = context.get(Datastore)

    context.blocking {
      datastore.deleteOlderThan(days)
    } then {
      context.render("$it records deleted")
    }
  }
}

// Or when using the handlers dsl
handlers {
  get("deleteOlderThan/:days") { Datastore datastore ->
    blocking {
      datastore.deleteOlderThan(pathTokens.asInt("days"))
    } then {
      context.render("$it records deleted")
    }
  }
}
```

The `Callable` submitted as the blocking operation is executed asynchronously (i.e. the `blocking()` method returns instantly), in a separate thread pool.
The result that it returns will processed back on a request processing (i.e. compute) thread.

See the [Context#blocking(Callable)](api/ratpack/handling/Context.html#blocking\(java.util.concurrent.Callable\)) method for more details.

## Performing async operations

The [Context#promise(Action<Fulfiller\<T>>)](api/ratpack/handling/Context.html#promise\(ratpack.func.Action\)) for integrating with async APIs.
It is essentially a mechanism for adapting 3rd party APIs to Ratpack's promise type.

```language-groovy groovy-handlers
import ratpack.handling.*;
import ratpack.exec.Fulfiller;
import ratpack.func.Action;

public class PromiseUsingJavaHandler implements Handler {
  public void handle(final Context context) {
    context.promise(new Action<Fulfiller<String>>() {
      public void execute(final Fulfiller<String> fulfiller) {
        new Thread(new Runnable() {
          public void run() {
            fulfiller.success("hello world!");
          }
        }).start();
      }
    }).then(new Action<String>() {
      public void execute(String string) {
        context.render(string);
      }
    });
  }
}

class PromiseUsingGroovyHandler implements Handler {
  void handle(Context context) {
    context.promise { Fulfiller<String> fulfiller ->
      Thread.start {
        fulfiller.success("hello world!")
      }
    } then { String string ->
      context.render(string)
    }
  }
}

// Or when using the handlers dsl
handlers {
  get {
    promise { Fulfiller<String> fulfiller ->
      Thread.start {
        fulfiller.success("hello world!")
      }
    } then {
      render(it)
    }
  }
}
```

It is important to note that the promise is always fulfilled on a compute thread managed by Ratpack.
When the “fulfiller” is invoked from a non Ratpack thread (perhaps it's a thread managed by the 3rd party async API) the promise subscriber will be invoked on a Ratpack thread.

## Async composition and avoiding callback hell

One of the challenges of asynchronous programming lies in composition.
Non trivial asynchronous programming can quickly descend into a phenomenon known as “callback hell”, which is the term used to describe the incomprehensibility of many layers of nested callbacks.

Elegantly and cleanly composing async operations together into complex workflows is an area of rapid innovation at this time.
Ratpack does not attempt to provide a framework for asynchronous composition.
Instead, it aims to integrate and provide adapters to specialised tools for this task.
An example of this approach is [Ratpack's integration with RxJava](rxjava.html).

In general, integration is a matter of adapting Ratpack's [`Promise`](api/ratpack/exec/Promise.html) type with the composition primitive of the target framework.
