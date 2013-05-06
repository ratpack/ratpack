package org.ratpackframework.reload.internal;

import org.jboss.netty.buffer.ChannelBuffer;
import org.ratpackframework.util.IoUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ReloadableFileBackedFactory<T> implements Factory<T> {

  private final File file;
  private final boolean reloadable;
  private final Delegate<T> delegate;

  private final AtomicLong lastModifiedHolder = new AtomicLong(-1);
  private final AtomicReference<ChannelBuffer> contentHolder = new AtomicReference<>();
  private final AtomicReference<T> delegateHolder = new AtomicReference<>(null);
  private final Lock lock = new ReentrantLock();

  static public interface Delegate<T> {
    T produce(File file, ChannelBuffer bytes);
  }

  public ReloadableFileBackedFactory(File file, boolean reloadable, Delegate<T> delegate) {
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
  public T create() {
    if (!reloadable) {
      return delegateHolder.get();
    }

    if (!file.exists()) {
      return null;
    }

    try {
      if (refreshNeeded()) {
        refresh();
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return delegateHolder.get();
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
