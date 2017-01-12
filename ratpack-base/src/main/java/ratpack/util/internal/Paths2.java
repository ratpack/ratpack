/*
 * Copyright 2015 the original author or authors.
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

package ratpack.util.internal;

import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.google.common.base.Preconditions.checkNotNull;

public class Paths2 {

  public static ByteSource asByteSource(Path path) {
    return new PathByteSource(path);
  }

  public static CharSource asCharSource(Path path, Charset charset) {
    return asByteSource(path).asCharSource(charset);
  }

  public static String readText(Path path, Charset charset) throws IOException {
    return asCharSource(path, charset).read();
  }

  private static final class PathByteSource extends ByteSource {
    private final Path path;

    private PathByteSource(Path path) {
      this.path = checkNotNull(path);
    }

    @Override
    public InputStream openStream() throws IOException {
      return Files.newInputStream(path);
    }

    @Override
    public long size() throws IOException {
      return Files.size(path);
    }

    @Override
    public byte[] read() throws IOException {
      return Files.readAllBytes(path);
    }

    @Override
    public String toString() {
      return "Paths2.asByteSource(" + path + ")";
    }
  }
}
