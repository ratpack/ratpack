# Testing Ratpack applications

Ratpack provides testing support, both at the functional and unit level.
The `ratpack-test` library contains the core support, and `ratpack-groovy-test` provides convenient Groovy extensions.

Note: The `ratpack` and `ratpack-groovy` Gradle plugins auto configure these libraries to be added to the test classpath.

## Unit testing

The primary integration point between Ratpack and your code is the [`Handler`](api/ratpack/handling/Handler.html) contract.
Ratpack provides the [`RequestFixture.handle()`](api/ratpack/test/handling/RequestFixture.html#handle-ratpack.handling.Handler-ratpack.func.Action-) and
[`GroovyRequestFixture.handle()`](api/ratpack/groovy/test/handling/GroovyRequestFixture.html#handle-ratpack.handling.Handler-groovy.lang.Closure-) for contriving the invocation of a handler.
As the routing of requests is also implemented via handlers in Ratpack, this can also be used to test the routing.

Ratpack doesn't couple to any particular test framework.
It only provides some utilities that you can use from whatever test framework you choose.
However, we strongly recommend using the Groovy based [Spock Framework](http://www.spockframework.org), even if your application is implemented in Java.

There is no special setup or teardown required for unit testing Ratpack applications, as by definition they require no server infrastructure.

## Functional testing
Functional testing for Ratpack is built up around the [ApplicationUnderTest](api/ratpack/test/ApplicationUnderTest.html).

This interface provides the address of the running application. Implementations of this interface will take care of starting the server for you.

### TestHttpClient

Ratpack provides [TestHttpClient](api/ratpack/test/http/TestHttpClient.html) in `ratpack-groovy-test`, this is a client that makes it very simple to test status codes and responses.

Note below we use @Delegate so we just need to call `get()` in the when block instead of `client.get()`.

```language-groovy tested-dynamic

import ratpack.groovy.test.GroovyRatpackMainApplicationUnderTest
import ratpack.test.http.TestHttpClient
import ratpack.test.ServerBackedApplicationUnderTest
import spock.lang.*

class SiteSmokeSpec extends Specification {

  ServerBackedApplicationUnderTest aut = new GroovyRatpackMainApplicationUnderTest()
  @Delegate TestHttpClient client = TestHttpClient.testHttpClient(aut)

  def "Check Site Index"() {
	when:
    get("index.html")

    then:
    assert response.statusCode == 200
    assert response.body.text.contains('<title>Ratpack: A toolkit for JVM web applications</title>')

  }

  def "Check Site Root"() {
	when:
    get()

	then:
    assert response.statusCode == 200
    assert response.body.text.contains('<title>Ratpack: A toolkit for JVM web applications</title>')
  }

  def cleanup() {
    aut.stop()
  }

}
``` 

### Geb

[Geb](http://www.gebish.org/) can also be used we just need to set up the correct base URL and make sure the test app is shut down.

To set the correct base URL we will use the ServerBackedApplicationUnderTest instance to get the address and give that to the Geb browser instance.

For shutting down the app we will call stop in the cleanup function.

An example of a Geb based test is available [here](https://github.com/ratpack/ratpack/blob/master/ratpack-site/src/browserTest/groovy/ratpack/site/SiteBrowserSmokeSpec.groovy).

### Application subsets, modules and extensions

The [`ratpack.test.embed`](api/ratpack/test/embed/package-summary.html) package (provided by the `ratpack-test` library) provides the [`EmbeddedApp`](api/ratpack/test/embed/EmbeddedApp.html) type.
These can be used to functionally test features in isolation, by creating small Ratpack apps within test cases and is actually the basis for how Ratpack itself is tested.
This approach is most commonly used for functionally testing reusable Ratpack components (e.g. [Guice modules](guice.html)) but can also be used for functionally testing a subset of an application.

The [`ratpack.groovy.test.embed`](api/ratpack/groovy/test/embed/package-summary.html) package (provided by the `ratpack-groovy-test` library) provides the [`GroovyEmbeddedApp`](api/ratpack/groovy/test/embed/GroovyEmbeddedApp.html) Groovy specialization.
This is the preferred implementation to use as it provides Guice support, flexibility and is easy to use.

The [`EmbeddedApp`](api/ratpack/test/embed/EmbeddedApp.html) type extends the [`ApplicationUnderTest`](api/ratpack/test/ApplicationUnderTest.html) type.
This makes them convenient to use with the [`TestHttpClient`](api/ratpack/test/http/TestHttpClient.html) mechanism provided by the `ratpack-groovy-test` library.
