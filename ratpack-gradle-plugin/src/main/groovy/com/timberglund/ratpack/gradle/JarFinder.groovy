package com.timberglund.ratpack.gradle

class JarFinder {

  static File find(String className, Collection<File> classpath) {
    findJarFile(maybeLoadClass(className, toClassLoader(classpath)))
  }

  private static File findJarFile(Class<?> targetClass) {
    if (targetClass) {
      String absolutePath = targetClass.getResource('/' + targetClass.getName().replace(".", "/") + ".class").path
      String jarPath = absolutePath.substring("file:".length(), absolutePath.lastIndexOf("!"))
      new File(jarPath);
    } else {
      null
    }
  }

  private static ClassLoader toClassLoader(Collection<File> classpath) {
    List<URL> urls = new ArrayList<URL>(classpath.size())
    for (File file in classpath) {
      try {
        urls.add(file.toURI().toURL())
      } catch (MalformedURLException ignore) {}
    }

    new URLClassLoader(urls as URL[])
  }

  private static Class<?> maybeLoadClass(String className, ClassLoader classLoader) {
    if (classLoader) {
      try {
        return classLoader.loadClass(className)
      } catch (ClassNotFoundException ignore) {}
    }

    null
  }

}
