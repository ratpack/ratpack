package org.ratpackframework.routing.internal;

import org.codehaus.groovy.runtime.ResourceGroovyMethods;
import org.ratpackframework.Handler;
import org.ratpackframework.routing.Routed;

import java.io.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ReloadableFileBackedHandler<T> implements Handler<Routed<T>> {

  private final File file;
  private final boolean reloadable;
  private final Delegate<T> delegate;

  private final AtomicLong lastModifiedHolder = new AtomicLong(-1);
  private final AtomicReference<byte[]> contentHolder = new AtomicReference<>();
  private final AtomicReference<Handler<Routed<T>>> delegateHolder = new AtomicReference<>(null);
  private final Lock lock = new ReentrantLock();

  static public interface Delegate<T> {
    Handler<Routed<T>> produce(File file, byte[] bytes);
  }

  public ReloadableFileBackedHandler(File file, boolean reloadable, Delegate<T> delegate) {
    this.file = file;
    this.reloadable = reloadable;
    this.delegate = delegate;

    if (!reloadable) {
      try {
        refresh();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void handle(final Routed<T> routed) {
    if (!reloadable) {
      delegateHolder.get().handle(routed);
      return;
    }

    if (!file.exists()) {
      routed.next();
      return;
    }

    try {
      if (refreshNeeded()) {
        refresh();
      }
    } catch (Exception e) {
      routed.error(e);
    }

    delegateHolder.get().handle(routed);
  }

  private boolean isBytesAreSame() throws IOException {
    byte[] existing = contentHolder.get();
    if (existing == null) {
      return false;
    }

    FileInputStream fileIn = new FileInputStream(file);
    InputStream in = new BufferedInputStream(fileIn);
    int i = 0;
    int b = in.read();
    while (b != -1 && i < existing.length) {
      if (b != existing[i++]) {
        return false;
      }
    }

    return true;
  }

  private boolean refreshNeeded() throws IOException {
    return (file.lastModified() != lastModifiedHolder.get()) || !isBytesAreSame();
  }

  private void refresh() throws IOException {
    lock.lock();
    try {
      long lastModifiedTime = file.lastModified();
      byte[] bytes = ResourceGroovyMethods.getBytes(file);

      if (lastModifiedTime == lastModifiedHolder.get() && Arrays.equals(bytes, contentHolder.get())) {
        return;
      }

      delegateHolder.set(delegate.produce(file, bytes));

      this.lastModifiedHolder.set(lastModifiedTime);
      this.contentHolder.set(bytes);
    } finally {
      lock.unlock();
    }
  }


}
