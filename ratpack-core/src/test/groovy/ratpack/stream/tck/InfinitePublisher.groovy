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

package ratpack.stream.tck

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

/**
 * An infinite producing Publisher for testing Subscribers with the TCK.
 * <p>
 * This Publisher is suitable for testing only.
 * <p>
 * This Publisher keeps publishing elements as long as a Subscriber keeps asking for them.
 * It will never call Subscriber#onComplete
 */
class InfinitePublisher implements Publisher<Integer> {

  public static InfinitePublisher infinitePublish() {
    return new InfinitePublisher()
  }

  private InfinitePublisher() {}

  @Override
  void subscribe(Subscriber<? super Integer> s) {
    s.onSubscribe(new Subscription() {
      @Override
      void request(long n) {
        (0..<n).each {
          s.onNext(n)
        }
      }

      @Override
      void cancel() { }
    })
  }

}
