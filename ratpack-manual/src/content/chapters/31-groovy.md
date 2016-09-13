# Groovy

[Groovy](http://www.groovy-lang.org/) is an alternative JVM programming language.
It has a strong synergy with Java and many language and library features that make it a compelling programming environment.
Ratpack provides strong integration with Groovy via the `ratpack-groovy` and `ratpack-groovy-test` libraries.
Writing Ratpack applications in Groovy generally leads to less code through Groovy's concise syntax compared to Java and a more productive and enjoyable development experience.
To be clear though, Ratpack applications do not _need_ to be written in Groovy.

Groovy is commonly known as a dynamic language.
However, Groovy 2.0 added full static typing and static compilation as an option.
Ratpack's Groovy support is strictly designed to fully support “static Groovy” and also leverages the newest features of Groovy to avoid introducing boilerplate code to achieve this goal.
Said another way, Ratpack's Groovy support does not use any dynamic language features and has a strongly typed API.

> TODO: find decent links describing static Groovy and use above

## Prerequisites

If you are new to Groovy, you may want to research the following foundational Groovy topics before proceeding:

1. Closures
1. The `def` keyword

> TODO: what else should be in this list? Also, links needed
>
> something else

## Ratpack Groovy API

> TODO: explain that Groovy API wraps Java API and mirrors it in corresponding ratpack.groovy.*

### @DelegatesTo

`@DelegatesTo` is aimed at documenting code and providing the IDE and static type checker/compiler with more type information at compile-time. Applying the annotation is particularly
interesting for DSL authors.

Let's consider the following Ratpack code snippet:

```language-groovy
ratpack {
  handlers {
    get {
      render "Hello world!"
    }
  }
}
```

This code is also known as being written in Ratpack's "GroovyChain DSL". It is essentially the same as

```language-groovy
ratpack({
  handlers({
    get({
      render("Hello world!")
    })
  })
})
```

The methods `ratpack`, `handlers`, `get` and `render` are called. In fact there is no restriction to call each method only once,
for example, the code could have had multiple method invocations of `get`, responding to different request URIs:

```language-groovy
ratpack {
  handlers {
    get {
      render "Hello world!"
    }

    get('foo')  {
      render "bar"
    }
  }
}
```

Notice how the calls to `ratpack`, `handlers` and `get` build a hierarchy. But how is it actually transformed to calls to Groovy/Java objects?

That's exactly where _delegation_ comes into play. Groovy allows to change the target of method calls inside `Closure` code blocks. Let's have
a look at a very basic example:

```language-groovy
class Settings {
   String host
   Integer port

   def host(String h) { this.host = h }
   def port(Integer p) { this.port = p }
}

Settings settings(Closure dsl)  {
    def p = new Settings()
    def code = dsl.clone() // better use: dsl.rehydrate(p, this, this)
    code.delegate = p
    code()
    return p
}

// our DSL starts here and returns a Settings instance
Settings config = settings {
  port 1234
  host 'localhost'
}

assert config.host == 'localhost'
assert config.port == 1234
```

The code in the `settings` DSL block calls methods which do not exist in the current lexical scope. At runtime, once the `delegate` property
is set though, Groovy additionally resolves the method against the given delegate object, in our case, the `Settings` instance.

Delegation is commonly used in Groovy DSLs, as it is the case with the Ratpack DSL, to decouple DSL code from underlying objects.

This technique bears a problem for code completion in IDEs or the static type checker/compiler that has been added in Groovy 2. Running the following
code in `groovyConsole`

```language-groovy
class Settings {
   String host
   Integer port

   def host(String h) { this.host = h }
   def port(Integer p) { this.port = p }
}

Settings settings(Closure dsl)  {
    def p = new Settings()
    def code = dsl.clone() // better use: dsl.rehydrate(p, this, this)
    code.delegate = p
    code()
    return p
}

@groovy.transform.TypeChecked
void createConfig() {
	Settings config = settings {
	  port 1234
	  host 'localhost'
	}

	assert config.host == 'localhost'
	assert config.port == 1234
}
```

gives

```language-groovy
[Static type checking] - Cannot find matching method ConsoleScript23#port(int). Please check if the declared type is right and if the method exists.
 at line: 20, column: 7

[Static type checking] - Cannot find matching method ConsoleScript23#host(java.lang.String). Please check if the declared type is right and if the method exists.
 at line: 21, column: 7
```

The type checker misses information about the delegate type `Settings` at compile-time.

This is the point where `@DelegatesTo` finally comes into play. It is exactly used in cases where this type information needs to be specified
for `Closure` method parameters:

```language-groovy
// ...

// let's tell the compiler we're delegating to the Settings class
Settings settings(@DelegatesTo(Settings) Closure dsl)  {
    def p = new Settings()
    def code = dsl.clone() // better use: dsl.rehydrate(p, this, this)
    code.delegate = p
    code()
    return p
}

// ...
```

Ratpack uses `@DelegatesTo` wherever `Closure` method parameters are used. This does not only serve better code completion or the static
type checker, but also for documentation purposes.

## ratpack.groovy script

> TODO: introduce DSL used in this file, discuss reloading when in development mode

## handlers {} DSL

> TODO: introduce the `GroovyChain` DSL, and closures as handlers

## Testing

Groovy comes with built-in support for writing tests. Besides integrated support for JUnit, the programming language comes with
features proven to be very valuable for test-driven development. One of them is the extended `assert` keyword on which
we'll have a look at in the next section.

If you're looking for Ratpack-specific test support documentation, there is a dedicated [Ratpack testing guide](testing.html) section in this
manual.

### Power assertions

Writing tests means formulating assumptions by using assertions. In Java this can be done by using the `assert` keyword that has been added
in J2SE 1.4. Assertion statements in Java are disabled by default.

Groovy comes with a powerful variant of `assert` also known as _power assertion statement_. Groovy’s power `assert` differs from the Java
version in its output given once the boolean expression validates to `false`:

```language-groovy
def x = 1
assert x == 2

// Output:
//
// Assertion failed:
// assert x == 2
//        | |
//        1 false
```

The `java.lang.AssertionError` contains an extended version of the original assertion error message. Its output shows all variable and
expression values from the outer to the inner most expression.

Due to its expressional power, it has become common sense in the Groovy community to write test
cases with the `assert` statement instead of any `assert*` methods provided by the testing library of choice.

The `assert` statement is only one of the many useful Groovy features for writing tests. If you're looking
for a comprehensive guide of all Groovy language testing features, please consult the [Groovy testing guide]().

### JUnit 3 support

Groovy comes with embedded JUnit 3 support and a custom `TestCase` base class: `groovy.util.GroovyTestCase`. `GroovyTestCase`
extends `TestCase` and adds useful utility methods.

One example is the `shouldFail` method family. Each `shouldFail` method takes a `Closure` and executes it. If an exception is thrown and the
code block fails, `shouldFail` catches the exception and the test method does not fail:

```language-groovy tested
import groovy.util.GroovyTestCase

class ListTest extends GroovyTestCase {

  void testInvalidIndexAccess1() {
    def numbers = [1,2,3,4]

    shouldFail {
      numbers.get(4)
    }
  }
}
```

`shouldFail` in fact returns the catched exception which allows for asserting on exception messages or types:

```language-groovy tested
import groovy.util.GroovyTestCase

class ListTest extends GroovyTestCase {

  void testInvalidIndexAccess1() {
    def numbers = [1,2,3,4]

    def msg = shouldFail {
      numbers.get(4)
    }

    assert msg == 'Index: 4, Size: 4'
  }
}
```

In addition there is a variant of the `shouldFail` method that comes with a `java.lang.Class` parameter before the `Closure`
parameter:

```language-groovy tested
import groovy.util.GroovyTestCase

class ListTest extends GroovyTestCase {

  void testInvalidIndexAccess1() {
    def numbers = [1,2,3,4]

    def msg = shouldFail(IndexOutOfBoundsException) {
      numbers.get(4)
    }

    assert msg == 'Index: 4, Size: 4'
  }
}
```

If the thrown exception would be another type, `shouldFail` would rethrow the exception letting the test method fail

A complete overview of all`GroovyTestCase` methods can be found in the [JavaDoc documentation](http://docs.groovy-lang.org/@groovy-version@/html/api/groovy/util/GroovyTestCase.html).

### JUnit 4 support

Groovy comes with embedded JUnit 4 support. As of Groovy 2.3.0, the `groovy.test.GroovyAssert` class can be seen as complement to
Groovy's `GroovyTestCase` JUnit 3 base class. `GroovyAssert` extends `org.junit.Assert` by adding static utility methods for writing JUnit 4
tests in Groovy.

```language-groovy
import static groovy.test.GroovyAssert.shouldFail

class ListTest {
  void testInvalidIndexAccess1() {
    def numbers = [1,2,3,4]
    shouldFail {
      numbers.get(4)
    }
  }
}
```

A complete overview of all `GroovyAssert` methods can be found in the [JavaDoc documentation](http://docs.groovy-lang.org/@groovy-version@/html/api/groovy/test/GroovyAssert.html).

### Spock

Spock is a testing and specification framework for Java and Groovy applications. What makes it stand out from the crowd is its
beautiful and highly expressive specification DSL. Spock specifications are written as Groovy classes.

Spock can be used for unit, integration or BDD (behavior-driven-development) testing, it doesn’t put itself into a specific category of
testing frameworks or libraries.

In the following paragraphs we will have a first look at the anatomy of a Spock specification. It is not meant to be a full blown documentation,
but rather give a pretty good feeling on what Spock is up to.

#### First steps

Spock lets you write specifications that describe features (properties, aspects) exhibited by a system of interest. The "system"
can be anything between a single class and an entire application, a more advanced term for it is _system under specification_. The
feature description starts from a specific snapshot of the system and its collaborators, this snapshot is called the _feature’s
fixture_.

Spock specification classes are automatically derived from `spock.lang.Specification`. A concrete specification class might consist of fields,
fixture methods, features methods and helper methods.

Let's have a look at a Ratack unit test specification:

```language-groovy tested
import ratpack.groovy.test.GroovyRatpackMainApplicationUnderTest
import ratpack.test.http.TestHttpClient
import ratpack.test.ServerBackedApplicationUnderTest

class SiteSpec {

  ServerBackedApplicationUnderTest aut = new GroovyRatpackMainApplicationUnderTest()
  @Delegate TestHttpClient client = TestHttpClient.testHttpClient(aut)

  def "Check Site Index"() {
    when:
      get("index.html")

    then:
      response.statusCode == 200
      response.body.text.contains('<title>Ratpack: A toolkit for JVM web applications</title>')
  }
}
```

Spock feature specifications are defined as methods inside a  `spock.lang.Specification ` class. They describe the feature by using a
String literal instead of a method name. The specification above uses `"Check Site Index"` to test the outcome of a request to `index.html`.

The feature specification uses the `when` and `then` blocks. The `when` block creates a so-called _stimulus_ and is a companion of the `then` block
which describes the response to the stimulus. Notice how we can also leave out the `assert` statements in the `then` block. Spock will interpret
Boolean expressions there correctly. The `setup` block could have been used to configure local variables visible inside the feature method only.

#### More on Spock

Spock provides more advanced features like data tables or mocking we didn't have a look at in this section. Feel free to consult the [Spock
GitHub page](https://github.com/spockframework/spock) for more documentation.
