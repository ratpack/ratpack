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