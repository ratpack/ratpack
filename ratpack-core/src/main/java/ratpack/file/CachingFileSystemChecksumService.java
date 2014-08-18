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

package ratpack.file;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import ratpack.api.Nullable;
import ratpack.util.ExceptionUtils;

import java.util.concurrent.ExecutionException;

public class CachingFileSystemChecksumService implements FileSystemChecksumService {

  private static class Entry {
    private final String checksum;
    private Entry(String checksum) {
      this.checksum = checksum;
    }
  }

  private final FileSystemChecksumService delegate;

  public CachingFileSystemChecksumService(FileSystemChecksumService delegate) {
    this.delegate = delegate;
  }

  private final LoadingCache<String, Entry> cache = CacheBuilder.newBuilder().build(new CacheLoader<String, Entry>() {
    @Override
    public Entry load(String key) throws Exception {
      return new Entry(delegate.checksum(key));
    }
  });

  @Nullable
  @Override
  public String checksum(String path) throws Exception {
    try {
      return cache.get(path).checksum;
    } catch (ExecutionException | UncheckedExecutionException e) {
      throw ExceptionUtils.toException(e.getCause());
    }
  }

}
