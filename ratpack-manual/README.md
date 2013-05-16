# The Ratpack Manual

## Building the manual

The manual consists of *Markdown* templates and a stylesheet composed with *SASS* and *Compass*. It is built with Gradle.

The manual is built to `ratpack-manual/build/manual/`.

### Requirements

The only thing you need to have installed in order to build the manual is a JVM. Although the stylesheet for the manual is composed with Compass the Gradle build handles everything so you do not need Compass or even Ruby installed on your machine.

### Commands

All commands can be run from the root of the *ratpack* project.

* `./gradlew packageManual` builds the manual.
* `./gradlew openManual` builds the manual and opens it in your default browser.
* `./gradlew openApi` builds the Javadoc API and opens it in your default browser.

## Contributing

### Documentation

The documentation is in Markdown format and is found in `ratpack-manual/src/content/chapters`. The chapter number is determined by the filename.

### Templates and stylesheet

The templates for different types of page are found in `ratpack-manual/src/content/templates`. They are HTML with placeholders from the [*markdown2book*]() Gradle plugin to inject content generated from the markdown files.

The stylesheet is built from *.scss* files found in `ratpack-manual/src/sass`. File names without an underscore are compiled to an equivalently named *.css* file. File names prefixed with an underscore are imported into the main stylesheet.

The manual's stylesheet uses *Modernizr* to ensure that appropriate fallbacks are provided for older browsers.

Any 3rd party libraries or images used in the manual should be credited in the *"About this manual"* chapter.
