# Jackson

Integration with the [Jackson JSON marshalling library](https://github.com/FasterXML/jackson-databind) provides the ability to work with JSON.
 
The `ratpack-jackson` JAR is released as part of Ratpack's core distribution and is versioned with it.
As of Ratpack @ratpack-version@ is built against (and depends on) Jackson Core @versions-jackson@.

The [`ratpack.jackson.Jackson`](api/ratpack/jackson/Jackson.html) class provides most of the Jackson related functions. 
 
## Initialisation

The Jackson can be used with the [Guice integration](guice.html).
The [`JacksonModule`](api/ratpack/jackson/JacksonModule.html) is a Guice module that enables the integration.

If not using Guice, you can use the [`Jackson.Init.register()`](api/ratpack/jackson/Jackson.Init.html#register-ratpack.registry.RegistrySpec-com.fasterxml.jackson.databind.ObjectMapper-com.fasterxml.jackson.databind.ObjectWriter-)
method to add the necessary objects to the context registry.

## Writing JSON responses

The Jackson integration adds a [Renderer](api/ratpack/render/Renderer.html) for rendering objects as JSON.

The [`Jackson.json()`](api/ratpack/jackson/Jackson.html#json-java.lang.Object-) method can be used to wrap any object (serializable by Jackson) for use with the [`Context.render()`](api/ratpack/handling/Context.html#render-java.lang.Object-) method. 

```language-java
import ratpack.guice.Guice;
import ratpack.test.embed.EmbeddedApp;
import ratpack.jackson.JacksonModule;
import ratpack.http.client.ReceivedResponse;

import static ratpack.jackson.Jackson.json;
import static org.junit.Assert.*;

public class Example {

  public static class Person {
    private final String name;
    public Person(String name) {
      this.name = name;
    }
    public String getName() {
      return name;
    }
  }

  public static void main(String... args) throws Exception {
    EmbeddedApp.fromHandlerFactory(launchConfig ->
      Guice.builder(launchConfig)
        .bindings(b ->
          b.add(JacksonModule.class, c -> c.prettyPrint(false))
        )
        .build(chain ->
          chain.get(ctx -> ctx.render(json(new Person("John"))))
        )
    ).test(httpClient -> {
      ReceivedResponse response = httpClient.get();
      assertEquals("{\"name\":\"John\"}", response.getBody().getText());
      assertEquals("application/json", response.getBody().getContentType().getType());
    });
  }
}
```

See the [`Jackson`](api/ratpack/jackson/Jackson.html) class documention for more examples, including streaming and JSON events.

## Reading JSON requests

The Jackson integration adds a [Parser](api/ratpack/parse/Parser.html) for converting JSON request bodies into objects.

The [`Jackson.jsonNode()`](api/ratpack/jackson/Jackson.html#Jackson.html#jsonNode--) and [`Jackson.fromJson()`](api/ratpack/jackson/Jackson.html#fromJson) methods can be used to create objects to be used with the [`Context.parse()`](api/ratpack/handling/Context.html#parse) method. 

```language-java
import ratpack.guice.Guice;
import ratpack.test.embed.EmbeddedApp;
import ratpack.jackson.JacksonModule;
import ratpack.http.client.ReceivedResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.reflect.TypeToken;

import java.util.List;

import static ratpack.util.Types.listOf;
import static ratpack.jackson.Jackson.jsonNode;
import static ratpack.jackson.Jackson.fromJson;
import static org.junit.Assert.*;

public class Example {

  public static class Person {
    private final String name;
    public Person(@JsonProperty("name") String name) {
      this.name = name;
    }
    public String getName() {
      return name;
    }
  }

  public static void main(String... args) throws Exception {
    EmbeddedApp.fromHandlerFactory(launchConfig ->
      Guice.builder(launchConfig)
        .bindings(b ->
          b.add(JacksonModule.class, c -> c.prettyPrint(false))
        )
        .build(chain -> chain
          .post("asNode", ctx -> {
            JsonNode node = ctx.parse(jsonNode());
            ctx.render(node.get("name").asText());
          })
          .post("asPerson", ctx -> {
            Person person = ctx.parse(fromJson(Person.class));
            ctx.render(person.getName());
          })
          .post("asPersonList", ctx -> {
            List<Person> person = ctx.parse(fromJson(listOf(Person.class)));
            ctx.render(person.get(0).getName());
          })
        )
    ).test(httpClient -> {
      ReceivedResponse response = httpClient.requestSpec(s ->
        s.body(b -> b.type("application/json").text("{\"name\":\"John\"}"))
      ).post("asNode");
      assertEquals("John", response.getBody().getText());

      response = httpClient.requestSpec(s ->
        s.body(b -> b.type("application/json").text("{\"name\":\"John\"}"))
      ).post("asPerson");
      assertEquals("John", response.getBody().getText());

      response = httpClient.requestSpec(s ->
        s.body(b -> b.type("application/json").text("[{\"name\":\"John\"}]"))
      ).post("asPersonList");
      assertEquals("John", response.getBody().getText());
    });
  }
}
```

The integration adds a [no opts parser](api/ratpack/parse/NoOptParserSupport.html), which makes it possible to use the [`Context.parse(Class)`](api/ratpack/handling/Context.html#parse-java.lang.Class-) and [`Context.parse(TypeToken)`](api/ratpack/handling/Context.html#parse-com.google.common.reflect.TypeToken-) methods.

```language-java
import ratpack.guice.Guice;
import ratpack.test.embed.EmbeddedApp;
import ratpack.jackson.JacksonModule;
import ratpack.http.client.ReceivedResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.reflect.TypeToken;

import java.util.List;

import static ratpack.util.Types.listOf;
import static org.junit.Assert.*;

public class Example {

  public static class Person {
    private final String name;
    public Person(@JsonProperty("name") String name) {
      this.name = name;
    }
    public String getName() {
      return name;
    }
  }

  public static void main(String... args) throws Exception {
    EmbeddedApp.fromHandlerFactory(launchConfig ->
      Guice.builder(launchConfig)
        .bindings(b ->
          b.add(JacksonModule.class, c -> c.prettyPrint(false))
        )
        .build(chain -> chain
          .post("asPerson", ctx -> {
            Person person = ctx.parse(Person.class);
            ctx.render(person.getName());
          })
          .post("asPersonList", ctx -> {
            List<Person> person = ctx.parse(listOf(Person.class));
            ctx.render(person.get(0).getName());
          })
        )
    ).test(httpClient -> {
      ReceivedResponse response = httpClient.requestSpec(s ->
        s.body(b -> b.type("application/json").text("{\"name\":\"John\"}"))
      ).post("asPerson");
      assertEquals("John", response.getBody().getText());

      response = httpClient.requestSpec(s ->
        s.body(b -> b.type("application/json").text("[{\"name\":\"John\"}]"))
      ).post("asPersonList");
      assertEquals("John", response.getBody().getText());
    });
  }
}
```

## Using Jackson feature modules

Jackson [feature modules](http://wiki.fasterxml.com/JacksonFeatureModules) allow Jackson to be extended to support extra data types and capabilities.
For example the [JDK8 module](https://github.com/FasterXML/jackson-datatype-jdk8) adds support for JDK8 types like [Optional](https://docs.oracle.com/javase/8/docs/api/java/util/Optional.html).

If using Guice, such modules can easily be registered via the module's config.

```language-java
import ratpack.guice.Guice;
import ratpack.test.embed.EmbeddedApp;
import ratpack.jackson.JacksonModule;
import ratpack.http.client.ReceivedResponse;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import java.util.Optional;

import static ratpack.jackson.Jackson.json;
import static org.junit.Assert.*;

public class Example {

  public static class Person {
    private final String name;
    public Person(String name) {
      this.name = name;
    }
    public String getName() {
      return name;
    }
  }

  public static void main(String... args) throws Exception {
    EmbeddedApp.fromHandlerFactory(launchConfig ->
      Guice.builder(launchConfig)
        .bindings(b ->
          b.add(JacksonModule.class, c -> c
            .modules(new Jdk8Module()) // register the Jackson module
            .prettyPrint(false)
          )
        )
        .build(chain ->
          chain.get(ctx -> {
            Optional<Person> personOptional = Optional.of(new Person("John"));
            ctx.render(json(personOptional));
          })
        )
    ).test(httpClient -> {
      ReceivedResponse response = httpClient.get();
      assertEquals("{\"name\":\"John\"}", response.getBody().getText());
      assertEquals("application/json", response.getBody().getContentType().getType());
    });
  }
}
```
