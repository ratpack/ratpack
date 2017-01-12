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

package ratpack.stream.internal;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import ratpack.func.Action;
import ratpack.stream.TransformablePublisher;

import java.util.concurrent.atomic.AtomicBoolean;

public class FlattenPublisher<T> implements TransformablePublisher<T> {

    private final Publisher<? extends Publisher<T>> publisher;
    private final Action<? super T> disposer;

    public FlattenPublisher(Publisher<? extends Publisher<T>> publisher, Action<? super T> disposer) {
        this.publisher = publisher;
        this.disposer = disposer;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        subscriber.onSubscribe(new ManagedSubscription<T>(subscriber, disposer) {

            private Subscription outerSubscription;
            private Subscription innerSubscription;

            private final AtomicBoolean pending = new AtomicBoolean();

            @Override
            protected void onRequest(long n) {
                if (outerSubscription == null) {
                    subscribeUpstream();
                } else if (innerSubscription == null) {
                    nextPublisher();
                } else {
                    innerSubscription.request(n);
                }
            }

            private void subscribeUpstream() {
                publisher.subscribe(new Subscriber<Publisher<T>>() {
                    @Override
                    public void onSubscribe(Subscription subscription) {
                        outerSubscription = subscription;
                        outerSubscription.request(1);
                    }

                    @Override
                    public void onNext(Publisher<T> next) {
                        next.subscribe(new Subscriber<T>() {
                            @Override
                            public void onSubscribe(Subscription s) {
                                innerSubscription = s;
                                innerSubscription.request(getDemand());
                                pending.set(false);
                            }

                            @Override
                            public void onNext(T t) {
                                emitNext(t);
                            }

                            @Override
                            public void onError(Throwable t) {
                                outerSubscription.cancel();
                                emitError(t);
                            }

                            @Override
                            public void onComplete() {
                                nextPublisher();
                            }
                        });
                    }

                    @Override
                    public void onError(Throwable t) {
                        if (innerSubscription != null) {
                            innerSubscription.cancel();
                            innerSubscription = null;
                        }
                        emitError(t);
                    }

                    @Override
                    public void onComplete() {
                        emitComplete();
                    }
                });
            }

            @Override
            protected void onCancel() {
                if (innerSubscription != null) {
                    innerSubscription.cancel();
                    innerSubscription = null;
                }
                if (outerSubscription != null) {
                    outerSubscription.cancel();
                    outerSubscription = null;
                }

            }

            private void nextPublisher() {
                if (pending.compareAndSet(false, true)) {
                    innerSubscription = null;
                    outerSubscription.request(1);
                }
            }
        });

    }

}
