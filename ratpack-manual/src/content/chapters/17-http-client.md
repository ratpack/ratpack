# Http client

Ratpack provides its own [`HttpClient`](api/ratpack/http/client/HttpClient.html) which can be used to make remote HTTP calls.
The Ratpack provided `HttpClient` is fully non-blocking and is a part of the core Ratpack library.
Just like the Ratpack server, the `HttpClient` also uses Netty under the hood and in fact shares the same [`EventLoopGroup`](http://netty.io/4.1/api/io/netty/channel/EventLoopGroup.html) as per Netty best practices.


## Basic GET request


```language-java
import ratpack.http.client.HttpClient;
import ratpack.test.embed.EmbeddedApp;

import static org.junit.Assert.assertEquals;

public class Example {
  public static void main(String... args) throws Exception {
    try (EmbeddedApp remoteApp = EmbeddedApp.fromHandler(ctx -> ctx.render("Hello from remoteApp"))) {
      EmbeddedApp.fromHandler(ctx -> ctx
          .render(
            ctx
              .get(HttpClient.class)
              .get(remoteApp.getAddress())
              .map(response -> response.getBody().getText())
          )
      ).test(httpClient -> 
        assertEquals("Hello from remoteApp", httpClient.getText())
      );
    }
  }
}
```
