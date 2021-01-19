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

package ratpack.gradle.internal;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class IoUtil {

  private IoUtil() {
  }

  public static String getText(File file) {
    try {
      return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static void setText(File file, String text) {
    try {
      Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static String getText(URL resource) {
    try (Reader reader = new BufferedReader(new InputStreamReader(resource.openConnection().getInputStream()))) {
      StringBuilder string = new StringBuilder();
      char[] buffer = new char[8192];
      int numRead;
      while ((numRead = reader.read(buffer)) != -1) {
        string.append(buffer, 0, numRead);
      }
      return string.toString();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
