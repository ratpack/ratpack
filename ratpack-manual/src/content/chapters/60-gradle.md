# Building with Gradle

The recommended way to build Ratpack applications is to use the [Gradle Build System](http://gradle.org), by way of the Gradle plugins provided by the Ratpack project.

Ratpack is purely a runtime toolkit and not also a development time tool like [Ruby on Rails](http://rubyonrails.org) and [Grails](http://grails.org/).
This means that you can use whatever you like to build a Ratpack app.
The provided Gradle plugins merely provide convenience and are not fundamental to Ratpack development.

## Setup

The first requirement is to apply the Gradle plugin to your Gradle project…

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

repositories {
  jcenter()
}
```

Or for a Groovy based Ratpack project…

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

repositories {
  jcenter()
}
```

The `'io.ratpack.ratpack-java'` plugin applies the core Gradle [`'java'` plugin](https://docs.gradle.org/current/userguide/java_plugin.html).
The `'io.ratpack.ratpack-groovy'` plugin applies the core Gradle [`'groovy'` plugin](https://docs.gradle.org/current/userguide/groovy_plugin.html).
This means that you can start adding code and dependencies to your app like a standard Gradle based project (e.g. putting source in `src/main/[groovy|java]`).
Note that the `'io.ratpack.ratpack-groovy'` plugin implicitly applies the `'io.ratpack.ratpack-java'` plugin.

## Ratpack dependencies

To depend on a Ratpack extension library, simply add it as a regular compile dependency…

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

repositories {
  jcenter()
}

dependencies {
  compile ratpack.dependency("dropwizard-metrics")
}
```

Using `ratpack.dependency("dropwizard-metrics")` is equivalent to `"io.ratpack:ratpack-dropwizard-metrics:«version of ratpack-gradle dependency»"`.
This is the recommended way to add dependencies that are part of the core distribution.

The `'io.ratpack.ratpack-java'` plugin adds the following implicit dependencies:

* `ratpack-core` - _compile_
* `ratpack-test` - _testCompile_

The `'io.ratpack.ratpack-groovy'` plugin adds the following implicit dependencies:

* `ratpack-groovy` - _compile_ (depends on `ratpack-core`)
* `ratpack-groovy-test` - _testCompile_ (depends on `ratpack-test`)

The available libraries can be [browsed via Bintray](https://bintray.com/ratpack/maven/ratpack/view/files/io/ratpack).
All Ratpack jars are published to both [Bintray's JCenter](https://bintray.com/bintray/jcenter) and [Maven Central](http://search.maven.org).

## The 'application' plugin

Both the `'ratpack-java'` and `'ratpack-groovy'` plugins also apply the core Gradle [`'application'` plugin](https://docs.gradle.org/current/userguide/application_plugin.html).
This plugin provides the ability to create a standalone executable distribution of your software.
This is the preferred deployment format for Ratpack applications.

The `'application'` plugin requires the main class (i.e. entry point) of your application to be specified. You must configure the `'mainClassName'` attribute in your Gradle
build file to be the fully qualified class name of class that contains a `'static void main(String[] args)'` method which configures the Ratpack server.
This is preconfigured by the `'ratpack-groovy'` plugin to be the [`GroovyRatpackMain`](api/ratpack/groovy/GroovyRatpackMain.html).
This can be changed if you wish to use a custom entry point (consult the `'application'` plugin documentation).

## The 'shadow' plugin

Both the `'ratpack-java'` and `'ratpack-groovy'` plugins ship with integration support for the 3rd party [`'shadow'` plugin](https://github.com/johnrengelman/shadow).
This plugin provides the ability to create a self-contained "fat-jar" that includes your ratpack application and any compile and runtime dependencies.

The plugins react to the application of the `'shadow'` plugin and configure additional task dependencies.
They do not apply the `'shadow'` plugin and, for compatibility reasons, do not ship with a version of the `'shadow'` as a dependency.

To use the `'shadow'` integration, you will need to include the dependency in your project and apply the plugin.

```language-groovy gradle
buildscript {
  repositories {
    jcenter()
  }
  dependencies {
    classpath "io.ratpack:ratpack-gradle:@ratpack-version@"
    classpath 'com.github.jengelman.gradle.plugins:shadow:@shadow-version@'
  }
}

apply plugin: "io.ratpack.ratpack-java"
apply plugin: 'com.github.johnrengelman.shadow'

repositories {
  jcenter()
}
```

The latest version of the `'shadow'` plugin can be found on the project's [Github page]((https://github.com/johnrengelman/shadow)).

You can now have the build generate the fat-jar, by running…

```language-bash
./gradlew shadowJar
```

## The base dir

The [base dir](launching.html#base_dir) is effectively the root of the filesystem for the application.
At build time, this is effectively the main set of resources for your application (i.e. `src/main/resources`).
The Ratpack plugin adds a complimentary source of main resources, `src/ratpack`.
You can choose not to use this dir, using `src/main/resources` instead, or changing its location via the `ratpack.baseDir` property in the Gradle build.

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

repositories {
  jcenter()
}

ratpack.baseDir = file('ratpack')
```

When packaging as a distribution, the plugin will create a directory called `app` within the distribution that contains _all_ the main resources of the project.
This directory will be _prepended_ to the classpath when the app is launched via the start scripts.
This allows the application to read files from the base dir directly from disk instead of decompressing on the fly from the JAR.
This is more efficient.

See [Launching](launching.html) for more information.

### The 'ratpack.groovy' script

The `'ratpack-groovy'` plugin expects the main application definition to be located at either `ratpack.groovy` or `Ratpack.groovy` **within the base dir**.
By default, it will effectively look in `src/main/resources` and `src/ratpack`.
This file should *not* go into `src/main/groovy` as the application manages compilation of this file.
Therefore, it needs to be on the classpath in source form (i.e. as a `.groovy` file) and not in compiled form.

See [Groovy](groovy.html) for more information about the contents of this file.

## Running the application

The `'application'` plugin provides the `'run'` task for starting the Ratpack application.
This is a task of the core Gradle [`JavaExec`](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.JavaExec.html) type.
The `'ratpack-java'` plugin configures this `'run'` task to launch with the system property `'ratpack.development'` set to `true`.

If you wish to set extra system properties for development time execution, you can configure this task…

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

repositories {
  jcenter()
}

run {
  systemProperty "app.dbPassword", "secret"
}
```

### Development time reloading

The Ratpack Gradle plugins integrate with [Gradle's Continuous Build feature](https://docs.gradle.org/@gradle-version@/userguide/continuous_build.html).
To leverage this, you can run the `run` task with the `--continuous` (or `-t`) argument.

Any changes made to source or resources will be compiled and processed and the application _reloaded_ as a result.

### Running with the 'shadow' plugin

If applied to the project, the `'shadow'` plugin provides the `'runShadow'` task for starting the Ratpack application from the fat-jar.
Like the `'run'` task, this is a task of the core Gradle `JavaExec` type.
The `'shadow'` plugin configure this `'runShadow'` task to start the process using the `java -jar <path/to/shadow-jar.jar>` command.

Class reloading is not supported through the `'runShadow'` task because the application is being run from the packaged jar file.

Extra system properties or JVM options can be configured on this task…

```language-groovy gradle
buildscript {
  repositories {
    jcenter()
  }
  dependencies {
    classpath "io.ratpack:ratpack-gradle:@ratpack-version@"
    classpath "com.github.jengelman.gradle.plugins:shadow:@shadow-version@"
  }
}

apply plugin: "io.ratpack.ratpack-java"
apply plugin: "com.github.johnrengelman.shadow"

repositories {
  jcenter()
}

runShadow {
  systemProperty "app.dbPassword", "secret"
}
```
