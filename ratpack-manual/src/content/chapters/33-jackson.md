# Jackson

Integration with the [Jackson JSON marshalling library](https://github.com/FasterXML/jackson-databind) provides the ability to work with JSON.
This is provided as part of `ratpack-core`.
 
As of Ratpack @ratpack-version@ is built against (and depends on) Jackson Core @versions-jackson@.

The [`ratpack.jackson.Jackson`](api/ratpack/jackson/Jackson.html) class provides most of the Jackson related functionality. 
 
## Writing JSON responses

The Jackson integration adds a [Renderer](api/ratpack/render/Renderer.html) for rendering objects as JSON.

The [`Jackson.json()`](api/ratpack/jackson/Jackson.html#json-java.lang.Object-) method can be used to wrap any object (serializable by Jackson) for use with the [`Context.render()`](api/ratpack/handling/Context.html#render-java.lang.Object-) method. 

```language-java
import ratpack.test.embed.EmbeddedApp;
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
    EmbeddedApp.of(s -> s
      .handlers(chain ->
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

See the [`Jackson`](api/ratpack/jackson/Jackson.html) class documentation for more examples, including streaming and JSON events.

## Reading JSON requests

The Jackson integration adds a [Parser](api/ratpack/parse/Parser.html) for converting JSON request bodies into objects.

The [`Jackson.jsonNode()`](api/ratpack/jackson/Jackson.html#Jackson.html#jsonNode--) and [`Jackson.fromJson()`](api/ratpack/jackson/Jackson.html#fromJson) methods can be used to create objects to be used with the [`Context.parse()`](api/ratpack/handling/Context.html#parse) method. 

```language-java
import ratpack.guice.Guice;
import ratpack.test.embed.EmbeddedApp;
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
    EmbeddedApp.of(s -> s
      .handlers(chain -> chain
        .post("asNode", ctx -> {
          ctx.render(ctx.parse(jsonNode()).map(n -> n.get("name").asText()));
        })
        .post("asPerson", ctx -> {
          ctx.render(ctx.parse(fromJson(Person.class)).map(p -> p.getName()));
        })
        .post("asPersonList", ctx -> {
          ctx.render(ctx.parse(fromJson(listOf(Person.class))).map(p -> p.get(0).getName()));
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
import ratpack.test.embed.EmbeddedApp;
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
    EmbeddedApp.of(s -> s
      .handlers(chain -> chain
        .post("asPerson", ctx -> {
          ctx.parse(Person.class).then(person -> ctx.render(person.getName()));
        })
        .post("asPersonList", ctx -> {
          ctx.parse(listOf(Person.class)).then(person -> ctx.render(person.get(0).getName()));
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

## Configuring Jackson

The Jackson API is based around the [`ObjectMapper`](http://fasterxml.github.io/jackson-databind/javadoc/2.5/com/fasterxml/jackson/databind/ObjectMapper.html).
Ratpack adds a default instance to the base registry automatically.
To configure Jackson behaviour, override this instance.

Jackson [feature modules](http://wiki.fasterxml.com/JacksonFeatureModules) allow Jackson to be extended to support extra data types and capabilities.
For example the [JDK8 module](https://github.com/FasterXML/jackson-datatype-jdk8) adds support for JDK8 types like [Optional](https://docs.oracle.com/javase/8/docs/api/java/util/Optional.html).

To use such modules, simply add an appropriately configured `ObjectMapper` to the registry.

```language-java
import ratpack.test.embed.EmbeddedApp;
import ratpack.http.client.ReceivedResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    EmbeddedApp.of(s -> s
      .registryOf(r -> r
        .add(ObjectMapper.class, new ObjectMapper().registerModule(new Jdk8Module())) 
      )
      .handlers(chain ->
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
