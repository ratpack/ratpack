/*
 * Copyright 2021 the original author or authors.
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

package ratpack.gradle.functional

import com.google.common.io.MoreFiles
import com.google.common.io.RecursiveDeleteOption
import groovy.transform.CompileStatic
import org.codehaus.groovy.runtime.ResourceGroovyMethods

import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING

/**
 * A specialized file that makes creating directory structures more convenient.
 * Provides chained methods for creating children and automatically creates
 * parent directories when using the common #setText and #leftShift operations.
 */
@CompileStatic
class TestFile extends File {

    static TestFile createTempDir(String prefix, String suffix) {
        new TestFile(File.createTempDir(prefix, suffix).canonicalPath)
    }

    TestFile(File file) {
        this(file.absolutePath)
    }

    TestFile(String path) {
        super(path)
    }

    TestFile(File parent, String path) {
        super(parent, path)
    }

    @Override
    TestFile getAbsoluteFile() {
        return new TestFile(super.getAbsoluteFile())
    }

    @Override
    TestFile getCanonicalFile() throws IOException {
        return new TestFile(super.getCanonicalFile())
    }

    TestFile leftShift(Object content) {
        getParentFile().mkdirs()
        ResourceGroovyMethods.leftShift(this, content)
        this
    }

    TestFile prepend(String content) {
        text = content + text
        this
    }

    String getText() {
        assertIsFile()
        ResourceGroovyMethods.getText(this)
    }

    TestFile setText(String content) {
        getParentFile().mkdirs()
        ResourceGroovyMethods.setText(this, content)
        this
    }

    TestFile assertIsFile() {
        assert isFile() : String.format("%s is not a file", this)
        this
    }

    TestFile dir(String path) {
        new TestFile(this, path)
    }

    TestFile createDir() {
        mkdirs()
        this
    }

    TestFile createDir(String path) {
        dir(path).createDir()
    }

    TestFile file(String path) {
        new TestFile(this, path)
    }

    TestFile createFile() {
        createNewFile()
        this
    }

    TestFile createFile(String path) {
        file(path).createFile()
    }

    @Override
    TestFile getParentFile() {
        def parent = super.getParentFile()
        parent ? new TestFile(parent.getPath()) : null
    }

    boolean deleteDir() {
        try {
            // delete but do not follow links
            MoreFiles.deleteRecursively(toPath(), RecursiveDeleteOption.ALLOW_INSECURE)
            return true
        }
        catch (IOException ignored) {
            return false
        }
    }

    @Override
    boolean createNewFile() throws IOException {
        getParentFile().mkdirs()
        super.createNewFile()
    }

    void copyTo(File target) {
        if (directory) {
            def targetDir = target.toPath()
            def sourceDir = this.toPath()
            Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
                @Override
                FileVisitResult visitFile(Path sourceFile, BasicFileAttributes attributes) throws IOException {
                    def targetFile = targetDir.resolve(sourceDir.relativize(sourceFile))
                    Files.copy(sourceFile, targetFile, COPY_ATTRIBUTES, REPLACE_EXISTING)
                    FileVisitResult.CONTINUE
                }

                @Override
                FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) throws IOException {
                    def newDir = targetDir.resolve(sourceDir.relativize(dir))
                    Files.createDirectories(newDir)
                    FileVisitResult.CONTINUE
                }
            })
        } else {
            Files.copy(this.toPath(), target.toPath(), COPY_ATTRIBUTES, REPLACE_EXISTING)
        }
    }
}
