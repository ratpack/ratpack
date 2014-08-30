/*
 * Copyright 2014 the original author or authors.
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

package ratpack.http.stream;

import ratpack.render.Renderer;

/**
 * A renderer for streaming HTTP chunks for HTTP chunked transfer-encoding.
 * <p>
 * An implementation of this is <b>always</b> provided by Ratpack core.
 * <p>
 * Example usage:
 * <pre class="java">
 * import ratpack.handling.Handler;
 * import ratpack.handling.Context;
 * import ratpack.func.Function;
 * import ratpack.stream.Streams;
 * import ratpack.launch.HandlerFactory;
 * import ratpack.launch.LaunchConfig;
 * import ratpack.launch.LaunchConfigBuilder;
 * import ratpack.test.embed.EmbeddedApplication;
 * import ratpack.test.embed.LaunchConfigEmbeddedApplication;
 * import ratpack.test.http.TestHttpClient;
 * import ratpack.test.http.TestHttpClients;
 * import static ratpack.http.stream.HttpResponseChunks.stringChunks;
 *
 * import java.util.concurrent.TimeUnit;
 * import java.util.concurrent.ScheduledExecutorService;
 * import org.reactivestreams.Publisher;
 *
 * public class Example {
 *
 *   private static EmbeddedApplication createApp() {
 *     return new LaunchConfigEmbeddedApplication() {
 *       protected LaunchConfig createLaunchConfig() {
 *         return LaunchConfigBuilder.noBaseDir().port(0).build(new HandlerFactory() {
 *             public Handler create(LaunchConfig launchConfig) {
 *
 *               // Example of streaming chunks
 *
 *               return new Handler() {
 *                 public void handle(Context context) {
 *                   // simulate streaming by periodically publishing
 *                   ScheduledExecutorService executor = context.getLaunchConfig().getExecController().getExecutor();
 *                   Publisher&lt;String&gt; strings = Streams.periodically(executor, 5, TimeUnit.MILLISECONDS, new Function&lt;Integer, String&gt;() {
 *                     public String apply(Integer i) {
 *                       if (i.intValue() < 5) {
 *                         return i.toString();
 *                       } else {
 *                         return null;
 *                       }
 *                     }
 *                   });
 *
 *                   context.render(stringChunks(strings));
 *                 }
 *               };
 *
 *             }
 *           });
 *       }
 *     };
 *   }
 *
 *   public static void main(String[] args) {
 *     try(EmbeddedApplication app = createApp()) {
 *       assert app.getHttpClient().getText().equals("01234");
 *     }
 *   }
 *
 * }
 * </pre>
 *
 * @see HttpResponseChunks
 * @see <a href="http://en.wikipedia.org/wiki/Chunked_transfer_encoding" target="_blank">Wikipedia - Chunked transfer encoding</a>
 */
public interface HttpResponseChunksRenderer extends Renderer<HttpResponseChunks> {
}
