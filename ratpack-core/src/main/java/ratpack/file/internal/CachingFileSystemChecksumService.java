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

package ratpack.file.internal;

import ratpack.api.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CachingFileSystemChecksumService implements FileSystemChecksumService {

  private final FileSystemChecksumService delegate;

  public CachingFileSystemChecksumService(FileSystemChecksumService delegate) {
    this.delegate = delegate;
  }

  private final ConcurrentMap<String, String> cache = new ConcurrentHashMap<>();

  @Nullable
  @Override
  public String checksum(String path) throws Exception {
    if (path == null) {
      return null;
    }
    String checksum = cache.get(path);
    if (checksum == null) {
      checksum = delegate.checksum(path);
      cache.put(path, checksum);
    }
    return checksum;
  }

}
