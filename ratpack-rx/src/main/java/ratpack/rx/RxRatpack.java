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

import com.google.common.base.Optional;
import ratpack.exec.*;
import ratpack.exec.internal.DefaultExecController;
import ratpack.func.Action;
import ratpack.rx.internal.DefaultSchedulers;
import ratpack.rx.internal.ExecControllerBackedScheduler;
import ratpack.util.ExceptionUtils;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.exceptions.OnErrorNotImplementedException;
import rx.functions.Action2;
import rx.internal.operators.OperatorSingle;
import rx.plugins.RxJavaDefaultSchedulers;
import rx.plugins.RxJavaObservableExecutionHook;
import rx.plugins.RxJavaPlugins;

import java.util.List;

import static ratpack.util.ExceptionUtils.toException;

/**
 * Provides integration with <a href="https://github.com/Netflix/RxJava">RxJava</a>.
 * <p>
 * <b>IMPORTANT:</b> the {@link #initialize()} method must be called to fully enable integration.
 */
public abstract class RxRatpack {

  private static boolean isDuringExecution() {
    Optional<ExecController> threadBoundController = DefaultExecController.getThreadBoundController();
    if (threadBoundController.isPresent()) {
      try {
        threadBoundController.get().getExecution();
        return true;
      } catch (ExecutionException e) {
        return false;
      }
    } else {
      return false;
    }
  }

  /**
   * Registers an {@link RxJavaObservableExecutionHook} with RxJava that provides a default error handling strategy of {@link ratpack.exec.Execution#setErrorHandler(ratpack.func.Action)} forwarding exceptions to the execution}.
   * <p>
   * This method is idempotent.
   * It only needs to be called once per JVM, regardless of how many Ratpack applications are running within the JVM.
   * <p>
   * For a Java application, a convenient place to call this is in the handler factory implementation.
   * <pre class="java">
   * import ratpack.launch.HandlerFactory;
   * import ratpack.launch.LaunchConfig;
   * import ratpack.handling.Handler;
   * import ratpack.handling.Handlers;
   * import ratpack.handling.Context;
   * import ratpack.handling.ChainAction;
   * import ratpack.error.ServerErrorHandler;
   * import ratpack.registry.RegistrySpecAction;
   * import ratpack.rx.RxRatpack;
   * import rx.Observable;
   * import rx.functions.Action1;
   *
   * public class Example {
   *   public static class MyHandlerFactory implements HandlerFactory {
   *     public Handler create(LaunchConfig launchConfig) throws Exception {
   *
   *       // Enable Rx integration
   *       RxRatpack.initialize();
   *
   *       return Handlers.chain(launchConfig, new ChainAction() {
   *         public void execute() throws Exception {
   *           register(new RegistrySpecAction() { // register a custom error handler
   *             public void execute() {
   *               add(ServerErrorHandler.class, new ServerErrorHandler() {
   *                 public void error(Context context, Exception exception) {
   *                   context.render("caught by error handler!");
   *                 }
   *               });
   *             }
   *           });
   *
   *           get(new Handler() {
   *             public void handle(Context context) {
   *               // An observable sequence with no defined error handler
   *               // The error will be propagated to context error handler implicitly
   *               Observable.&lt;String&gt;error(new Exception("!")).subscribe(new Action1&lt;String&gt;() {
   *                 public void call(String str) {
   *                   // will never be called
   *                 }
   *               });
   *             }
   *           });
   *         }
   *       });
   *     }
   *   }
   * }
   * </pre>
   * <p>
   * For a Groovy DSL application, it can be registered during the module bindings.
   * <pre class="tested">
   * import ratpack.handling.Context
   * import ratpack.error.ServerErrorHandler
   * import ratpack.rx.RxRatpack
   * import rx.Observable
   *
   * import static ratpack.groovy.test.embed.EmbeddedApplications.embeddedApp
   * import static ratpack.groovy.test.TestHttpClients.testHttpClient
   *
   * class CustomErrorHandler implements ServerErrorHandler {
   *   void error(Context context, Exception exception) {
   *     context.render("caught by error handler!")
   *   }
   * }
   *
   * def app = embeddedApp {
   *   bindings {
   *     // Enable Rx integration
   *     RxRatpack.initialize()
   *
   *     bind ServerErrorHandler, CustomErrorHandler
   *   }
   *
   *   handlers {
   *     get {
   *       // An observable sequence with no defined error handler
   *       // The error will be propagated to context error handler implicitly
   *       Observable.error(new Exception("!")).subscribe {
   *         // will never happen
   *       }
   *     }
   *   }
   * }
   *
   * def client = testHttpClient(app)
   *
   * try {
   *   client.getText() == "caught by error handler!"
   * } finally {
   *   app.close()
   * }
   * </pre>
   */
  public static void initialize() {
    RxJavaPlugins plugins = RxJavaPlugins.getInstance();

    ExecutionHook ourHook = new ExecutionHook();
    try {
      plugins.registerObservableExecutionHook(ourHook);
    } catch (IllegalStateException e) {
      RxJavaObservableExecutionHook existingHook = plugins.getObservableExecutionHook();
      if (!(existingHook instanceof ExecutionHook)) {
        throw new IllegalStateException("Cannot install RxJava integration because another execution hook (" + existingHook.getClass() + ") is already installed");
      }
    }

    System.setProperty("rxjava.plugin." + RxJavaDefaultSchedulers.class.getSimpleName() + ".implementation", DefaultSchedulers.class.getName());
    RxJavaDefaultSchedulers existingSchedulers = plugins.getDefaultSchedulers();
    if (!(existingSchedulers instanceof DefaultSchedulers)) {
      throw new IllegalStateException("Cannot install RxJava integration because another set of default schedulers (" + existingSchedulers.getClass() + ") is already installed");
    }
  }

