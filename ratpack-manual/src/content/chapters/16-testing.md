# Testing Ratpack applications

Testing is a first class citizen in Ratpack.
The `ratpack-test` library contains the core support, and `ratpack-groovy-test` provides some Groovy sugar to these types.

> The `ratpack` and `ratpack-groovy` Gradle plugins add these libraries implicitly to the test compile classpath.

The Ratpack test support is agnostic of the test framework in use.
Any framework can potential be used.

Many Ratpack users use the [Spock testing framework](http://spockframework.org).
While Spock requires writing tests in Groovy, it can effortlessly be used to effectively test Java code.

## Unit testing

### RequestFixture

The [`RequestFixture`](api/ratpack/test/handling/RequestFixture.html#handle-ratpack.handling.Handler-ratpack.func.Action-) class
facilitates creating a mocked request environment, ostensibly for testing [`Handler`](api/ratpack/handling/Handler.html) implementations.
However, it is also common to use an ad-hoc handler with a request fixture that integrates with other components (e.g. [`Parser`](api/ratpack/parse/Parser.html) implementations).

> Note: the [`GroovyRequestFixture`](api/ratpack/groovy/test/handling/GroovyRequestFixture.html) class provides Groovy sugar for working with request fixtures.
 
### ExecHarness

The [`ExecHarness`](api/ratpack/test/exec/ExecHarness.html) fixture facilitates testing code that leverages Ratpack's execution mechanisms outside of an application.
If you need to unit test code that uses [`Promise`](api/ratpack/exec/Promise.html), an exec harness is what you need.

## Integration testing

Ratpack integration tests are tests that test a subset of application components, via the HTTP interface.
 
The [`EmbeddedApp`](api/ratpack/test/embed/EmbeddedApp.html) fixture facilitates constructing an ad-hoc application that responds to real HTTP requests.
In the context of integration testing, it is typically used to glue together a specific combination of application components to test.

As it constructs a real Ratpack application, it can also be used for testing implementations of Ratpack extension points such as [`Renderer`](api/ratpack/render/Renderer.html), [`Parser`](api/ratpack/parse/Parser.html) and [`ConfigurableModule`](api/ratpack/guice/ConfigurableModule.html).

The `EmbeddedApp` fixture manages starting and stopping the application, as well as providing a [`TestHttpClient`](api/ratpack/test/http/TestHttpClient.html) that makes requests of the embedded application.

Importantly, embedded apps must be closed when they are no longer needed in order to free resources.
The `EmbeddedApp` type implements the [`java.io.AutoCloseable`](https://docs.oracle.com/javase/8/docs/api/java/lang/AutoCloseable.html) interface, who's `close()` method can be used to stop the server.
This can often be combined with the “after test” lifecycle event of the testing framework being used, such as JUnit's `@After` methods.

> Note: the `EmbeddedApp` fixture can also be used “standalone” for creating mocked HTTP services when testing other types of, non Ratpack, applications.  

## Functional testing

Ratpack functional tests are tests that test an entire application, via the HTTP interface.

For Ratpack apps that are defined as a Java main class, the [`MainClassApplicationUnderTest`](api/ratpack/test/MainClassApplicationUnderTest.html) fixture can be used.
For Ratpack app that are defined as a Groovy script, the [`GroovyRatpackMainApplicationUnderTest`](api/ratpack/groovy/test/GroovyRatpackMainApplicationUnderTest.html) fixture can be used.

If you have a custom entry point, the [`ServerBackedApplicationUnderTest`](api/ratpack/test/ServerBackedApplicationUnderTest.html) abstract super class can be extended for your needs.

These fixtures manage starting and stopping the application, as well as providing a [`TestHttpClient`](api/ratpack/test/http/TestHttpClient.html) that makes requests of the embedded application.

Importantly, applications under test must be closed when they are no longer needed in order to free resources.
The `CloseableApplicationUnderTest` type implements the [`java.io.AutoCloseable`](https://docs.oracle.com/javase/8/docs/api/java/lang/AutoCloseable.html) interface, who's `close()` method can be used to stop the server.
This can often be combined with the “after test” lifecycle event of the testing framework being used, such as JUnit's `@After` methods. 

### Impositions
 
Ratpack provides a mechanism for augmenting applications under test for testability, known as [`impositions`](api/ratpack/impose/Impositions.html).

Typically, impositions are specified by sub-classing [`MainClassApplicationUnderTest`](api/ratpack/test/MainClassApplicationUnderTest.html) or similar, and overriding the 
[`addImpositions(ImpositionsSpec)`](api/ratpack/test/ServerBackedApplicationUnderTest.html#addImpositions-ratpack.impose.ImpositionsSpec-) method.
 
### Browser testing

Browser testing works similarly to what has been previously named as functional testing here, except that usage of Ratpack's `TestHttpClient` is replaced with browser automation.
This typically involves using [`MainClassApplicationUnderTest`](api/ratpack/test/MainClassApplicationUnderTest.html) to start and stop the app, 
and to provide the application under test's address via the [`getAddress()`](api/ratpack/test/ApplicationUnderTest.html#getAddress--) method.

Ratpack users commonly use [Geb](http://www.gebish.org/) for browser tests as its expressive style and synergy with [Spock](http://spockframework.org) suit well. 
An example of a Ratpack/Geb based test for the `ratpack.io` site is [available for reference](https://github.com/ratpack/ratpack/blob/master/ratpack-site/src/browserTest/groovy/ratpack/site/SiteBrowserSmokeSpec.groovy).
