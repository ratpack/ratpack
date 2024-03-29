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

package ratpack.exec.util.internal

import com.google.common.collect.ImmutableMap
import io.netty.buffer.ByteBufAllocator
import ratpack.test.internal.BaseRatpackSpec
import ratpack.test.internal.spock.TempDir
import ratpack.test.internal.spock.TemporaryFolder

import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class IoUtilsSpec extends BaseRatpackSpec {
  private final ByteBufAllocator allocator = ByteBufAllocator.DEFAULT

  @TempDir
  TemporaryFolder temporaryFolder

  def "doesn't require write-access to directory containing a zipped resource"() {
    setup:
    def filename = "testfile.txt"
    def content = "This is my content"
    def folder = temporaryFolder.newFolder()
    def zipFile = new File(folder, "test.zip")
    zipFile.withOutputStream { os ->
      new ZipOutputStream(os).with { zs ->
        zs.putNextEntry(new ZipEntry(filename))
        zs.write(content.bytes)
        zs.closeEntry()
        zs.finish()
      }
    }
    folder.writable = false

    when:
    def uri = new URI("jar:${zipFile.toURI()}")
    def fs = FileSystems.newFileSystem(uri, ImmutableMap.of())
    def path = fs.getPath("/${filename}")
    def byteBuf = IoUtils.read(allocator, path)

    then:
    noExceptionThrown()
    byteBuf.toString(StandardCharsets.UTF_8) == content

    cleanup:
    fs?.close()
    byteBuf?.release()
    folder.writable = true
  }

}
