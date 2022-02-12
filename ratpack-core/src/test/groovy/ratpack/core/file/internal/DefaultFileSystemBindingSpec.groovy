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

package ratpack.core.file.internal

import ratpack.config.FileSystemBinding
import ratpack.test.internal.BaseRatpackSpec
import ratpack.test.internal.spock.TempDir
import ratpack.test.internal.spock.TemporaryFolder

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream

class DefaultFileSystemBindingSpec extends BaseRatpackSpec {

  static Stream<String> dupeDecode(String payload) {
    String decoded = null
    try {
      decoded = URLDecoder.decode(payload)
    } catch(IllegalArgumentException ignored) {
      // noop
    }
    return Stream.of(payload, decoded).filter { it != null }.distinct()
  }

  static Collection<String> payloadFor(String resource, String fileName) {
    return DefaultFileSystemBindingSpec
      .getResource(resource).text
      .replaceAll("\\{FILE}", fileName)
      .split("\n")
      .toList()
      .stream()
      .flatMap { dupeDecode(it) }
      .distinct()
      .collect()
  }

  static Collection<String> getPayloads(String fileName) {
    return payloadFor("/ratpack/core/server/deep_traversal.txt", fileName) +
      payloadFor("/ratpack/core/server/traversals-8-deep-exotic-encoding.txt", fileName)
  }


  @TempDir
  TemporaryFolder temporaryFolder
  File newRoot

  FileSystemBinding binding

  def setup() {
    newRoot = temporaryFolder.newFile("new-root")
    binding = FileSystemBinding.of(newRoot.toPath())
  }

  def "absolute paths are resolved relative"() {
    File expected = new File(newRoot, "foo")
    expect:
    binding.file("/foo") == expected.toPath()
  }

  def "files not in binding root returns null"() {
    expect:
    binding.file("../../../../etc/passwd") == null
  }

  def "something hidden can't be found"() {
    temporaryFolder.newFile("secret-file.txt")
    expect:
    binding.file("../secret-file.txt") == null
  }

  def "brute something hidden can't be found: #name"() {
    temporaryFolder.newFile("secret-file.txt") << "Text"
    // Sanity pre-test verification
    assert Files.exists(newRoot.toPath().resolve("../secret-file.txt").normalize())
    expect:
    Path thePath = binding.file(traversal)
    if (thePath == null) {
      // The path may be resolvable.
      return
    }
    // the file should never exist
    !Files.exists(thePath)
    where:
    traversal << getPayloads("secret-file.txt")
    name = traversal
  }

  def "non-absolute binding throws exception"() {
    when:
    FileSystemBinding.of(Paths.get("somewhere"))

    then:
    thrown(IllegalArgumentException)
  }
}
