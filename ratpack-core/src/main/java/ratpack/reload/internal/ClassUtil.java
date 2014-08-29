/*
 * Copyright 2013 the original author or authors.
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

package ratpack.reload.internal;

import java.io.File;
import java.net.URL;
import java.security.CodeSource;

public class ClassUtil {

  public static File getClassFile(Object object) {
    return getClassFile(object.getClass());
  }

  public static File getClassFile(Class<?> clazz) {
    CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();
    if (codeSource != null) {
      URL location = codeSource.getLocation();
      if (location != null && location.getProtocol().equals("file")) {
        File codeSourceFile = new File(location.getFile());
        File classFile = new File(codeSourceFile, clazz.getName().replace('.', File.separatorChar).concat(".class"));
        if (classFile.exists()) {
          return classFile;
        }
      }
    }

    return null;
  }
}
