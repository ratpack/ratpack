package ratpack.test;

import ratpack.handling.Handler;
import ratpack.http.client.ReceivedResponse;
import ratpack.sse.ServerSentEvent;
import ratpack.test.embed.EmbeddedApplication;

import java.util.Arrays;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static ratpack.sse.ServerSentEvents.serverSentEvents;
import static ratpack.stream.Streams.periodically;

public class Example {

  public static void main(String[] args) throws Exception {
    Handler handler = context ->
      context.render(serverSentEvents(periodically(context.getLaunchConfig(), 5, MILLISECONDS, i ->
          i < 5
            ? ServerSentEvent.builder().id(i.toString()).type("counter").data("event " + i).build()
            : null
      )));

    String expectedOutput = Arrays.asList(0, 1, 2, 3, 4)
      .stream()
      .map(i -> "event: counter\ndata: event " + i + "\nid: " + i + "\n")
      .collect(Collectors.joining("\n"))
      + "\n";

    EmbeddedApplication.fromHandler(handler).test(httpClient -> {
      ReceivedResponse response = httpClient.get();
      assert response.getHeaders().get("Content-Type").equals("text/event-stream;charset=UTF-8");
      assert response.getBody().getText().equals(expectedOutput);
    });
  }

}