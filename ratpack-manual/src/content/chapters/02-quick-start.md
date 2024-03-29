# Quick Start

This chapter provides instructions on how to get a Ratpack application up and running to play with.

## Using a Groovy script

A Ratpack application can be implemented as a single Groovy script.
This is a useful way to experiment with Ratpack and Groovy.

First, [install Groovy](http://groovy-lang.org/install.html).

Create the file `ratpack.groovy` with the following content:
 
```language-groovy hello-world-grab
@Grapes([
  @Grab('io.ratpack:ratpack-groovy:@ratpack-version@'),
  @Grab('org.slf4j:slf4j-simple:@slf4j-version@')
])
import static ratpack.groovy.Groovy.ratpack

ratpack {
    handlers {
        get {
            render "Hello World!"
        }
        get(":name") {
            render "Hello $pathTokens.name!"
        }
    }
}
``` 

You can now start the app by running the following on the command line:

```language-bash
groovy ratpack.groovy
```

The server will be available via `http://localhost:5050/`.

The [`handlers()` method](api/ratpack/groovy/Groovy.Ratpack.html#handlers%28groovy.lang.Closure%29) takes a closure that delegates to a [`GroovyChain`](api/ratpack/groovy/handling/GroovyChain.html) object.
The “Groovy Handler Chain DSL” is used to build the response handling strategy.

Changes to the file are live during development.
You can edit the file, and the changes will take effect on the next request.

## Using the Gradle plugin(s)

We recommend the use of the [Gradle build system](http:///www.gradle.org) to build Ratpack applications.
Ratpack does not require Gradle; any build system can be used.

> The following instructions assume you have already installed Gradle.
> See the [Gradle User Guide](https://docs.gradle.org/current/userguide/installation.html) for installation instructions.

The Ratpack project provides two Gradle plugins:

1. [io.ratpack.ratpack-java](http://plugins.gradle.org/plugin/io.ratpack.ratpack-java) - for Ratpack applications implemented in Java
2. [io.ratpack.ratpack-groovy](http://plugins.gradle.org/plugin/io.ratpack.ratpack-groovy)  - for Ratpack applications implemented in [Groovy](http://groovy-lang.org)
 
> For a more detailed explanation of the Gradle build support, please see the [dedicated chapter](gradle.html).

### Using the Gradle Java plugin

Create a `build.gradle` file with the following contents:

```language-groovy gradle
buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath "io.ratpack:ratpack-gradle:@ratpack-version@"
  }
}

apply plugin: "io.ratpack.ratpack-java"
apply plugin: "idea"

repositories {
  mavenCentral()
}

dependencies {
  runtimeOnly "org.slf4j:slf4j-simple:@slf4j-version@"
}

mainClassName = "my.app.Main"
```

Create the file `src/main/java/my/app/Main.java`, with the following content:

```language-java hello-world
package my.app;

import ratpack.core.server.RatpackServer;

public class Main {
  public static void main(String... args) throws Exception {
    RatpackServer.start(server -> server 
      .handlers(chain -> chain
        .get(ctx -> ctx.render("Hello World!"))
        .get(":name", ctx -> ctx.render("Hello " + ctx.getPathTokens().get("name") + "!"))     
      )
    );
  }
}
```

You can now start the application either by executing the `run` task with Gradle (i.e. `gradle run` on the command line),
or by importing the project into your IDE and executing the `my.app.Main` class.

When run, the server will be available via `http://localhost:5050/`.

The [`handlers()` method](api/ratpack/core/server/RatpackServerSpec.html#handlers%28ratpack.func.Action%29) takes a function that receives a [`Chain`](api/ratpack/core/handling/Chain.html) object.
The “Handler Chain API” is used to build the response handling strategy.

The Ratpack Gradle plugin supports [Gradle's Continuous Build feature](https://docs.gradle.org/current/userguide/continuous_build.html).
Use it to have changes to your source code be automatically applied to your running application. 

For further information on using Ratpack with Groovy, please see the [Gradle](gradle.html) chapter.

### Using the Gradle Groovy plugin

Create a `build.gradle` file with the following contents:

```language-groovy gradle
buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath "io.ratpack:ratpack-gradle:@ratpack-version@"
  }
}

apply plugin: "io.ratpack.ratpack-groovy"
apply plugin: "idea"

repositories {
  mavenCentral()
}

dependencies {
  runtimeOnly "org.slf4j:slf4j-simple:@slf4j-version@"
}
```

Create the file `src/ratpack/ratpack.groovy`, with the following content:

```language-groovy hello-world
import static ratpack.groovy.Groovy.ratpack

ratpack {
    handlers {
        get {
            render "Hello World!"
        }
        get(":name") {
            render "Hello $pathTokens.name!"
        }
    }
}
```

You can now start the application either by executing the `run` task with Gradle (i.e. `gradle run` on the command line),
or by importing the project into your IDE and executing the [`ratpack.groovy.GroovyRatpackMain`](api/ratpack/groovy/GroovyRatpackMain.html) class.

When run, the server will be available via `http://localhost:5050/`.

The [`handlers()` method](api/ratpack/groovy/Groovy.Ratpack.html#handlers%28groovy.lang.Closure%29) takes a closure that delegates to a [`GroovyChain`](api/ratpack/groovy/handling/GroovyChain.html) object.
The “Groovy Handler Chain DSL” is used to build the response handling strategy.

The Ratpack Gradle plugin supports [Gradle's Continuous Build feature](https://docs.gradle.org/current/userguide/continuous_build.html).
Use it to have changes to your source code be automatically applied to your running application. 

For further information on using Ratpack with Groovy, please see the [Groovy](groovy.html) chapter.

For further information on using Ratpack with Groovy, please see the [Gradle](gradle.html) chapter.
