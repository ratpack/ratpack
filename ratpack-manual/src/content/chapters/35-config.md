# Config

Most applications need some level of configuration.
This can be used to specify the correct external resources to use (databases, other services, etc.), tune performance, or otherwise adjust to the requirements of a given environment.
`ratpack-config` provides an easy, flexible mechanism to access configuration information in your Ratpack application.

Configuration data is accessed via object binding using Jackson.
Configuration objects provided by Ratpack are intended to be usable out of the box.
Configuration data can be loaded from multiple sources, such as YAML files, JSON files, properties files, environment variables and system properties.

## Quick-Start
To get started using `ratpack-config`:

1. Add a `compile` dependency on the module to your build file
1. Within your `RatpackServer` definition function, build a [`ConfigData`](api/ratpack/config/ConfigData.html) instance (see class documentation for an example)
1. Retrieve bound configuration objects from the config data

## Config Sources

[`ConfigDataSpec`](api/ratpack/config/ConfigDataSpec.html) provides methods to easily load data from the most common sources.

Commonly used file formats can be used via the [`yaml`](api/ratpack/config/ConfigDataSpec.html#yaml-java.lang.String-), [`json`](api/ratpack/config/ConfigDataSpec.html#json-java.lang.String-) and [`props`](api/ratpack/config/ConfigDataSpec.html#props-java.lang.String-) methods.
The provided signatures can be used to load data from local files (`String` or `Path`), over the network (`URL`), from the classpath (use [`Resources.getResource(String)`](http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/io/Resources.html) to get a `URL`), or anywhere else you can treat as a [`ByteSource`](http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/io/ByteSource.html).
Additionally, you can load data from non-file sources such as `Map`s/`Properties` objects (particularly useful for default values), system properties, and environment variables.
If additional flexibility is needed, you can provide your own [`ConfigSource`](api/ratpack/config/ConfigSource.html) implementation.

### Flat Config Sources
Environment variables, `Properties`, and `Map`s are flat data structures, whereas the binding model used by `ratpack-config` is hierarchical.
To bridge this gap, these config source implementations apply conventions to allow for the flat key-value pairs to be transformed into useful data.

#### Environment Variables
The default environment variable config source uses the following rules:

* Only environment variables matching a given prefix (`RATPACK_` by default) are considered; this prefix is removed before further processing
* The environment variable name is split into per-object segments using double underscore as an object boundary
* Segments are transformed into camel-case field names using a single underscore as a word boundary.

If custom environment parsing is needed, you can supply an [`EnvironmentParser`](api/ratpack/config/EnvironmentParser.html) implementation.

#### Properties/Maps
The default `Properties`/`Map` config source uses the following rules:

* If loading from system properties, only keys matching a given prefix (`ratpack.` by default) are considered; this prefix is removed before further processing
* The key is split into per-object segments using dot (".") as an object boundary
* Integer indexes between square brackets may be used to populate lists
  * This is supported both for simple values (strings) and objects (which would then have additional segments after the index)

TODO: example

## TODO

* importance of ordering
* object mapper 
* error handling
* binding
* reloading
