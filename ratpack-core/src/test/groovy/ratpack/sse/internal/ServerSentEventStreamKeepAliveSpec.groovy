/*
 * Copyright 2021 the original author or authors.
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

package ratpack.sse.internal

import io.netty.buffer.UnpooledByteBufAllocator
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import ratpack.sse.ServerSentEvent
import ratpack.stream.Streams
import ratpack.stream.internal.CollectingSubscriber
import ratpack.stream.internal.ManagedSubscription
import ratpack.test.internal.time.FixedWindableClock
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class ServerSentEventStreamKeepAliveSpec extends Specification {

  Duration frequency = Duration.ofSeconds(1)
  ManagedSubscription<ServerSentEvent> subscription
  boolean cancelled
  FixedWindableClock clock = new FixedWindableClock(Instant.EPOCH, ZoneId.systemDefault())
  Runnable scheduled
  boolean scheduledCancelled

  def heartbeat = ": keepalive heartbeat\n\n"

  CollectingSubscriber<String> subscribe() {
    def source = new Publisher<ServerSentEvent>() {
      @Override
      void subscribe(Subscriber s) {
        subscription = new ManagedSubscription<ServerSentEvent>(s, {}) {
          @Override
          protected void onRequest(long n) {
          }

          @Override
          protected void onCancel() {
            cancelled = true
          }
        }
        s.onSubscribe(subscription)
      }
    }

    def keepAlive = new ServerSentEventStreamKeepAlive(
      source.map { ServerSentEventEncoder.INSTANCE.encode(it, UnpooledByteBufAllocator.DEFAULT) },
      [schedule: { Runnable runnable, long delay, TimeUnit timeUnit ->
        scheduled = runnable
        [cancel: { boolean interrupt -> scheduledCancelled = true }] as ScheduledFuture
      }] as ScheduledExecutorService,
      frequency,
      { Duration.between(Instant.EPOCH, clock.instant()).toNanos() } as Clock
    )

    CollectingSubscriber.subscribe(Streams.map(keepAlive) {
      def s = it.toString(StandardCharsets.UTF_8)
      it.release()
      s
    })
  }

  def "issues keep alive after frequency"() {
    given:
    def subscriber = subscribe()
    subscriber.subscription.request(1)

    expect:
    subscription.demand == 1

    when:
    scheduled.run()

    then:
    subscriber.received.empty

    when:
    clock.windClock(frequency)

    and:
    scheduled.run()

    then:
    subscriber.received.pop() == heartbeat
  }

  def "does not issue keep alive if no demand"() {
    given:
    def subscriber = subscribe()
    subscriber.subscription.request(1)

    expect:
    subscription.demand == 1

    when:
    scheduled.run()

    then:
    subscriber.received.empty // too early

    when:
    clock.windClock(frequency)

    and:
    scheduled.run()
    subscriber.request(1)

    then:
    subscriber.received.pop() == heartbeat
    subscription.demand == 1 // upstream demand is unchanged

    when:
    subscription.emitNext(event("1"))
    subscriber.received.pop() == "data: 1\n\n"
    clock.windClock(frequency)

    and:
    scheduled.run()

    then:
    subscriber.received.empty

    when:
    clock.windClock(frequency)
    subscriber.request(2)

    then:
    subscriber.received.pop() == heartbeat
    subscriber.received.empty
    subscription.demand == 2

    when:
    clock.windClock(frequency)
    scheduled.run()
    clock.windClock(frequency)
    scheduled.run()
    clock.windClock(frequency)
    scheduled.run()

    then:
    subscription.demand == 2

    and:
    subscriber.received.size() == 1
    subscriber.received.pop() == heartbeat
    subscriber.received.empty

    when:
    subscriber.request(1)
    clock.windClock(frequency)
    scheduled.run()
    clock.windClock(frequency)
    scheduled.run()

    then:
    subscription.demand == 2
    subscriber.received.size() == 1
    subscriber.received.pop() == heartbeat
    subscriber.received.empty
  }

  private static ServerSentEvent event(String data) {
    ServerSentEvent.builder().data(data).build()
  }

}
