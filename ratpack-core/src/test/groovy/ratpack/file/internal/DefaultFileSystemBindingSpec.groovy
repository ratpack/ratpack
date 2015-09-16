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

package ratpack.file.internal

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ratpack.file.FileSystemBinding
import spock.lang.Specification

import java.nio.file.Paths

class DefaultFileSystemBindingSpec extends Specification {

  @Rule
  TemporaryFolder temporaryFolder

  FileSystemBinding binding

  def setup() {
    binding = FileSystemBinding.of(temporaryFolder.root.toPath())
  }

  def "absolute paths are resolved relative"() {
    expect:
    binding.file("/foo") == temporaryFolder.newFile("foo").toPath()
  }

  def "files not in binding root returns null"() {
    expect:
    binding.file("../../../etc/passwd") == null
  }

  def "non-absolute binding throws exception"() {
    when:
    FileSystemBinding.of(Paths.get("somewhere"))

    then:
    thrown(IllegalArgumentException)
  }
}
