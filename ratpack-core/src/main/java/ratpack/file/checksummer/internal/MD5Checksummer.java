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

package ratpack.file.checksummer.internal;

import java.io.InputStream;
import ratpack.func.Function;

import java.security.MessageDigest;

/**
 *  Calculate checksum with MD5 algorithm.
 *  <p>
 *  This function type is implementation of {@link ratpack.func.Function} interface and
 *  takes {@code InputStream} as parameter while returns {@code String} with calculated checksum.
 */
public class MD5Checksummer implements Function<InputStream, String> {
  private static final int BUFFER_SIZE = 8192;

  /**
   * Algorythm implementation.
   *
   * @param is {@code InputStream} of file to calculate checksum
   * @return checksum string
   */
  @Override
  public String apply(InputStream is) throws Exception {
    byte[] buffer = new byte[BUFFER_SIZE];
    MessageDigest md = MessageDigest.getInstance("MD5");
    int read = is.read(buffer, 0, BUFFER_SIZE);
    while (read != -1) {
      md.update(buffer, 0, read);
      read = is.read(buffer, 0, BUFFER_SIZE);
    }

    byte[] digest = md.digest();
    StringBuilder sb = new StringBuilder();
    for (int i=0; i < digest.length; i++) {
      sb.append(Integer.toString((digest[i] & 0xFF) + 0x100, 16).substring(1));
    }
    return sb.toString();
  }
}
