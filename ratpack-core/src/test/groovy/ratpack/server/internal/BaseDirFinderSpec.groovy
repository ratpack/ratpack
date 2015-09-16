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

package ratpack.server.internal

import ratpack.test.embed.EphemeralBaseDir
import spock.lang.AutoCleanup
import spock.lang.Specification

class BaseDirFinderSpec extends Specification {

  @AutoCleanup
  EphemeralBaseDir b1 = EphemeralBaseDir.tmpDir()

  def classLoader = new GroovyClassLoader()

  def "returns empty when not found"() {
    expect:
    !BaseDirFinder.find(classLoader, "foo").isPresent()
  }

  def "returns when found in classloader dir"() {
    when:
    b1.path("foo") << "bar"
    classLoader.addURL(b1.root.toUri().toURL())
    def r = BaseDirFinder.find(classLoader, "foo").get()

    then:
    r.baseDir == b1.root
    r.resource.text == "bar"
  }

  def "returns when found in classloader jar"() {
    when:
    def f = File.createTempFile("ratpack", "test")
    f.delete()
    f.deleteOnExit()
    def dir = EphemeralBaseDir.jar(f)
    dir.path("foo") << "bar"
    dir.root.getFileSystem().close()
    classLoader.addURL(f.toURI().toURL())

    def r = BaseDirFinder.find(classLoader, "foo").get()

    then:
    r.baseDir == dir.root
    r.resource.text == "bar"
  }

}
