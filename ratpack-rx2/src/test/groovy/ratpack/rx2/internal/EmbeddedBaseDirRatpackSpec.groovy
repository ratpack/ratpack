package ratpack.rx2.internal

import ratpack.test.embed.EphemeralBaseDir
import spock.lang.AutoCleanup

abstract class EmbeddedBaseDirRatpackSpec extends EmbeddedRatpackSpec {

  @AutoCleanup
  @Delegate
  EphemeralBaseDir baseDir

  def setup() {
    baseDir = createBaseDirBuilder()
  }

  protected EphemeralBaseDir createBaseDirBuilder() {
    dir(temporaryFolder.newFolder("app"))
  }

  void useJarBaseDir() {
    baseDir = jar(new File(temporaryFolder.newFolder("jar"), "the.jar"))
  }

}
