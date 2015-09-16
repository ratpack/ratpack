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

package ratpack.gradle

class JarFinder {

  static File find(String className, Collection<File> classpath) {
    findJarFile(maybeLoadClass(className, toClassLoader(classpath)))
  }

  private static File findJarFile(Class<?> targetClass) {
    if (targetClass) {
      String absolutePath = targetClass.getResource('/' + targetClass.getName().replace(".", "/") + ".class").path
      String jarPath = absolutePath.substring("file:".length(), absolutePath.lastIndexOf("!"))
      new File(jarPath)
    } else {
      null
    }
  }

  private static ClassLoader toClassLoader(Collection<File> classpath) {
    List<URL> urls = new ArrayList<URL>(classpath.size())
    for (File file in classpath) {
      try {
        urls.add(file.toURI().toURL())
      } catch (MalformedURLException ignore) {
      }
    }

    new URLClassLoader(urls as URL[])
  }

  private static Class<?> maybeLoadClass(String className, ClassLoader classLoader) {
    if (classLoader) {
      try {
        return classLoader.loadClass(className)
      } catch (ClassNotFoundException ignore) {
      }
    }

    null
  }

}
