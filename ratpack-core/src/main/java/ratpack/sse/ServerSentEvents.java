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

package ratpack.sse;

import org.reactivestreams.Publisher;

/**
 * A {@link ratpack.handling.Context#render(Object) renderable} object for streaming server side events.
 * <p>
 * A {@link ratpack.render.Renderer renderer} for this type is implicitly provided by Ratpack core.
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
 * import ratpack.http.client.ReceivedResponse;
 * import ratpack.sse.ServerSentEvent;
 * import ratpack.test.embed.EmbeddedApplication;
 * import ratpack.test.embed.LaunchConfigEmbeddedApplication;
 * import ratpack.test.http.TestHttpClient;
 * import ratpack.test.http.TestHttpClients;
 *
 * import static ratpack.sse.ServerSentEvents.serverSentEvents;
 *
 * import java.util.List;
 * import java.util.concurrent.TimeUnit;
 * import java.util.concurrent.ScheduledExecutorService;
 * import org.reactivestreams.Publisher;
 *
 * import com.google.common.collect.Lists;
 * import com.google.common.base.Joiner;
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
 *                   Publisher&lt;ServerSentEvent&gt; eventStream = Streams.periodically(executor, 5, TimeUnit.MILLISECONDS, new Function&lt;Integer, ServerSentEvent&gt;() {
 *                     public ServerSentEvent apply(Integer i) {
 *                       if (i.intValue() &lt; 5) {
 *                         return new ServerSentEvent(i.toString(), "counter", "event " + i);
 *                       } else {
 *                         return null;
 *                       }
 *                     }
 *                   });
 *
 *                   context.render(serverSentEvents(eventStream));
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
 *       ReceivedResponse response = app.getHttpClient().get();
 *       assert response.getHeaders().get("Content-Type").equals("text/event-stream;charset=UTF-8");
 *
 *       List&lt;String&gt; outputEvents = Lists.transform(Lists.newArrayList(0, 1, 2, 3, 4), new com.google.common.base.Function&lt;Integer, String&gt;() {
 *         public String apply(Integer i) {
 *           return "event: counter\ndata: event " + i + "\nid: " + i + "\n";
 *         }
 *       });
 *
 *       String expectedOutput = Joiner.on("\n").join(outputEvents) + "\n";
 *       assert response.getBody().getText().equals(expectedOutput);
 *     }
 *   }
 *
 * }
 * </pre>
 *
 * @see ServerSentEvents
 * @see <a href="http://en.wikipedia.org/wiki/Server-sent_events" target="_blank">Wikipedia - Using server-sent events</a>
 * @see <a href="https://developer.mozilla.org/en-US/docs/Server-sent_events/Using_server-sent_events" target="_blank">MDN - Using server-sent events</a>
 */
public class ServerSentEvents {

  public static ServerSentEvents serverSentEvents(final Publisher<? extends ServerSentEvent> publisher) {
    return new ServerSentEvents(publisher);
  }

  private final Publisher<? extends ServerSentEvent> publisher;

  private ServerSentEvents(Publisher<? extends ServerSentEvent> publisher) {
    this.publisher = publisher;
  }

  public Publisher<? extends ServerSentEvent> getPublisher() {
    return publisher;
  }
}
