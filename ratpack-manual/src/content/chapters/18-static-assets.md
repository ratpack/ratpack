# Static Assets

Ratpack provides support for serving static files as responses.

## From a directory 

Ratpack applications have a notion of a [“base dir”](launching.html#base_dir), which is specified at launch time.
This is effectively root of the filesystem as far as the application is concerned.
Files from the base dir can be served, using the [`Chain.files()`](api/ratpack/handling/Chain.html#files-ratpack.func.Action-) method.

```language-java
import ratpack.test.embed.EmbeddedApp;
import ratpack.test.embed.EphemeralBaseDir;

import static org.junit.Assert.assertEquals;

public class Example {
  public static void main(String... args) throws Exception {
    EphemeralBaseDir.tmpDir().use(baseDir -> {
      baseDir.write("public/some.text", "foo");
      baseDir.write("public/index.html", "bar");

      EmbeddedApp.of(s -> s
        .serverConfig(c -> c.baseDir(baseDir.getRoot()))
        .handlers(c -> c
          .files(f -> f.dir("public").indexFiles("index.html"))
        )
      ).test(httpClient -> {
        assertEquals("foo", httpClient.getText("some.text"));
        assertEquals("bar", httpClient.getText());
        assertEquals(404, httpClient.get("no-file-here").getStatusCode());
      });

    });
  }
}
```

Files will be served with a `Last-Modified` header based on the file's advertised last modified timestamp.
If the client sends an `If-Modified-Since` header, Ratpack will respond with a `304` response if the file has not been modified since the given value.
Served files do not include ETags.

By default, files will be GZIP compressed over the wire if the client asks for it.
This can be disabled on a per request basis by calling the [`Response.noCompress()`](api/ratpack/http/Response.html#noCompress--) method.
This is typically used by putting a handler in front of the file serving handler that inspects the request path (e.g. file extension) and disables compression.

## Ad-hoc files

Individual files can be served by using the [`Context.file()`](api/ratpack/handling/Context.html#file-java.lang.String-) and [`Context.render()`](api/ratpack/handling/Context.html#render-java.lang.Object-) methods.

```language-java
import ratpack.test.embed.EmbeddedApp;
import ratpack.test.embed.EphemeralBaseDir;

import static org.junit.Assert.assertEquals;

public class Example {
  public static void main(String... args) throws Exception {
    EphemeralBaseDir.tmpDir().use(baseDir -> {
      baseDir.write("some.text", "foo");

      EmbeddedApp.of(s -> s
        .serverConfig(c -> c.baseDir(baseDir.getRoot()))
        .handlers(c -> c
          .get("f", ctx -> ctx.render(ctx.file("some.text")))
        )
      ).test(httpClient ->
        assertEquals("foo", httpClient.getText("f"))
      );

    });
  }
}
```

If the file returned by `Context.file()` doesn't exist, a `404` will be issued.

Responses are timestamped and compressed in exactly the same manner as described for the [`Chain.files()`](api/ratpack/handling/Chain.html#files-ratpack.func.Action-) method. 

## Advanced Asset serving with “Asset Pipeline”

The [`Asset Pipeline`](https://github.com/bertramdev/asset-pipeline/tree/master/ratpack-asset-pipeline) project offers an integration with Ratpack.
This provides advanced asset bundling, compiling and serving.
