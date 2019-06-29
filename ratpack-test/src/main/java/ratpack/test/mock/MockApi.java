/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.test.mock;

import ratpack.server.RatpackServer;
import ratpack.test.embed.EmbeddedApp;
import ratpack.test.handling.HandlerFactory;
import ratpack.util.Exceptions;

/**
 * A test harness for simulating behavior of remote APIs by starting an {@link EmbeddedApp} that will
 * handle requests based on the content of the received request.
 * <p>
 * {@link MockApi} provides functionality similar to <a href="http://wiremock.org/" target="_blank">WireMock</a> and
 * <a href="https://betamax.readthedocs.io/en/latest/index.html" target="_blank">BetaMax</a>.
 * <pre class="java">{@code
 * import ratpack.http.HttpMethod;
 * import ratpack.http.client.HttpClient;
 * import ratpack.test.embed.EmbeddedApp;
 * import ratpack.test.handling.HandlerFactory;
 * import ratpack.test.mock.MockApi;
 *
 * import static org.junit.Assert.assertEquals;
 *
 * public class Example {
 *   public static void main(String... args) throws Exception {
 *
 *     HandlerFactory factory = request -> {
 *       if (request.getMethod() ==  HttpMethod.GET)  {
 *         return ctx -> ctx.render("get on remote API");
 *       }
 *       return ctx -> ctx.getResponse().status(400).send();
 *     };
 *
 *     MockApi remoteApi = MockApi.of(factory);
 *
 *     EmbeddedApp.fromHandlers(chain -> {
 *       chain.get("get", ctx ->
 *         ctx.get(HttpClient.class)
 *           .get(remoteApi.getAddress())
 *           .then(resp ->
 *             resp.forwardTo(ctx.getResponse())
 *           )
 *       );
 *       chain.get("post", ctx ->
 *         ctx.get(HttpClient.class)
 *           .post(remoteApi.getAddress(), spec -> {})
 *           .then(resp ->
 *             resp.forwardTo(ctx.getResponse())
 *           )
 *       );
 *     }).test(httpClient -> {
 *       assertEquals("get on remote API", httpClient.get("get").getBody().getText());
 *       assertEquals(400, httpClient.get("post").getStatusCode());
 *     });
 *   }
 * }
 * }</pre>
 * <p>
 * {@link MockApi} is particularly powerful when combined with
 * Spock's {@code Mock} API by providing a Mock {@link HandlerFactory}
 * to this class. Interactions to a remote API can then be
 * validated inline to the {@link spock.lang.Specification} by
 * verifying the invocations of the mock {@code HandlerFactory}.
 * <pre class="groovy">{@code
 * import ratpack.groovy.test.embed.GroovyEmbeddedApp
 * import ratpack.http.HttpMethod
 * import ratpack.http.client.HttpClient
 * import spock.lang.Specification
 *
 * import static ratpack.groovy.Groovy.groovyHandler
 *
 * class ApiSpec extends Specification {
 *
 *   def "test api"() {
 *     given:
 *     MockApi remoteApi = MockApi.of(Mock(HandlerFactory))
 *
 *     def app = GroovyEmbeddedApp.ratpack {
 *       handlers {
 *         get { ctx ->
 *           ctx.get(HttpClient).get(remoteApi.address).then { resp ->
 *             resp.forwardTo(ctx.response)
 *           }
 *         }
 *       }
 *     }
 *
 *     when:
 *     def resp = app.httpClient.get()
 *
 *     then:
 *     1 * remoteApi.handlerFactory.receive({
 *       it.method == HttMethod.GET
 *       it.path == ""
 *     } >> groovyHandler {
 *       render("remote ok")
 *     }
 *     resp.body.text == "remote ok"
 *   }
 * }
 * }</pre>
 * @since 1.7.0
 */
public class MockApi implements EmbeddedApp {

  private final EmbeddedApp app;
  private final HandlerFactory factory;

  private MockApi(HandlerFactory factory) {
    this.factory = factory;
    this.app = Exceptions.uncheck(() -> EmbeddedApp.of(spec ->
      spec.handlers(c ->
        c.all(ctx ->
          this.factory.receive(ctx.getRequest()).handle(ctx)
        )
      )
    ));
  }

  @Override
  public RatpackServer getServer() {
    return app.getServer();
  }

  public HandlerFactory getHandlerFactory() {
    return this.factory;
  }

  public static MockApi of(HandlerFactory factory) {
    return new MockApi(factory);
  }

}
