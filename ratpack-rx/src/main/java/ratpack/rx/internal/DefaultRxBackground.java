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

package ratpack.rx.internal;

import ratpack.handling.Background;
import ratpack.rx.RxBackground;
import ratpack.rx.RxRatpack;
import rx.Observable;

import javax.inject.Inject;
import java.util.concurrent.Callable;

public class DefaultRxBackground implements RxBackground {

  private final Background background;

  @Inject
  public DefaultRxBackground(Background background) {
    this.background = background;
  }

  @Override
  public <T> Observable<T> observe(final Callable<T> callable) {
    return RxRatpack.observe(background.exec(callable));
  }

  @Override
  public <I extends Iterable<T>, T> Observable<T> observeEach(final Callable<I> callable) {
    return RxRatpack.observeEach(background.exec(callable));
  }

}

