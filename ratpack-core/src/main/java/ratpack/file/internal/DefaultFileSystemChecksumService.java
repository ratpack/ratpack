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
import ratpack.file.FileSystemBinding;
import ratpack.func.Function;

import java.util.List;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.NoSuchFileException;

public class DefaultFileSystemChecksumService implements FileSystemChecksumService {

  private final Function<? super InputStream, ? extends String> checksummer;
  private final FileSystemBinding fileSystemBinding;
  private final List<String> fileEndsWith;

  public DefaultFileSystemChecksumService(FileSystemBinding fileSystemBinding, Function<? super InputStream, ? extends String> checksummer, List<String> fileEndsWith) {
    this.checksummer = checksummer;
    this.fileSystemBinding = fileSystemBinding;
    this.fileEndsWith = fileEndsWith;
  }

  /**
   *  Calculate checksum for file given by ```path``` relative to ```fileSystemBinding```.
   *  If fileEndsWith list is present and given file extenstion no match then NoSuchFileException is thrown.
   *
   *  @param path file pathe relative to root defined by ```fileSystemBinding```
   *  @return calculated checksum
   *  @throws Exception NoSuchFileException
   */
  @Nullable
  @Override
  public String checksum(String path) throws Exception {
    if (path == null) {
      return null;
    }
    Path child = fileSystemBinding.file(path);
    if (child == null) {
      throw new NoSuchFileException(path);
    }
    if (fileEndsWith != null && !fileEndsWith.isEmpty()) {
      if (fileEndsWith.stream().noneMatch(path::endsWith)) {
        throw new NoSuchFileException(child.toString());
      }
    }
    return getChecksum(child);
  }

  private String getChecksum(Path child) throws Exception {
    try (InputStream inputStream = Files.newInputStream(child, StandardOpenOption.READ)) {
      return checksummer.apply(inputStream);
    }
  }

}
