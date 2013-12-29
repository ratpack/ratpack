# Testing Ratpack applications

Ratpack provides testing support, both at the functional and unit level.
The `ratpack-test` library contains the core support, and `ratpack-test-groovy` provides convenient Groovy extensions.

Note: The `ratpack` and `ratpack-groovy` Gradle plugins auto configure these libraries to be added to the test classpath.

## Unit testing

The primary integration point between Ratpack and your code is the [`Handler`](api/ratpack/handling/Handler.html) contract.
Ratpack provides the [`UnitTest.invoke()`](api/ratpack/test/UnitTest.html#invoke\(ratpack.handling.Handler,%20ratpack.util.Action\)) and
[`GroovyUnitTest.invoke()`](api/ratpack/groovy/test/GroovyUnitTest.html#invoke\(ratpack.handling.Handler,%20groovy.lang.Closure\)) for contriving the invocation of a handler.
As the routing of requests is also implemented via handlers in Ratpack, this can also be used to test the routing.

Ratpack doesn't couple to any particular test framework.
It only provides some utilities that you can use from whatever test framework you choose.
However, we strongly recommend using the Groovy based [Spock Framework](http://www.spockframework.org), even if your application is implemented in Java.

There is no special setup or teardown required for unit testing Ratpack applications, as by definition they require no server infrastructure.

## Functional testing

TODO.

### Application subsets, modules and extensions

The [`ratpack.test.embed`](api/ratpack/test/embed/package-summary.html) package (provided by the `ratpack-test` library) provides the [`EmbeddedApplication`](api/ratpack/test/embed/EmbeddedApplication.html) interface and implementations.
These can be used to functionally test features in isolation, by creating small Ratpack apps within test cases and is actually the basis for how Ratpack itself is tested.
This approach is most commonly used for functionally testing reusable Ratpack components (e.g. [Guice modules](guice.html)) but can also be used for functionally testing a subset of an application.

The [`LaunchConfigEmbeddedApplication`](api/ratpack/test/embed/LaunchConfigEmbeddedApplication.html) is a convenient abstract super class,
where implementors only need to provide a [`LaunchConfig`](api/ratpack/launch/LaunchConfig.html) to define the application to test.
The [`ratpack.groovy.test.embed`](api/ratpack/groovy/test/embed/package-summary.html) package (provided by the `ratpack-groovy-test` library) provides the [`ClosureBackedEmbeddedApplication`](api/ratpack/groovy/test/embed/ClosureBackedEmbeddedApplication.html) implementation that uses user supplied closures as the basis of the application to test.
This is the preferred implementation to use as it provides Guice support, flexibility and is easy to use.

The [`EmbeddedApplication`](api/ratpack/test/embed/EmbeddedApplication.html) type extends the [`ApplicationUnderTest`](api/ratpack/test/ApplicationUnderTest.html) type.
This makes them convenient to use with the [`TestHttpClient`](api/ratpack/groovy/test/TestHttpClients.html) mechanism provided by the `ratpack-groovy-test` library.