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
Additionally, you can load data from non-file sources such as `Map`s/`Properties` objects (particularly useful for default values; see [example](api/ratpack/config/ConfigDataSpec.html#props-java.util.Map-)), system properties, and environment variables.
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

## Usage

### Ordering
If you have multiple config sources, add them to the builder from least important to most important.
For example, if you had a configuration file that you wanted to be able to override via system properties, you would first add the configuration file source, followed by the system properties source.
Likewise, if you have default settings that you wanted to be able to override via environment variables, you would first add the default settings source (perhaps via [`props`](api/ratpack/config/ConfigDataSpec.html#props-java.util.Map-)), followed by the environment variables source.

### Error Handling
As shown in the [ConfigDataSpec docs](api/ratpack/config/ConfigDataSpec.html), [`onError`](api/ratpack/config/ConfigDataSpec.html#onError-ratpack.func.Action-) can be used to customize the behavior when an error is encountered while loading data from a config source.
Most commonly, this is used to make configuration sources optional by ignoring load exceptions.

### Reloading
Reloading based on configuration data changes is enabled if the config data object is added to the server registry (see example in [ConfigData docs](api/ratpack/config/ConfigData.html)) and the server is in [development mode](api/ratpack/server/ServerConfig.html#isDevelopment--).
When enabled, the configuration data sources are polled periodically.
If any changes are detected, it triggers a server reload.
For this to work properly, all construction of the `ConfigData` object should take place within the [RatpackServer](api/ratpack/server/RatpackServer.html) `of`/`start` block.

### Binding
TODO

### Object Mapper
TODO
