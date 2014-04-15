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

package ratpack.rx;

import ratpack.exec.ExecContext;
import ratpack.exec.ExecException;
import ratpack.exec.SuccessOrErrorPromise;
import ratpack.func.Action;
import ratpack.util.ExceptionUtils;
import rx.Observable;
import rx.Subscriber;
import rx.exceptions.OnErrorNotImplementedException;
import rx.functions.Action1;
import rx.functions.Action2;

import static ratpack.util.ExceptionUtils.toException;

/**
 * Provides static access Rx helper functions.
 */
public abstract class RxRatpack {

  /**
   * Converts a Ratpack promise into an Rx observable.
   *
   * @param promise the promise
   * @param <T> the type of value promised
   * @return an observable for the promised value
   */
  public static <T> Observable<T> observe(SuccessOrErrorPromise<T> promise) {
    return Observable.create(new PromiseSubscribe<>(promise, new Action2<T, Subscriber<? super T>>() {
      @Override
      public void call(T thing, Subscriber<? super T> subscriber) {
        subscriber.onNext(thing);
      }
    }));
  }

  /**
   * Converts a Ratpack promise of an iterable value into an Rx observable for each element of the promised iterable.
   *
   * @param promise the promise
   * @param <T> the element type of the promised iterable
   * @param <I> the type of iterable
   * @return an observable for each element of the promised iterable
   */
  public static <T, I extends Iterable<T>> Observable<T> observeEach(SuccessOrErrorPromise<I> promise) {
    return Observable.create(new PromiseSubscribe<>(promise, new Action2<I, Subscriber<? super T>>() {
      @Override
      public void call(I things, Subscriber<? super T> subscriber) {
        for (T thing : things) {
          subscriber.onNext(thing);
        }
      }
    }));
  }

  /**
   * An error handler suitable for use with {@link Observable#subscribe(Action1, Action1)}.
   * <pre class="tested">
   * import ratpack.handling.*;
   * import ratpack.func.Action;
   * import rx.Observable;
   * import rx.functions.Action1;
   *
   * import java.util.concurrent.Callable;
   *
   * import static ratpack.rx.RxRatpack.errorHandler;
   *
   * public class RenderAction implements Action1&lt;String&gt; {
   *   private final Context context;
   *
   *   public RenderAction(Context context) {
   *     this.context = context;
   *   }
   *
   *   public void call(String string) {
   *     context.render(string);
   *   }
   * }
   *
   * public class TheHandler implements Handler {
   *   void handle(final Context context) {
   *     Observable.&lt;String&gt;error(new Exception("bang!")).subscribe(new RenderAction(context), errorHandler(context));
   *   }
   * }
   *
   * // Test (Groovy) &hellip;
   *
   * import ratpack.test.embed.PathBaseDirBuilder
   * import ratpack.groovy.test.TestHttpClients
   * import ratpack.error.ServerErrorHandler
   * import ratpack.error.DebugErrorHandler
   * import static ratpack.groovy.test.embed.EmbeddedApplications.embeddedApp
   * import static ratpack.registry.Registries.just
   *
   * def app = embeddedApp {
   *   handlers {
   *     register just(ServerErrorHandler, { Context context, Exception error -> context.render("error!") } as ServerErrorHandler)
   *     get("errorHandler", new TheHandler())
   *   }
   * }
   *
   * def client = TestHttpClients.testHttpClient(app)
   *
   * assert client.getText("errorHandler") == "error!"
   *
   * app.close()
   * </pre>
   *
   * @param context the exec context
   * @return an error handler
   */
  public static Action1<Throwable> errorHandler(final ExecContext context) {
    return new Action1<Throwable>() {
      @Override
      public void call(Throwable throwable) {
        if (throwable instanceof ExecException) {
          ExecException execException = (ExecException) throwable;
          execException.getContext().error(toException(execException.getCause()));
        } else {
          context.error(toException(throwable));
        }
      }
    };
  }

  private static class PromiseSubscribe<T, S> implements Observable.OnSubscribe<S> {
    private final SuccessOrErrorPromise<T> promise;
    private final Action2<T, Subscriber<? super S>> emitter;

    public PromiseSubscribe(SuccessOrErrorPromise<T> promise, Action2<T, Subscriber<? super S>> emitter) {
      this.promise = promise;
      this.emitter = emitter;
    }

    @Override
    public void call(final Subscriber<? super S> subscriber) {
      try {
        promise
          .onError(new Action<Throwable>() {
            @Override
            public void execute(Throwable throwable) throws Exception {
              try {
                subscriber.onError(throwable);
              } catch (OnErrorNotImplementedException e) {
                throw ExceptionUtils.toException(e.getCause());
              }
            }
          })
          .then(new Action<T>() {
            @Override
            public void execute(T thing) throws Exception {
              try {
                emitter.call(thing, subscriber);
                subscriber.onCompleted();
              } catch (OnErrorNotImplementedException e) {
                throw ExceptionUtils.toException(e.getCause());
              }
            }
          });
      } catch (Exception e) {
        throw ExceptionUtils.uncheck(e);
      }
    }
  }

}

