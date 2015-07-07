# Context

The [`Context`](api/ratpack/handling/Context.html) type is at the core of Ratpack.

It provides:

* Access the HTTP [`Request`](api/ratpack/http/Request.html) and [`Response`](api/ratpack/http/Response.html)
* Delegation and flow control (via the [`next()`](api/ratpack/handling/Context.html#next--) and [`insert()`](api/ratpack/handling/Context.html#insert-ratpack.handling.Handler...-) methods)
* Access to _contextual objects_
* Convenience for common handler operations

For working directly with the request/response, see the [HTTP chapter](http.html).

For information about delegation, see the [Handlers chapter](handlers.html).

## Contextual objects

The context is a [registry](api/ratpack/registry/Registry.html).
It provides access to via-type-lookup of objects that were made available upstream in the handler pipeline.
This is the mechanism for inter-handler collaboration in Ratpack.

Consider the following example:

```language-java
import ratpack.test.embed.EmbeddedApp;
import ratpack.registry.Registry;

import static org.junit.Assert.assertEquals;

public class Example {

  public static interface Person {
    String getId();

    String getStatus();

    String getAge();
  }

  public static class PersonImpl implements Person {
    private final String id;
    private final String status;
    private final String age;

    public PersonImpl(String id, String status, String age) {
      this.id = id;
      this.status = status;
      this.age = age;
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public String getStatus() {
      return status;
    }

    @Override
    public String getAge() {
      return age;
    }
  }

  public static void main(String... args) throws Exception {
    EmbeddedApp
      .fromHandlers(chain -> chain
          .prefix("person/:id", (personChain) -> personChain
            .all(ctx -> {
              String id = ctx.getPathTokens().get("id"); // (1)
              Person person = new PersonImpl(id, "example-status", "example-age");
              ctx.next(Registry.single(Person.class, person)); // (2)
            })
            .get("status", ctx -> {
              Person person = ctx.get(Person.class); // (3)
              ctx.render("person " + person.getId() + " status: " + person.getStatus());
            })
            .get("age", ctx -> {
              Person person = ctx.get(Person.class); // (4)
              ctx.render("person " + person.getId() + " age: " + person.getAge());
            }))
      )
      .test(httpClient -> {
        assertEquals("person 10 status: example-status", httpClient.get("person/10/status").getBody().getText());
        assertEquals("person 6 age: example-age", httpClient.get("person/6/age").getBody().getText());
      });
  }
}
```

At `(2)` we are pushing the `Person` instance into the registry for the downstream handlers to use and at `(3)` and `(4)` how they retrieve it.
We are decoupling the creation detail from the usage, and avoiding duplicating the creation code in the `status` and `age` handlers.
The benefit of avoiding duplication is obvious.
What's slightly more subtle is that the decoupling makes testing easier when the downstream handlers are not implemented as anonymous classes (see the [Testing chapter](testing.html) for for information).

At `(1)` we are also using contextual objects.
The [`prefix()`](api/ratpack/handling/Chain.html#prefix-java.lang.String-ratpack.func.Action-) chain method binds on a request path, potentially capturing tokens.
If the binding is successful, a [`PathBinding`](api/ratpack/path/PathBinding.html) object is registered with the context that describes the binding result.
This includes any path tokens that were captured as part of the binding.
In the case above, we are capturing the second path component as the `id`.
The [`getPathTokens()`](api/ratpack/handling/Context.html#getPathTokens--) method on a context is literally shorthand for `get(PathBinding.class).getPathTokens()` on the same context.
This is another example of using the context object mechanism for inter-handler communication.

Another example of using contextual objects is the shorthand for accessing files from the file system. Consider the following script, which makes use of the context's `file` method to retrieve a static asset from the file system:

```language-groovy groovy-ratpack-dsl

import static ratpack.groovy.Groovy.ratpack

ratpack {
  handlers {
    get {
      def f = file('../')
      render f ?: "null-value"
    }
  }
}
```

In the above example, the context's [`file()`](api/ratpack/handling/Context.html#file-java.lang.String-) method is being called to retrieve a `java.io.File` instance for the provided path.
The context's `file()` method is a shorthand to retrieve the `FileSystemBinding` object from the registry, and literally is a shorthand to `get(FileSystemBinding.class).file(path/to/file)`.
The context will always resolve file assets relative to the application root, so in the case where an absolute path is provided, it should be noted that the path to the asset will be prefixed by the path in which the application exists. For example, if your application exists in `/home/ratpack/app` and your handler uses the `file` method to resolve `/etc/passwd`, then the actual path that is resolved will be `/home/ratpack/app/etc/passwd`.
In the case where a file cannot be resolved from within the application's root, the `file()` method may return a null value, which is demonstrated in the above example. The developer is responsible for handling scenarios where accessing a file may return a null object.

### Partitioning

The context object mechanism supports partitioning application logic by providing different objects to different partitions.
This is because objects registered with context are implicitly scoped, depending on how they were registered.
Objects registered with the [`next()`](api/ratpack/handling/Context.html#next-ratpack.registry.Registry-) methods are available to all downstream handlers that
were part of the same insertion (i.e. [`context.insert()`](api/ratpack/handling/Context.html#insert-ratpack.handling.Handler...-) including and nested insertions.
Objects registered with the [`insert()`](api/ratpack/handling/Context.html#insert-ratpack.registry.Registry-ratpack.handling.Handler...-) methods are available to the inserted handlers and
nested insertions.

A typical use for this is using different error handling strategies for different parts of your application.

```language-java
import ratpack.error.ServerErrorHandler;
import ratpack.test.embed.EmbeddedApp;

import static org.junit.Assert.assertEquals;

public class Example {

  public static void main(String... args) throws Exception {
    EmbeddedApp.fromHandlers(chain -> chain
        .prefix("api", api -> api
            .register(r -> r.add(ServerErrorHandler.class, (context, throwable) ->
                  context.render("api error: " + throwable.getMessage())
              )
            )
            .all(ctx -> {
              throw new Exception("in api - " + ctx.getRequest().getPath());
            })
        )
        .register(r -> r.add(ServerErrorHandler.class, (ctx, throwable) ->
              ctx.render("app error: " + throwable.getMessage())
          )
        )
        .all(ctx -> {
          throw new Exception("in app - " + ctx.getRequest().getPath());
        })
    ).test(httpClient -> {
      assertEquals("api error: in api - api/foo", httpClient.get("api/foo").getBody().getText());
      assertEquals("app error: in app - bar", httpClient.get("bar").getBody().getText());
    });
  }

}
```
