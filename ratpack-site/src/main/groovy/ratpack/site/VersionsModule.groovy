package ratpack.site

import com.google.inject.AbstractModule
import com.google.inject.Provides
import ratpack.util.RatpackVersion

@SuppressWarnings(["GrMethodMayBeStatic", "GroovyUnusedDeclaration"])
class VersionsModule extends AbstractModule {

  private final ClassLoader classLoader

  VersionsModule(ClassLoader classLoader) {
    this.classLoader = classLoader
  }

  @Override
  protected void configure() {}

  @Provides
  RatpackVersions provideRatpackVersions() {
    def current = classLoader.getResourceAsStream("currentVersion.txt").text
    def snapshot = RatpackVersion.version
    new RatpackVersions(current, snapshot)
  }
}