  /**
   * A scheduler that uses the application event loop and initialises each job as an {@link ratpack.exec.Execution} (via {@link ratpack.exec.ExecController#start(ratpack.func.Action)}).
   *
   * @param execController the execution controller to back the scheduler
   * @return a scheduler
   */
  public static Scheduler scheduler(final ExecController execController) {
    return new ExecControllerBackedScheduler(execController);
  }

  /**
   * Forks the current execution in order to subscribe to the given source, then joining the original execution with the source values.
   * <p>
   * This method supports parallelism in the observable stream.
   * <p>
   * This method uses {@link rx.Observable#toList()} on the given source to collect all values before returning control to the original execution.
   * As such, {@code source} should not be an infinite or extremely large stream.
   *
   * @param execControl the execution control
   * @param source the observable source
   * @param <T> the type of item observed
   * @return an observable stream equivalent to the given source
   */
  public static <T> Observable<T> forkAndJoin(final ExecControl execControl, final Observable<T> source) {
    Promise<List<T>> promise = execControl.promise(new Action<Fulfiller<List<T>>>() {
      @Override
      public void execute(final Fulfiller<List<T>> fulfiller) throws Exception {
        execControl.fork(new Action<Execution>() {
          @Override
          public void execute(Execution execution) throws Exception {
            source
              .toList()
              .subscribe(new Subscriber<List<T>>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {
                  fulfiller.error(e);
                }

                @Override
                public void onNext(List<T> ts) {
                  fulfiller.success(ts);
                }
              });
          }
        });
      }
    });

