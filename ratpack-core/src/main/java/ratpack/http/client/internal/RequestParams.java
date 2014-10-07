

package ratpack.http.client.internal;

import java.util.concurrent.TimeUnit;

public class RequestParams {

  long readTimeoutNanos = TimeUnit.SECONDS.toNanos(30);

}
