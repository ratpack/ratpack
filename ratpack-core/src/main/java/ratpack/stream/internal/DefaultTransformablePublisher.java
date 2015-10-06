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

package ratpack.stream.internal;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import ratpack.stream.TransformablePublisher;

public class DefaultTransformablePublisher<T> implements TransformablePublisher<T> {

  private final Publisher<? extends T> delegate;

  public DefaultTransformablePublisher(Publisher<? extends T> delegate) {
    this.delegate = delegate;
  }

  @Override
  public void subscribe(Subscriber<? super T> s) {
    delegate.subscribe(s);
  }

}
