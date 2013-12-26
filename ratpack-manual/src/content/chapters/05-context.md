# Context

Handlers operate on a [`Context`](api/ratpack/handling/Context.html).

It provides:

* Access the HTTP {@link #getRequest() request} and {@link #getResponse() response}
* Delegation (via the [`next()`](api/ratpack/handling/Context.html#next\(\)) and [`next()`](api/ratpack/handling/Context.html#insert\(ratpack.handling.Handler...\)) family of methods)
* Access to _contextual objects_
* Convenience for common handler operations

For working directly with the request/response, see the [HTTP chapter](http.html).

For information about delegation, see the [Handlers chapter](handlers.html).

## Contextual objects

The context is a [registry](api/ratpack/registry/Registry.html).
It provides access to via-type-lookup of objects that were made available upstream in the handler pipeline.
This is the mechanism for inter-handler collaboration in Ratpack.

Consider the following example:

```language-groovy tested
import ratpack.handling.Handler;
import ratpack.handling.Context;
import ratpack.handling.Chain;
import ratpack.util.Action;
import ratpack.launch.HandlerFactory;
import ratpack.launch.LaunchConfig;

import static ratpack.handling.Handlers.chain;

// The api of some business object
interface Person {}

// Implementation
public class PersonImpl implements Person {
  private final String id;
  public PersonImpl(String id) {
    this.id = id;
  };
}

public class Application implements HandlerFactory {
  public Handler create(LaunchConfig launchConfig) {
    return chain(launchConfig, new Action<Chain>() {
      void execute(Chain chain) {
        chain.
          prefix("person/:id", new Action<Chain>() {
            void execute(Chain personChain) {
              personChain.
                handler(new Handler() {
                  public void handle(Context context) {
                    String id = context.getPathTokens().get("id"); // (1)
                    Person person = new PersonImpl(id);
                    context.next(Person.class, person); // (2)
                  }
                }).
                get("status", new Handler() {
                  public void handle(Context context) {
                    Person person = context.get(Person.class); // (3)
                    // show the person's status
                  }
                }).
                get("age", new Handler() {
                  public void handle(Context context) {
                    Person person = context.get(Person.class); // (4)
                    // show the person's age
                  }
                });
            }
          });
      }
    });
  }
}
```

At `(2)` we are pushing the `Person` instance into the registry for the downstream handlers to use and at `(3)` and `(4)` how they retrieve it.
We are decoupling the creation detail from the usage, and avoiding duplicating the creation code in the `status` and `age` handlers.
The benefit of avoiding duplication is obvious.
What's slightly more subtle is that the decoupling makes testing easier when the downstream handlers are not implemented as anonymous classes (see the [Testing chapter](testing.html) for for information).

At `(1)` we are also using contextual objects.
The [`prefix()`](api/ratpack/handling/Chain.html#prefix\(java.lang.String, ratpack.util.Action\)) chain method binds on a request path, potentially capturing tokens.
If the binding is successful, a [`PathBinding`](api/ratpack/path/PathBinding.html) object is registered with the context that describes the binding result.
This includes any path tokens that were captured as part of the binding.
In the case above, we are capturing the second path component as the `id`.
The [`getPathTokens()`](api/ratpack/handling/Context.html#getPathTokens\(\)) method on a context is literally shorthand for `get(PathBinding.class).getPathTokens()` on the same context.
This is another example of using the context object mechanism for inter-handler communication.

Another example of using contextual objects is the shorthand for accessing files from the file system. Consider the following script, which makes use of the context's `file` method to retrieve a static asset from the file system:

```language-groovy tested

import static ratpack.groovy.Groovy.ratpack

ratpack {
  handlers {
    get {
      def f = file('../ratpack.groovy')
      if (!f)
        render "null value"
      else
        render "non-null value"
    }
  }
}
```

In the above example, the context's [`file()`](api/ratpack/handling/Context.html#file\(\)) method is being called to retrieve a `java.io.File` instance for the provided path.
The context's `file()` method is a shorthand to retrieve the `FileSystemBinding` object from the registry, and literally is a shorthand to `get(FileSystemBinding.class).file(path/to/file)`.
The context will always resolve file assets relative to the application root, so in the case where an absolute path is provided, it should be noted that the path to the asset will be prefixed by the path in which the application exists. For example, if your application exists in `/home/ratpack/app` and your handler uses the `file` method to resolve `/etc/passwd`, then the actual path that is resolved will be `/home/ratpack/app/etc/passwd`.
In the case where a file cannot be resolved within the context of the application root, the `file()` method may return a null value, which is demonstrated in the above example. This is generally transparent to developers who are simply streaming a file's contents, but in the case where you need to access metadata for a file, you should explicitly handle the null-value scenario.

### Partitioning

The context object mechanism supports partitioning application logic by providing different objects to different partitions.
This is because objects registered with context are implicitly scoped, depending on how they were registered.
Objects regis tered with the [`next()`](api/ratpack/handling/Context.html#next(java.lang.Class,%20T)) methods are available to all downstream handlers that
were part of the same insertion (i.e. [`context.insert()`](api/ratpack/handling/Context.html#insert\(ratpack.handling.Handler...\)) including and nested insertions.
Objects registered with the [`insert()`](api/ratpack/handling/Context.html#insert\(java.lang.Class,%20T,%20ratpack.handling.Handler...\)) methods are available to the inserted handlers and
nested insertions.

A typical use for this is using different error handling strategies for different parts of your application.

```language-groovy tested
import ratpack.handling.Handler;
import ratpack.handling.Context;
import ratpack.handling.Chain;
import ratpack.util.Action;
import ratpack.launch.HandlerFactory;
import ratpack.launch.LaunchConfig;
import ratpack.error.ServerErrorHandler;

import static ratpack.handling.Handlers.chain;

public class ApiHandlers implements Action<Chain> {
  public void execute(Chain chain) {
    // add api handlers
  }
}

public class ApiServerErrorHandler implements ServerErrorHandler {
  public void error(Context context, Exception exception) {
    // return error data object
  }
}

public class AppHandlers implements Action<Chain> {
  public void execute(Chain chain) {
    // add normal app handlers
  }
}

public class AppServerErrorHandler implements ServerErrorHandler {
  public void error(Context context, Exception exception) {
    // display pretty error page
  }
}

public class Application implements HandlerFactory {
  public Handler create(final LaunchConfig launchConfig) {
    return chain(launchConfig, new Action<Chain>() {
      void execute(Chain chain) {
        chain.
          prefix("api", chain.chain(new Action<Chain>() {
            void execute(Chain apiChain) {
              apiChain.
                register(ServerErrorHandler.class, new ApiServerErrorHandler(), new ApiHandlers());
            }
          })).
          register(ServerErrorHandler.class, new AppServerErrorHandler(), new AppHandlers());
      }
    });
  }
}
```
