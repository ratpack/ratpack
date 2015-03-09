# Quick Start

This chapter provides instructions on how to get a Ratpack application up and running to play with.

## Using the Gradle plugin(s)

We recommend the use of the [Gradle build system](http:///www.gradle.org) to build Ratpack applications.
Ratpack does not require Gradle; any build system can be used.

> The following instructions assume you have already installed Gradle.
> See the [Gradle User Guide](http://www.gradle.org/docs/current/userguide/installation.html) for installation instructions.

The Ratpack project provides two Gradle plugins:

1. [io.ratpack.ratpack-java](http://plugins.gradle.org/plugin/io.ratpack.ratpack-java) - for Ratpack applications implemented in Java
2. [io.ratpack.ratpack-groovy](http://plugins.gradle.org/plugin/io.ratpack.ratpack-groovy)  - for Ratpack applications implemented in [Groovy](http://groovy-lang.org)
 
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
apply plugin: "org.gradle.idea"

repositories {
  jcenter()
}

dependencies {
  runtime "org.slf4j:slf4j-simple:@slf4j-version@"
}

mainClassName = "my.app.Main"
```

Create the file `src/main/java/my/app/Main.java`, with the following content:

```language-java main
package my.app;

import ratpack.server.RatpackServer;

public class Main {
  public static void main(String... args) throws Exception {
    RatpackServer.start(server -> server 
      .handlers(chain -> chain
        .get(ctx -> ctx.render("Hello World!"))
        .get(":name", ctx -> ctx.render("Hello " + ctx.getPathTokens().get("name")))     
      )
    );
  }
}
```

You can now start the application either by executing the `run` task with Gradle (i.e. `gradle run` on the command line),
or by importing the project into your IDE and executing the `my.app.Main` class.

For further information on using Ratpack with Gradle, please the [Gradle chapter](gradle.html).

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

Create directories `src/ratpack` and `src/main/groovy`.

If desired, run `gradle idea` to generate project files for IntelliJ and open the project.

Create a `src/ratpack/ratpack.groovy` file with the following contents:

```language-groovy
import static ratpack.groovy.Groovy.ratpack

ratpack {
    handlers {
        get("foo") {
            render "from the foo handler"
        }
        get("bar") {
            render "from the bar handler"
        }
    }
}
```

Run the project by running `gradle run`, or create a distribution archive by running `gradle distZip`.

For further information on using Ratpack with Gradle and Groovy, please the [Gradle](gradle.html) and [Groovy](groovy.html) chapters.

## Using Lazybones project templates

[Lazybones](https://github.com/pledbrook/lazybones) is a command line tool that allows you to generate a project structure for any framework based on pre-defined templates.

Ratpack's Lazybones templates can be found on [Bintray](https://bintray.com) in the [ratpack/lazybones repository](https://bintray.com/ratpack/lazybones).
Templates are published with each Ratpack release and template versions are aligned with Ratpack release versions.
There are different types of Ratpack templates available, listed in the repository.
See the description of each for details.

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

Where `x.x.x` is a valid template version.  See the [Bintray template repository](https://bintray.com/ratpack/lazybones/ratpack-template/view) for all available template versions.
