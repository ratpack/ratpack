/*
 * Copyright 2017 the original author or authors.
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

package ratpack.stream.tck;

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import ratpack.func.Action;
import ratpack.stream.Streams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FlattenPublisherVerification extends PublisherVerification<Integer> {

  public FlattenPublisherVerification() {
    super(new TestEnvironment());
  }

  @Override
  public Publisher<Integer> createPublisher(long elements) {
    elements = Math.min(elements, 10000);
    List<Publisher<Integer>> publishers = new ArrayList<>();
    for (int i = 0; i < elements; ++i) {
      publishers.add(Streams.publish(Collections.singleton(1)));
    }
    return Streams.flatten(Streams.publish(publishers), Action.noop());
  }

  @Override
  public Publisher<Integer> createFailedPublisher() {
    return null; // because subscription always succeeds. Nothing is attempted until a request is received.
  }

}
