# Setup

Ratpack is purely a runtime.
There is no installable package.
To build Ratpack applications, you can use any JVM build tool.
The Ratpack project provides specific support for [Gradle](http://www.gradle.org) through plugins, but any could be used.

Ratpack is published as a set of library JARs.
The `ratpack-core` library is the only strictly required library.
Others such as `ratpack-groovy`, `ratpack-guice`, `ratpack-jackson`, `ratpack-test` etc. are optional.

With Ratpack on the classpath, you can use the API described in the next chapter to launch the application.

## Using the Gradle plugin(s)

First, [install Gradle](http://www.gradle.org/docs/current/userguide/installation.html) if you haven't already.
On Mac OS X, if you have [Homebrew](http://brew.sh/) installed, you can simply run `brew install gradle`.

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
```

Create directories `src/ratpack` and `src/main/java`.

If desired, run `gradle idea` to generate project files for IntelliJ and open the project.

Create a `src/ratpack/ratpack.properties` file with the following contents:

```
handlerFactory=AppHandlerFactory
```

Create a `src/main/java/AppHandlerFactory.java` with the following contents:

```language-java
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.launch.HandlerFactory;
import ratpack.launch.LaunchConfig;

import static ratpack.handling.Handlers.*;

public class AppHandlerFactory implements HandlerFactory {
  @Override
  public Handler create(LaunchConfig launchConfig) throws Exception {
    return chain(
      path("foo", new Handler() {
          @Override
          public void handle(Context context) {
              context.render("from the foo handler");
          }
      }),
      path("bar", new Handler() {
        @Override
        public void handle(Context context) throws Exception {
          context.render("from the bar handler");
        }
      })
    );
  }
}
```

Run the project by running `gradle run`, or create a distribution archive by running `gradle distZip`.

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
