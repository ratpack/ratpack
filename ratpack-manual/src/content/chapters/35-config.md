# Config

Most applications need some level of configuration.
This can be used to specify the correct external resources to use (databases, other services, etc.), tune performance, or otherwise adjust to the requirements of a given environment.
Ratpack provides an easy, flexible mechanism to access configuration information in your Ratpack application.

Configuration data is accessed via object binding using Jackson.
Configuration objects provided by Ratpack are intended to be usable out of the box.
Configuration data can be loaded from multiple sources, such as YAML files, JSON files, properties files, environment variables and system properties.

## Quick-Start

To get started:

1. Within your `RatpackServer` definition function, build a [`ConfigData`](api/ratpack/config/ConfigData.html) instance (see class documentation for an example)
1. Retrieve bound configuration objects from the config data

## Config Sources

[`ConfigDataBuilder`](api/ratpack/config/ConfigDataBuilder.html) provides methods to easily load data from the most common sources.

Commonly used file formats can be used via the [`yaml`](api/ratpack/config/ConfigDataBuilder.html#yaml-java.lang.String-), [`json`](api/ratpack/config/ConfigDataBuilder.html#json-java.lang.String-) and [`props`](api/ratpack/config/ConfigDataBuilder.html#props-java.lang.String-) methods.
The provided signatures can be used to load data from local files (`String` or `Path`), over the network (`URL`), from the classpath (use [`Resources.getResource(String)`](http://google.github.io/guava/releases/18.0/api/docs/com/google/common/io/Resources.html) to get a `URL`), or anywhere else you can treat as a [`ByteSource`](http://google.github.io/guava/releases/18.0/api/docs/com/google/common/io/ByteSource.html).
Additionally, you can load data from non-file sources such as `Map`s/`Properties` objects (particularly useful for default values; see [example](api/ratpack/config/ConfigDataBuilder.html#props-java.util.Map-)), system properties, and environment variables.
You can also choose to load configuration data from existing objects using the [`object`](api/ratpack/config/ConfigDataBuilder.html#object-java.lang.String-java.lang.Object-) method.
If additional flexibility is needed, you can provide your own [`ConfigSource`](api/ratpack/config/ConfigSource.html) implementation.

### Flat Config Sources

Environment variables, `Properties`, and `Map`s are flat data structures, whereas the binding model used by Ratpack is hierarchical.
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
  * This syntax is not supported for environment variables. 

## Usage

### Ordering

If you have multiple config sources, add them to the builder from least important to most important.
For example, if you had a configuration file that you wanted to be able to override via system properties, you would first add the configuration file source, followed by the system properties source.
Likewise, if you have default settings that you wanted to be able to override via environment variables, you would first add the default settings source (perhaps via [`props`](api/ratpack/config/ConfigDataBuilder.html#props-java.util.Map-)), followed by the environment variables source.

### Error Handling

As shown in the [ConfigDataBuilder docs](api/ratpack/config/ConfigDataBuilder.html), [`onError`](api/ratpack/config/ConfigDataBuilder.html#onError-ratpack.func.Action-) can be used to customize the behavior when an error is encountered while loading data from a config source.
Most commonly, this is used to make configuration sources optional by ignoring load exceptions.

### Object Mapper

Ratpack uses Jackson for config object binding.
The default `ObjectMapper` used is configured with commonly used Jackson modules pre-loaded, and set to allow unquoted field names, allow single quotes, and ignore unknown field names.
This is intended to make it easy to use, out-of-the-box.
However, there will sometimes be cases where you may want to change a Jackson configuration setting or add additional Jackson modules.
If so, this can be accomplished via various signatures of `ConfigData.of(...)` or via `ConfigDataBuilder.configureObjectMapper(...)`.

### Binding

Once you've built your [`ConfigData`](api/ratpack/config/ConfigData.html) instance, you can bind the data to configuration objects.
The simplest option is to define a class that represents the entirety of your application's configuration, and bind to it all at once using [`ConfigData.get(Class)`](api/ratpack/config/ConfigData.html#get-java.lang.Class-).
Alternatively, you can bind objects one-at-a-time at specified paths within the data using [`ConfigData.get(String, Class)`](api/ratpack/config/ConfigData.html#get-java.lang.String-java.lang.Class-).
For the common case of binding to a [`ServerConfig`](api/ratpack/server/ServerConfig.html) object, `ConfigData.getServerConfig(...)` signatures are provided as a convenience.
