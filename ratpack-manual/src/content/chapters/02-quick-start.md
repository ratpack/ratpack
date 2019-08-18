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

The [`handlers()` method](api/ratpack/groovy/Groovy.Ratpack.html#handlers-groovy.lang.Closure-) takes a closure that delegates to a [`GroovyChain`](api/ratpack/groovy/handling/GroovyChain.html) object.
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
    jcenter()
  }
  dependencies {
    classpath "io.ratpack:ratpack-gradle:@ratpack-version@"
  }
}

apply plugin: "io.ratpack.ratpack-java"
apply plugin: "idea"

repositories {
  jcenter()
}

dependencies {
  runtime "org.slf4j:slf4j-simple:@slf4j-version@"
}

mainClassName = "my.app.Main"
```

Create the file `src/main/java/my/app/Main.java`, with the following content:

```language-java hello-world
package my.app;

import ratpack.server.RatpackServer;

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

The [`handlers()` method](api/ratpack/server/RatpackServerSpec.html#handlers-ratpack.func.Action-) takes a function that receives a [`Chain`](api/ratpack/handling/Chain.html) object.
The “Handler Chain API” is used to build the response handling strategy.

The Ratpack Gradle plugin supports [Gradle's Continuous Build feature](https://docs.gradle.org/current/userguide/continuous_build.html).
Use it to have changes to your source code be automatically applied to your running application. 

For further information on using Ratpack with Groovy, please see the [Gradle](gradle.html) chapter.

### Using the Gradle Groovy plugin

Create a `build.gradle` file with the following contents:

```language-groovy gradle
buildscript {
  repositories {
    jcenter()
  }
  dependencies {
    classpath "io.ratpack:ratpack-gradle:@ratpack-version@"
  }
}

apply plugin: "io.ratpack.ratpack-groovy"
apply plugin: "idea"

repositories {
  jcenter()
}

dependencies {
  runtime "org.slf4j:slf4j-simple:@slf4j-version@"
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

The [`handlers()` method](api/ratpack/groovy/Groovy.Ratpack.html#handlers-groovy.lang.Closure-) takes a closure that delegates to a [`GroovyChain`](api/ratpack/groovy/handling/GroovyChain.html) object.
The “Groovy Handler Chain DSL” is used to build the response handling strategy.

The Ratpack Gradle plugin supports [Gradle's Continuous Build feature](https://docs.gradle.org/current/userguide/continuous_build.html).
Use it to have changes to your source code be automatically applied to your running application. 

For further information on using Ratpack with Groovy, please see the [Groovy](groovy.html) chapter.

For further information on using Ratpack with Groovy, please see the [Gradle](gradle.html) chapter.

## Using Lazybones project templates

[Lazybones](https://github.com/pledbrook/lazybones) is a command line tool that allows you to generate a project structure for any framework based on pre-defined templates.

Ratpack's Lazybones templates can be found on [Bintray](https://bintray.com) in the [ratpack/lazybones repository](https://bintray.com/ratpack/lazybones).
Templates are published with each Ratpack release and template versions are aligned with Ratpack release versions.

See the [Lazybones documentation](https://github.com/pledbrook/lazybones#running-it) for help with installing Lazybones.

Lazybones commands are in the format...

```language-bash
lazybones create <ratpack template> <ratpack version> <app name>
```

With Lazybones installed, creating a new Ratpack application is as easy as…

```language-bash
lazybones create ratpack my-ratpack-app
cd my-ratpack-app
./gradlew run
```

This will use the latest available version of Ratpack.
If a specific version is required…

```language-bash
lazybones create ratpack x.x.x my-ratpack-app
cd my-ratpack-app
./gradlew run
```

Where `x.x.x` is a valid template version.
