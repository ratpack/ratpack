# Blocking IO

By default, Ratpack is based on [non blocking (or async) IO](http://en.wikipedia.org/wiki/Asynchronous_I/O) which is more efficient than blocking (or sync) IO based
applications and therefore supports greater throughput.
A key implication of this is that handlers must make special provisions if they do need to perform blocking IO.

Non blocking apps use a small pool of threads to handle requests, roughly equivalent to the number of processing units available to the JVM running the application.
This is possible because these request threads are (ideally) always doing work instead of waiting for IO.
This means that wherever possible you should use libraries to access external resources (e.g. files, databases, web services) that support asynchronous IO.
However, this is not always possible.
For example, many database access libraries only support blocking on IO.
In such cases, you can use Ratpack's “blocking” API to perform the blocking operation off the request handling threads.

Ratpack can also be used to write blocking applications (i.e. using a large thread pool for handling requests).
This can result is simpler application code, but greatly reduced throughput.
See [the bootstrapping chapter](bootstrapping.html) chapter for details on how to launch a Ratpack app in this mode.

## The blocking API

The context object offers the [getBlocking()](api/ratpack/handling/Context.html#getBlocking\(\)) method that returns a [Blocking](api/ratpack/block/Blocking.html) object.
This gives access to the background thread pool for performing blocking operations.
The [blocking(Callable)](api/ratpack/handling/Context.html#blocking\(java.util.concurrent.Callable\)) method provides a more convenient entry point for this API.

The Groovy API offers a more Groovy friendly API via the [blocking(Closure)](api/ratpack/groovy/handling/GroovyContext.html#blocking\(groovy.lang.Closure\)) of the
[GroovyContext](api/ratpack/groovy/handling/GroovyContext.html) type.