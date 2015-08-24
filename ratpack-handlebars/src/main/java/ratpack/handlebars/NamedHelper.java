/*
 * Copyright 2013 the original author or authors.
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

package ratpack.handlebars;

import com.github.jknack.handlebars.Helper;

/**
 * Implementations of this interface bound with Guice will be automatically registered as handlebars helpers.
 *
 * <pre class="java">{@code
 * import com.github.jknack.handlebars.Options;
 * import ratpack.guice.Guice;
 * import ratpack.handlebars.HandlebarsModule;
 * import ratpack.handlebars.NamedHelper;
 * import ratpack.test.embed.EphemeralBaseDir;
 * import ratpack.test.embed.EmbeddedApp;
 *
 * import java.io.IOException;
 * import java.nio.file.Path;
 *
 * import static ratpack.handlebars.Template.handlebarsTemplate;
 * import static org.junit.Assert.*;
 *
 * public class Example {
 *
 *   public static class HelloHelper implements NamedHelper<String> {
 *     public String getName() {
 *       return "hello";
 *     }
 *
 *     public CharSequence apply(String context, Options options) throws IOException {
 *       return "Hello " + context.toUpperCase() + "!";
 *     }
 *   }
 *
 *   public static void main(String... args) throws Exception {
 *     EphemeralBaseDir.tmpDir().use(baseDir -> {
 *       baseDir.write("handlebars/myTemplate.html.hbs", "{{hello \"ratpack\"}}");
 *       EmbeddedApp.of(s -> s
 *         .serverConfig(c -> c.baseDir(baseDir.getRoot()))
 *         .registry(Guice.registry(b -> b
 *           .module(new HandlebarsModule())
 *           .bind(HelloHelper.class)
 *         ))
 *         .handlers(chain -> chain
 *           .get(ctx -> ctx.render(handlebarsTemplate("myTemplate.html")))
 *         )
 *       ).test(httpClient -> {
 *         assertEquals("Hello RATPACK!", httpClient.getText());
 *       });
 *     });
 *   }
 * }
 * }</pre>
 */
public interface NamedHelper<T> extends Helper<T> {
  String getName();
}
