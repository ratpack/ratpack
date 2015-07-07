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

The `'io.ratpack.ratpack-java'` plugin applies the core Gradle [`'java'` plugin](http://www.gradle.org/docs/current/userguide/java_plugin.html).
The `'io.ratpack.ratpack-groovy'` plugin applies the core Gradle [`'groovy'` plugin](http://www.gradle.org/docs/current/userguide/groovy_plugin.html).
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
  compile ratpack.dependency("jackson")
}
```

Using `ratpack.dependency("jackson")` is equivalent to `"io.ratpack:ratpack-jackson:«version of ratpack-gradle dependency»"`.
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

Both the `'ratpack-java'` and `'ratpack-groovy'` plugins also apply the core Gradle [`'application'` plugin](http://www.gradle.org/docs/current/userguide/application_plugin.html).
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
    classpath 'com.github.jengelman.gradle.plugins:shadow:1.2.1'
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

By default, the plugin configures the base dir of your Ratpack application to be the `src/ratpack` directory in the Gradle project.
That is, these are the files that are visible to your application (e.g. static files to serve).

The base dir can by configured by setting the `ratpack.baseDir` property in the Gradle build.

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

ratpack.baseDir = file('assets/ratpack')
```

This directory will be included in the distribution built by the `'application'` plugin as the `app` directory.
This directory will be added to the classpath when starting the application, and will also be the JVM working directory.

See [Launching](launching.html) for more information.

### The 'ratpack.groovy' script

The `'ratpack-groovy'` plugin expects the main application definition to be located at either `<ratpackBaseDir>/ratpack.groovy` or `<ratpackBaseDir>/Ratpack.groovy`.
By default, it will look in `src/ratpack/ratpack.groovy` and `src/ratpack/Ratpack.groovy` respectively.
This file should *not* go in to `src/main/groovy`.

See [Groovy](groovy.html) for more information about the contents of this file.

### Generated files

Your build may generate files to be served or otherwise used at runtime.
The best approach is to have the tasks that generate these files generate into a subdirectory of the `<ratpackBaseDir>` (by default `src/ratpack`).
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

apply plugin: "io.ratpack.ratpack-java"

repositories {
  jcenter()
}

task generateDocs(type: Copy) {
  from "src/documentation"
  into "${ratpack.baseDir}/documentation"
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

The `'application'` plugin provides the `'run'` task for starting the Ratpack application.
This is a task of the core Gradle [`JavaExec`](http://www.gradle.org/docs/current/dsl/org.gradle.api.tasks.JavaExec.html) type.
The `'ratpack-java'` plugin configures this `'run'` task to start the process in `<ratpackBaseDir>` (default `src/ratpack`) and to launch with the system property `'ratpack.development'` set to `true` (which enables development time code reloading).

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
    classpath "com.github.jengelman.gradle.plugins:shadow:1.2.1"
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

## Class reloading via SpringLoaded

With a little extra configuration, you can enable reloading of changed classes at development time without restarting the server.
This is achieved by leveraging [SpringLoaded, by Pivotal](https://github.com/spring-projects/spring-loaded).
To use SpringLoaded in your Ratpack project, you need to add a dependency on the SpringLoaded agent.

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
  maven { url "https://repo.spring.io/repo" } // for springloaded
}

dependencies {
  springloaded "org.springframework:springloaded:1.2.3.RELEASE"
}
```

Reloading is now enabled for your application.
SpringLoaded will detect changed _class files_ while your application is running and patch the code in memory.

An effective workflow is to open two terminal windows.
In the first, execute…

```language-bash
./gradlew run
```

In the second, run the following after making a code change…

```language-bash
./gradlew classes
```

If you'd like to have Gradle automatically compile changes as they happen, you can use the [Gradle Watch](https://github.com/bluepapa32/gradle-watch-plugin) plugin.

Note: You do not need SpringLoaded support for reloading changes to the `<ratpackBaseDir>/Ratpack.groovy` file when using `'ratpack-groovy'`, nor do you need to have Gradle recompile the code.
The reloading of this file is handled at runtime in reloadable mode.

## IntelliJ IDEA support

The `'ratpack-java'` Gradle plugin integrates with the [core `'idea'` Gradle plugin](http://www.gradle.org/docs/current/userguide/idea_plugin.html).
A [“Run Configuration”](https://www.jetbrains.com/idea/webhelp/run-debug-configuration.html) is automatically created, making it easy to start your application from within IDEA.
The run configuration mimics the configuration of the `'run'` Gradle task, including integration with SpringLoaded.

To use the integration, you need to apply the `'idea'` plugin to your build.

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
```

You can now have the build generate metadata that allows the project to be opened in IDEA, by running…

```language-bash
./gradlew idea
```

This will generate a `«project name».ipr` file, which can be opened with IDEA.
Once the project is opened, you will see a “Run Configuration” named “Ratpack Run” that can be used to start the application.

### Reloading

If you have configured your build to use SpringLoaded, it will also be used by IDEA.
However, IDEA will not automatically recompile code while there is an active run configuration.
This means that after making a code change (to anything other than `<ratpackBaseDir>/Ratpack.groovy`) you need to click “Make Project” in the “Build” menu (or use the corresponding key shortcut).

