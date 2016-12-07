package ratpack.stream.tck;

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import ratpack.func.Action;
import ratpack.stream.Streams;
import ratpack.stream.TransformablePublisher;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.Executors;

public class ConcatPublisherVerification extends PublisherVerification<Long> {

  public ConcatPublisherVerification() {
    super(new TestEnvironment());
  }

  @Override
  public Publisher<Long> createPublisher(long elements) {
    double p1 = Math.floor((double) elements / 2);
    double p2 = Math.ceil((double) elements / 2);

    return Streams.concat(Arrays.asList(makePublisher(p1), makePublisher(p2)), Action.noop());
  }

  private static TransformablePublisher<Long> makePublisher(final double elements) {
    return Streams.periodically(Executors.newSingleThreadScheduledExecutor(), Duration.ofMillis(5), i -> i < elements ? 1L : null);
  }

  @Override
  public Publisher<Long> createFailedPublisher() {
    return null;// because subscription always succeeds. Nothing is attempted until a request is received.
  }

}
