# Handlers

Handlers are the fundamental unit in Ratpack applications.
The entire request processing mechanism consists solely of invoking handlers.

## What is a handler?

A handler is a function.
It operates within a handling _context_, which encompasses the request/response exchange among other things discussed later in this chapter.
 
Handlers may send a response back to the client, finalizing processing, or delegate in some way to another handler.
For example, a handler may inspect the path of the request and delegate to one handler if the path matches a certain value and another handler if it does not.
Sophisticated processing _pipelines_ can be created just by composing handlers (i.e. functions). 
The concept of a handler is a rather abstract one.  
Yet, it offers a simple but powerfully flexible model for defining processing logic.

A handler implements the simple [`Handler`](api/ratpack/handling/Handler.html) interface.
Handlers are typically singletons and operate on many different contexts, potentially concurrently.

## The context

Handlers handle within a context, which is the only argument to the handler's `handle()` method (which is the only method of the interface).
It is of type [`Context`](api/ratpack/handling/Context.html)

The context provides:

1. The request/response pair ([`getRequest()`](api/ratpack/handling/Context.html#getRequest%28%29)
& [`getResponse()`](api/ratpack/handling/Context.html#getResponse%28%29))
1. Mechanisms for delegating to other handlers (i.e. chaining, discussed below)
1. Means for handlers to communicate (i.e. the registry, discussed below)
1. Convenience methods for common kinds of handler communications (e.g. [`getPathTokens()`](api/ratpack/handling/Context.html#getPathTokens%28%29))
1. A mechanism for executing blocking IO operations (i.e. [`getBackground()`](api/ratpack/handling/Context.html#getBackground%28%29), discussed below)
1. Various other convenience utilities (e.g. responders)

The context is how the handler interacts with the rest of the application.
Its different functions and uses will be discussed below.

## Handler chaining

Most applications are too complex to be specified as a single handler. 
Instead, the request handling logic is broken up into separate reusable steps that compose together to form the concept of a handler chain.

For each invocation of a handler's `handle()` method, the handler is connected to a _next_ handler.
A handler can pass responsibility on to the next handler in the chain by calling the [`next()`](api/ratpack/handling/Context.html#next%28%29) method of the context.
This will ultimately invoke the `handle()` method of the next handler in the chain. 
The very last handler in the chain is _always_ a handler that simply responds to the client with a 404 status code and empty body.

Rather than just delegate to the predefined next handler, handlers can also [`insert()`](api/ratpack/handling/Context.html#insert%28java.util.List%29) one or more handlers into the chain before passing control to the first one that was inserted. The second inserted becomes the next handler of the first inserted and so on.
The last inserted handler's next handler becomes the next handler of the handler that performed the insert.

Handler chains can be constructed by using a [`Chain`](api/ratpack/handling/Chain.html), which is really a chain builder.
You can use [`Handlers.chain()`](pi/org/ratpackframework/handling/Handlers.html#chain%28ratpack.util.Action%29) method to build a handler chain.
Note that a handler chain is just an implementation of `Handler`.
A handler chain can actually be composed of other handler chains.

The term “chain” is somewhat of an intentional misnomer.
Most Ratpack applications are really trees or graphs of handlers.
However, a chain is a more useful analogy in practice.

## Built in handlers

The [`Handlers`](api/ratpack/handling/Handlers.html) class provides static methods to compose standard types of handlers.

As an example, the [`Handlers.post(String path, Handler delegate)`](api/ratpack/handling/Handlers.html#post%28java.lang.String%2C%20ratpack.handling.Handler%29) can be used for creating handlers that respond to HTTP POST requests at a certain path.
It takes a path and a `Handler`, and returns a `Handler`. 

The static methods of this class form the basis of implementing handlers in Ratpack.
Typically you will want to write a handler that implements business logic and doesn't contain any _routing_ (e.g. request path, request method etc.) logic.
A final handler is composed from your business logic handler and the static methods of the [`Handlers`](api/ratpack/handling/Handlers.html) class.

It's worth noting that specifying handlers in a Groovy based Ratpack application is typically a little different.
In a Groovy application you use a Closure based Domain Specific Language (DSL) to build a composed handler.
This DSL is provided by the [`Chain`](api/ratpack/groovy/handling/Chain.html) interface of the Groovy module (which extends the interface of the same name in the core module).

## The context registry

The handler [context](api/ratpack/handling/Context.html) also serves as a [registry](api/ratpack/registry/Registry.html).
Objects can be retrieved from the registry via type.
Ratpack pre-populates the context registry with some key services and values. 
See the [`Context`](api/ratpack/handling/Context.html) documentation for details.

Handlers can also register items with the registry for handlers that they [insert](api/ratpack/handling/Context.html#insert%28java.lang.Object%2C%20java.util.List%29).
This makes new items available from the context, _just_ for the inserted handlers.
That is, the registration is scope to just those handlers.


## `HandlerFactory`

One of the roles of the launch configuration is to provide a [`HandlerFactory`](api/ratpack/launch/HandlerFactory.html) implementation.
This factory provides a [`Handler`](api/ratpack/handling/Handler.html) that effectively is the Ratpack application.
Different “modes” of Ratpack applications may not explicitly require a handler factory implementation. For example, the Groovy module supplies a handler factory implementation that delegates to a user provided Groovy script that defines the handler.