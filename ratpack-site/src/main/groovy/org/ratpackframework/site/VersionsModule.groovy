package org.ratpackframework.site

import com.google.inject.AbstractModule
import com.google.inject.Provides

@SuppressWarnings(["GrMethodMayBeStatic", "GroovyUnusedDeclaration"])
class VersionsModule extends AbstractModule {

  private final ClassLoader classLoader
  private final String resourceName

  VersionsModule(ClassLoader classLoader, String resourceName) {
    this.classLoader = classLoader
    this.resourceName = resourceName
  }

  @Override
  protected void configure() {}

  @Provides
  RatpackVersions provideRatpackVersions() {
    def resourceStream = classLoader.getResourceAsStream(resourceName)
    resourceStream.withStream { InputStream inputStream ->
      def properties = new Properties()
      properties.load(inputStream)
      new RatpackVersions(properties)
    }
  }
}
