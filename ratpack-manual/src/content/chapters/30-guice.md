# Google Guice integration

The `ratpack-guice` extension provides integration with [Google Guice](https://code.google.com/p/google-guice/).
The primary feature of this extension is to allow the server registry to be built by Guice.
That is, a Guice [`Injector`](http://google.github.io/guice/api-docs/4.0/javadoc/?com/google/inject/Injector.html) can be presented as a Ratpack [Registry](api/?ratpack/registry/Registry.html).
This allows the wiring of the application to be specified by Guice modules and bindings, but still allowing the registry to be the common integration layer between different Ratpack extensions at runtime.

The `ratpack-guice` module as of @ratpack-version@ is built against (and depends on) Guice @versions-guice@ (and [the multibindings extension](https://github.com/google/guice/wiki/Multibindings)).

## Modules

Guice provides the concept of a module, which is a kind of recipe for providing objects.
See Guice's [“Getting Started”](https://code.google.com/p/google-guice/wiki/GettingStarted) documentation for details.

The `ratpack-guice` library provides the [`BindingsSpec`](api/ratpack/guice/BindingsSpec.html) type for specifying the bindings for the application.

## Dependency injected handlers

The Guice integration gives you a way to decouple the components of your application.
You can factor out functionality into standalone (i.e. non `Handler`) objects and use these objects from your handlers.
This makes your code more maintainable and more testable.
This is the standard “Dependency Injection” or “Inversion of Control” pattern.

The [Guice](api/ratpack/guice/Guice.html) class provides static `handler()` factory methods for creating root handlers that are the basis of the application.
These methods (the commonly used ones) expose [`Chain`](api/ratpack/handling/Chain.html) instance that can be used to build the application's handler chain.
The instance exposed by these methods provide a registry (via the [`getRegistry()`](api/ratpack/handling/Chain.html#getRegistry--)) method that can be used
to construct dependency injected handler instances.

See the documentation of the [Guice](api/ratpack/guice/Guice.html) class for example code.

## Guice and the context registry

TODO guice backed registry impl

This offers an alternative to dependency injected handlers, as objects can just be retrieved on demand from the context.

More usefully, this means that Ratpack infrastructure can be integrated via Guice modules.
For example, an implementation of the [`ServerErrorHandler`](api/ratpack/error/ServerErrorHandler.html) can be provided by a Guice module.
Because Guice bound objects are integrated into the context registry lookup mechanism, this implementation will participate in the error handling infrastructure.

This is true for all Ratpack infrastructure that works via context registry lookup, such as [Renderer](api/ratpack/render/Renderer.html) implementations for example.
