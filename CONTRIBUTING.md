# Contributing to Ratpack

Contributions are always welcome to Ratpack!
If you'd like to contribute (and we hope you do) please take a look at the following guidelines and instructions.

Please see the “ready” issues on the issue board via the badge below for a list of possible tickets to work on:

[![Stories in Ready](https://badge.waffle.io/ratpack/ratpack.png?label=ready)](http://waffle.io/ratpack/ratpack)

## Building

Ratpack builds with [Gradle](http://gradle.org). 
You do not need to install Gradle to build the project, as Gradle can install itself.

To build, run this in the checkout:

    ./gradlew check

That will build the entire project and run all the tests.

### IDE import

The project is setup to work with IntelliJ IDEA.
Run the following in the checkout…

    ./gradlew idea

Then import the project into IDEA or open the `ratpack.ipr` file.

If you use a different IDE or editor you're on your own.    

## Licensing and attribution

Ratpack is licensed under [ASLv2](http://www.apache.org/licenses/LICENSE-2.0). All source code falls under this license.

The source code will not contain attribution information (e.g. Javadoc) for contributed code.
Contributions will be recognised elsewhere in the project documentation.

## Documentation

Documentation contributions are extremely valuable.
You can find the source for the Ratpack manual in the source tree in the `ratpack-manual` dir.
See the README in that directory for instructions on building and authoring the manual.

## Code changes

Code can be submitted via GitHub pull requests.

### Proposing new features

If you would like to implement a new feature, please [raise an issue](https://github.com/ratpack/ratpack/issues) before sending a pull request so the feature can be discussed.
This is to avoid you spending your valuable time working on a feature that the project developers are not willing to accept into the codebase.

### Fixing bugs

If you would like to fix a bug, please [raise an issue](https://github.com/ratpack/ratpack/issues) before sending a pull request so it can be discussed.
If the fix is trivial or non controversial then this is not usually necessary.

## Coding style guidelines

The following are some general guide lines to observe when contributing code:

1. All source files must have the appropriate ASLv2 license header
1. All source files use an indent of 2 spaces
1. All code must be written in Java, not Groovy, unless it is impossible to do so.
1. Everything needs to be tested

The build processes checks that most of the above conditions have been met.

### Public API

The public API of Ratpack is managed very carefully.
Everything that is not behind an `internal` package space is public API.

Ratpack uses [semantic versioning](http://semver.org/) which makes guarantees about API compatibility across version numbers.
In short, users should be able to upgrade their version of Ratpack at the minor level (e.g. `1.2.0` to `1.3.0`) without their code breaking.

Some rules for public API:

1. Use interfaces as much as possible, avoid classes
2. Avoid providing classes designed for subclassing (it's ok in some contexts, but try to avoid)
3. All public, private and package types and methods must include Javadoc
4. Public API cannot be changed in an incompatible way in minor or patch releases (any change needs discussion)
5. Less is more, expose as little as possible

There is no need to Javadoc internal classes.
