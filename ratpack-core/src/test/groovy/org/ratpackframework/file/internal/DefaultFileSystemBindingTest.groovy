package org.ratpackframework.file.internal

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.ratpackframework.file.FileSystemBinding
import spock.lang.Specification

class DefaultFileSystemBindingTest extends Specification {

  @Rule TemporaryFolder temporaryFolder

  FileSystemBinding binding

  def setup() {
    binding = new DefaultFileSystemBinding(temporaryFolder.root)
  }

  def "absolute paths are resolved relative"() {
    expect:
    binding.file("/foo") == temporaryFolder.newFile("foo")
  }
}
