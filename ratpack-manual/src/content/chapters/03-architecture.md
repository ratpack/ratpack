# Architecture

This chapter describes Ratpack applications at a high level.

## Strongly typed

Ratpack is strongly typed.
Beyond being implemented in Java, a strongly typed language, its API embraces types.
For example, the notion of a [`Registry`](api/ratpack/registry/Registry.html) is used extensively in Ratpack.
A `Registry` can be thought of as a map that uses types as keys.

This may be of most interest to Ratpack users implementing their applications in Groovy.
Ratpack's [Groovy adapter](groovy.html) uses the latest Groovy features to fully support static typing, while maintaining an idiomatic, concise, Groovy API.

## Non blocking

Ratpack at its core is an event based (i.e. non-blocking) HTTP IO engine, and an API that makes it easy to structure response logic.
Being non blocking imposes a different style of API than “traditional” blocking Java APIs in that the API must be _asynchronous_.

Ratpack aims to significantly simplify this style of programming for HTTP applications.
It provides support for structuring asynchronous code (see the [“Asynchronous & Non Blocking”](async.html) chapter), 
and uses an innovative approach for structuring request processing into a self building, asynchronously traversed, graph of functions (it's nowhere near as complicated to use as that may sound).
  
## The parts   

> In the following section, “quotes” are used to denote key Ratpack terms and concepts.

A Ratpack application begins with a “launch configuration”, which as you would assume provides the configuration that is needed to start the application.
A Ratpack “server” can be built and started solely from a “launch configuration”.
The “server” once started, starts listening for requests. 
See the [“Launching”](launching.html) chapter for more detail on this aspect.

One key piece of configuration provided to the “launch configuration” is the “handler factory”, which creates a “handler”.
The “handler” is asked to respond to each request.
A handler can do one of three things:

1. Respond to the request
2. Delegate to the “next” handler
3. “Insert” handlers and immediately delegate to them

All request processing logic is simply the composition of handlers (see the [`Handlers`](handlers.html) chapter for more detail on this aspect). 
Importantly, the processing is not bound to a thread and can be completed asynchronously.
The “handler” API supports this asynchronous composition.

Handlers operate on a “context”.
A “context” represents the state of request processing at that particular point in the handler graph.
One of its key functions is to act as a “registry”, that can be used to retrieve objects by type.
This allows handlers to retrieve _strategy_ objects (typically just objects implementing key interfaces) from the “context” by public types.
As handlers insert other handlers into the handler graph, they can contribute to the context registry.
This allows handlers to contribute code (as strategy objects) to downstream handlers.
See the [“Context”](context.html) chapter for more detail, and the following section for how this context registry is used in practice.

> This has been a high level, abstract, description of a Ratpack application.
> It is likely unclear exactly how all this translates to real code.
> The rest of this manual, and the accompanying API reference will provide the detail.

## Plugins and extensibility through the Registry

Ratpack has no notion of plugins.
However, add-on [integration with Google Guice](guice.html) facilitates a kind of plugin system through Guice modules.
Guice is a dependency injection container.
Guice modules define objects to be part of the dependency injection container.
Guice modules can act as plugins by providing implementations of key Ratpack interfaces, that are used by handlers.
When using the Guice integration, all of the objects known to Guice (typically through Guice modules) are obtainable via the “context registry”.
That is, handlers can retrieve them by type.

To see why this is useful, we will use the requirement of rendering an object as JSON to the response.
The “context” object given to a “handler” has a [render(Object)](api/ratpack/handling/Context.html#render-java.lang.Object-) method.
The implementation of this method simply searches the context registry for an implementation of [`Renderer`](api/ratpack/render/Renderer.html)
that can render objects of the given type. 
Because objects available to Guice are available through the registry, they may be used for rendering.
Therefore, adding a Guice module with a `Renderer` implementation for the desired type will allow it to be integrated into request processing.
This is no different in concept to plain dependency injection.

While we have used the Guice integration in the above example, this approach is not tied to Guice (Guice is not part of Ratpack's core API).
Another dependency injection container (such as Spring) could easily be used, or no container at all.
Any source of objects can be adapted to Ratpack's [`Registry`](api/ratpack/registry/Registry.html) interface (there is also [a builder](api/ratpack/registry/RegistryBuilder.html)).

## Services & business logic

Ratpack has no opinion on how you structure your code that isn't related to request handling (i.e. business logic).
We'll use the term “service” as a catch all for an object that performs some kind of business logic.

Handlers can of course freely use whatever services they need to.
There are two main patterns for access services from handlers:

1. Provide the service to the handler when it is constructed
2. Retrieve the service from the context registry
