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

package ratpack.test.internal.snippets.junit;

import org.junit.runner.Description;
import ratpack.func.Block;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

final class TimeoutInterceptor {

  private final Duration timeout;

  public TimeoutInterceptor(Duration timeout) {
    this.timeout = timeout;
  }

  public void intercept(Block runnable, Description description) throws Throwable {
    final Thread mainThread = Thread.currentThread();
    final SynchronousQueue<StackTraceElement[]> sync = new SynchronousQueue<>();
    final CountDownLatch startLatch = new CountDownLatch(2);

    new Thread(String.format("[TimeoutInterceptor] Watcher for method '%s'", description)) {
      @Override
      public void run() {
        StackTraceElement[] stackTrace = new StackTraceElement[0];
        long waitMillis = timeout.toMillis();
        boolean synced = false;

        try {
          startLatch.countDown();
          //noinspection ResultOfMethodCallIgnored
          startLatch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
          System.out.printf("[TimeoutInterceptor] Could not sync with Feature for method '%s'", description);
        }

        while (!synced) {
          try {
            synced = sync.offer(stackTrace, waitMillis, TimeUnit.MILLISECONDS);
          } catch (InterruptedException ignored) {
            // The mission of this thread is to repeatedly interrupt the main thread until
            // the latter returns. Once this mission has been accomplished, this thread will die quickly
          }
          if (!synced) {
            if (stackTrace.length == 0) {
              stackTrace = mainThread.getStackTrace();
              waitMillis = 250;
            } else {
              waitMillis *= 2;
              System.out.printf("[TimeoutInterceptor] Method '%s' has not yet returned - interrupting. Next try in %1.2f seconds.\n",
                description, waitMillis / 1000.);
            }
            mainThread.interrupt();
          }
        }
      }
    }.start();


    try {
      startLatch.countDown();
      //noinspection ResultOfMethodCallIgnored
      startLatch.await(5, TimeUnit.SECONDS);
    } catch (InterruptedException ignored) {
      System.out.printf("[spock.lang.Timeout] Could not sync with Watcher for method '%s'", description);
    }

    Throwable saved = null;
    try {
      runnable.execute();
    } catch (Throwable t) {
      saved = t;
    }
    StackTraceElement[] stackTrace = null;
    while (stackTrace == null) {
      try {
        stackTrace = sync.take();
      } catch (InterruptedException e) {
        // There is a small chance that this came from the watcher thread,
        // i.e. the two threads narrowly missed each other at the sync point.
        // Therefore, let's sync again to learn whether this thread has timed out or not.
        // As this won't take long, it should also be acceptable if this thread
        // got interrupted by some other thread. To report on the latter case,
        // we save off the exception, overriding any previously saved exception.
        saved = e;
      }
    }
    if (stackTrace.length > 0) {
      // We know that this thread got timed out (and interrupted) by the watcher thread and
      // act accordingly. We gloss over the fact that some other thread might also have tried to
      // interrupt this thread. This shouldn't be a problem in practice, in particular because
      // throwing an InterruptedException wouldn't abort the whole test run anyway.
      double timeoutSeconds = timeout.toMillis() / 1000.0;
      String msg = String.format("Method timed out after %1.2f seconds", timeoutSeconds);
      AssertionError error = new AssertionError(msg, null);
      error.setStackTrace(stackTrace);
      throw error;
    }
    if (saved != null) {
      throw saved;
    }
  }
}
