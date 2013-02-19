package org.ratpackframework.app.internal;

import org.jboss.netty.buffer.ChannelBuffer;
import org.ratpackframework.handler.Handler;
import org.ratpackframework.util.IoUtils;
import org.ratpackframework.routing.Routed;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ReloadableFileBackedHandler<T> implements Handler<Routed<T>> {

  private final File file;
  private final boolean reloadable;
  private final Delegate<T> delegate;

  private final AtomicLong lastModifiedHolder = new AtomicLong(-1);
  private final AtomicReference<ChannelBuffer> contentHolder = new AtomicReference<>();
  private final AtomicReference<Handler<Routed<T>>> delegateHolder = new AtomicReference<>(null);
  private final Lock lock = new ReentrantLock();

  static public interface Delegate<T> {
    Handler<Routed<T>> produce(File file, ChannelBuffer bytes);
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
      return;
    }

    delegateHolder.get().handle(routed);
  }

  private boolean isBytesAreSame() throws IOException {
    ChannelBuffer existing = contentHolder.get();
    //noinspection SimplifiableIfStatement
    if (existing == null) {
      return false;
    }

    return IoUtils.readFile(file).equals(existing);
  }

  private boolean refreshNeeded() throws IOException {
    return (file.lastModified() != lastModifiedHolder.get()) || !isBytesAreSame();
  }

  private void refresh() throws IOException {
    lock.lock();
    try {
      long lastModifiedTime = file.lastModified();
      ChannelBuffer bytes = IoUtils.readFile(file);

      if (lastModifiedTime == lastModifiedHolder.get() && bytes.equals(contentHolder.get())) {
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