    return observeEach(promise);
  }

  /**
   * Converts a Ratpack promise into an Rx observable.
   * <p>
   * For example, this can be used to observe blocking operations.
   * <p>
   * In Java&hellip;
   * <pre class="tested">
   * import ratpack.handling.Handler;
   * import ratpack.handling.Context;
   * import ratpack.exec.Promise;
   * import java.util.concurrent.Callable;
   * import rx.functions.Func1;
   * import rx.functions.Action1;
   *
   * import static ratpack.rx.RxRatpack.observe;
   *
   * public class ReactiveHandler implements Handler {
   *   public void handle(Context context) {
   *     Promise&lt;String&gt; promise = context.blocking(new Callable&lt;String&gt;() {
   *       public String call() {
   *         // do some blocking IO here
   *         return "hello world";
   *       }
   *     });
   *
   *     observe(promise).map(new Func1&lt;String, String&gt;() {
   *       public String call(String input) {
   *         return input.toUpperCase();
   *       }
   *     }).subscribe(new Action1&lt;String&gt;() {
   *       public void call(String str) {
   *         context.render(str); // renders: HELLO WORLD
   *       }
   *     });
   *   }
   * }
   * </pre>
   * <p>
   * A similar example in the Groovy DSL would look like:
   * </p>
   * <pre class="groovy-chain-dsl">
   * import static ratpack.rx.RxRatpack.observe
   *
   * handler {
   *   observe(blocking {
   *     // do some blocking IO
   *     "hello world"
   *   }) map {
   *     it.toUpperCase()
   *   } subscribe {
   *     render it // renders: HELLO WORLD
   *   }
   * }
   * </pre>
   *
   * @param promise the promise
   * @param <T> the type of value promised
   * @return an observable for the promised value
   */
  public static <T> Observable<T> observe(Promise<T> promise) {
    return Observable.create(new PromiseSubscribe<>(promise, new Action2<T, Subscriber<? super T>>() {
      @Override
      public void call(T thing, Subscriber<? super T> subscriber) {
        subscriber.onNext(thing);
      }
    }));
  }

  /**
   * Converts a Ratpack promise of an iterable value into an Rx observable for each element of the promised iterable.
   * <p>
   * The promised iterable will be emitted to the observer one element at a time.
   * <p>
   * For example, this can be used to observe background operations that produce some kind of iterable&hellip;
   * <pre class="groovy-chain-dsl">
   * import static ratpack.rx.RxRatpack.observeEach
   *
   * handler {
   *   observeEach(blocking {
   *     // do some blocking IO and return a List&lt;String&gt;
   *     // each item in the List is emitted to the next Observable, not the List
   *     ["a", "b", "c"]
   *   }) map { String input ->
   *     input.toUpperCase()
   *   } subscribe {
   *     println it
   *   }
   * }
   * </pre>
   * The output would be:
   * <br>A
   * <br>B
   * <br>C
   *
   * @param promise the promise
   * @param <T> the element type of the promised iterable
   * @param <I> the type of iterable
   * @return an observable for each element of the promised iterable
   * @see #observe(ratpack.exec.Promise)
   */
  public static <T, I extends Iterable<T>> Observable<T> observeEach(Promise<I> promise) {
    return Observable.create(new PromiseSubscribe<>(promise, new Action2<I, Subscriber<? super T>>() {
      @Override
      public void call(I things, Subscriber<? super T> subscriber) {
        for (T thing : things) {
          subscriber.onNext(thing);
        }
      }
    }));
  }

  public static <T> Subscriber<? super T> subscriber(final Fulfiller<T> fulfiller) {
    return new OperatorSingle<T>().call(new Subscriber<T>() {
      @Override
      public void onCompleted() {

      }

      @Override
      public void onError(Throwable e) {
        fulfiller.error(e);
      }

      @Override
      public void onNext(T t) {
        fulfiller.success(t);
      }
    });
  }

  private static class PromiseSubscribe<T, S> implements Observable.OnSubscribe<S> {

    private final Promise<T> promise;
    private final Action2<T, Subscriber<? super S>> emitter;

    public PromiseSubscribe(Promise<T> promise, Action2<T, Subscriber<? super S>> emitter) {
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
                throw toException(e.getCause());
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
                throw toException(e.getCause());
              }
            }
          });
      } catch (Exception e) {
        throw ExceptionUtils.uncheck(e);
      }
    }

  }

  private static class ExecutionHook extends RxJavaObservableExecutionHook {

    @Override
    public <T> Observable.OnSubscribe<T> onSubscribeStart(Observable<? extends T> observableInstance, final Observable.OnSubscribe<T> onSubscribe) {
      if (isDuringExecution()) {
        return new Observable.OnSubscribe<T>() {
          @Override
          public void call(final Subscriber<? super T> subscriber) {
            onSubscribe.call(new ExecutionBackedSubscriber<>(subscriber));
          }
        };
      } else {
        return onSubscribe;
      }
    }

    @Override
    public Throwable onSubscribeError(Throwable e) {
      if (e instanceof ExecutionSegmentTerminationError) {
        throw (ExecutionSegmentTerminationError) e;
      } else if (e.getClass().equals(RuntimeException.class) || e.getCause() instanceof ExecutionSegmentTerminationError) {
        throw (ExecutionSegmentTerminationError) e.getCause();
      } else {
        return e;
      }
    }
  }

  private static class ExecutionBackedSubscriber<T> extends Subscriber<T> {
    private final Subscriber<? super T> subscriber;

    public ExecutionBackedSubscriber(Subscriber<? super T> subscriber) {
      this.subscriber = subscriber;
    }

    @Override
    public void onCompleted() {
      subscriber.onCompleted();
    }

    @Override
    public void onError(Throwable e) {
      if (e instanceof ExecutionSegmentTerminationError) {
        throw (ExecutionSegmentTerminationError) e;
      }

      try {
        try {
          subscriber.onError(e);
        } catch (OnErrorNotImplementedException onErrorNotImplementedException) {
          throw onErrorNotImplementedException.getCause();
        }
      } catch (final Throwable throwable) {
        throw new ExecutionSegmentTerminationError(throwable);
      }
    }

    @Override
    public void onNext(T t) {
      try {
        try {
          subscriber.onNext(t);
        } catch (OnErrorNotImplementedException onErrorNotImplementedException) {
          throw onErrorNotImplementedException.getCause();
        }
      } catch (Throwable throwable) {
        throw new ExecutionSegmentTerminationError(throwable);
      }
    }
  }


}

