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

apply plugin: "ratpack"

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

apply plugin: "ratpack-groovy"

repositories {
  jcenter()
}
```

The `'ratpack'` plugin applies the core Gradle [`'java'` plugin](http://www.gradle.org/docs/current/userguide/java_plugin.html).
The `'ratpack-groovy'` plugin applies the core Gradle [`'groovy'` plugin](http://www.gradle.org/docs/current/userguide/groovy_plugin.html).
This means that you can start adding code and dependencies to your app like a standard Gradle based project (e.g. putting source in `src/main/[groovy|java]`).

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

apply plugin: "ratpack-groovy"

repositories {
  jcenter()
}

dependencies {
  compile "io.ratpack:ratpack-jackson:@ratpack-version@"
}
```

The `'ratpack'` plugin adds the following implicit dependencies:

* `ratpack-core` - _compile_
* `ratpack-test` - _testCompile_

The `'ratpack-groovy'` plugin adds the following implicit dependencies:

* `ratpack-groovy` - _compile_ (depends on `ratpack-core`)
* `ratpack-groovy-test` - _testCompile_ (depends on `ratpack-testing`)

The available libraries can be [browsed via Bintray](https://bintray.com/ratpack/maven/ratpack/view/files/io/ratpack).
All Ratpack jars are published to both [Bintray's JCenter](https://bintray.com/bintray/jcenter) and [Maven Central](http://search.maven.org).

## The 'application' plugin

Both the `'ratpack'` and `'ratpack-groovy'` plugins also apply the core Gradle [`'application'` plugin](http://www.gradle.org/docs/current/userguide/application_plugin.html).
This plugin provides the ability to create a standalone executable distribution of your software.
This is the preferred deployment format for Ratpack applications.

The `'application'` plugin requires the main class (i.e. entry point) of your application to be specified.
This is preconfigured by the `'ratpack'` and `'ratpack-groovy'` plugins to be the [`RatpackMain`](api/ratpack/launch/RatpackMain.html) and [`GroovyRatpackMain`](api/ratpack/groovy/launch/GroovyRatpackMain.html) respectively.
This can be changed if you wish to use a custom entry point (consult the `'application'` plugin documentation).

## The base dir

The `src/ratpack` directory in the Gradle project effectively becomes the base dir of your Ratpack application.
That is, these are the files that are visible to your application (e.g. static files to serve).

This directory will be included in the distribution built by the `'application'` plugin as the `app` directory.
This directory will be added to the classpath when starting the application, and will also be the JVM working directory.

### ratpack.properties and launch configuration

It is a good idea to immediately put a (potentially empty) `ratpack.properties` file in the `src/ratpack` directory.
When the application is launched, this file contributes to the application [`LaunchConfig`](api/ratpack/launch/LaunchConfig.html).

For example, to configure the maximum request size that the application will accept, add the following to the `src/ratpack/ratpack.properties` file…

```
ratpack.maxContentLength=1024
```

See [Launching](launching.html) for more information about specifying the effective `LaunchConfig` for the application.

### The 'ratpack.groovy' script

The `'ratpack-groovy'` plugin expects the main application definition to be located at either `src/ratpack/ratpack.groovy` or `src/ratpack/Ratpack.groovy`.
This file should *not* go in to `src/main/groovy`.

See [Groovy](groovy.html) for more information about the contents of this file.

### Generated files

Your build may generate files to be served or otherwise used at runtime
The best approach is to have the tasks that generate these files generate into a subdirectory of `src/ratpack`.
The Ratpack Gradle plugins add a special task named `'prepareBaseDir`' that you should make depend on your generation task.

```language-groovy gradle
buildscript {
  repositories {
    jcenter()
  }
  dependencies {
    classpath "io.ratpack:ratpack-gradle:@ratpack-version@"
  }
}

apply plugin: "ratpack"

repositories {
  jcenter()
}

task generateDocs(type: Copy) {
  from "src/documentation"
  into "src/ratpack/documentation"
  expand version: project.version
}

prepareBaseDir {
  dependsOn generateDocs
}

// Ensure that 'clean' removes the files generated by 'generateDocs'
clean {
  delete generateDocs
}
```

Making `'prepareBaseDir'` depend on your generation task ensures that it is invoked whenever the application is run or assembled.

## Running the application

TODO: discuss the 'run' task

## Class reloading via SpringLoaded

TODO: discuss spring loaded, with example of required build configuration to enable

## IntelliJ IDEA support

TODO: discuss IDEA support - run configuration, reloading, testing