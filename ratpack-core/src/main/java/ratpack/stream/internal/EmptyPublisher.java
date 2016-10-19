/*
 * Copyright 2016 the original author or authors.
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

package ratpack.stream.internal;

import org.reactivestreams.Subscriber;
import ratpack.stream.TransformablePublisher;
import ratpack.util.Types;

public class EmptyPublisher<T> implements TransformablePublisher<T> {

  private static final TransformablePublisher<?> INSTANCE = new EmptyPublisher<>();

  public static <T> TransformablePublisher<T> instance() {
    return Types.cast(INSTANCE);
  }

  @Override
  public void subscribe(Subscriber<? super T> s) {
    s.onSubscribe(new SubscriptionSupport<T>(s) {
      {
        start();
      }

      @Override
      protected void doRequest(long n) {
        onComplete();
      }
    });
  }

}
